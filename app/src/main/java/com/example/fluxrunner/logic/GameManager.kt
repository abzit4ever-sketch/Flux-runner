package com.example.fluxrunner.logic

import android.graphics.Color
import com.example.fluxrunner.engine.ParticleSystem
import com.example.fluxrunner.media.AudioManager
import com.example.fluxrunner.model.Element
import com.example.fluxrunner.model.MissionType
import com.example.fluxrunner.model.Obstacle
import com.example.fluxrunner.model.ObstacleType
import com.example.fluxrunner.model.PowerUp
import com.example.fluxrunner.model.PowerUpType
import com.example.fluxrunner.model.Skin
import com.example.fluxrunner.model.Vector3D
import com.example.fluxrunner.model.getTrackY
import kotlin.math.abs
import kotlin.random.Random

enum class GameState {
    MENU,
    PLAYING,
    PAUSED,
    GAME_OVER
}

class GameManager(
    val player: PlayerController,
    val trackGenerator: TrackGenerator,
    val audioManager: AudioManager,
    val saveManager: SaveManager,
    val missionManager: MissionManager,
    val particleSystem: ParticleSystem
) {
    var gameState: GameState = GameState.MENU
        private set

    // Game stats
    var score: Int = 0
    var coinsCollected: Int = 0
    var shardsCollected: Int = 0
    var combo: Int = 0
    var maxCombo: Int = 0
    var distanceTraveled: Float = 0f
    var runTimer: Float = 0f
        private set

    // Speed increase tracker
    private var speedTimer: Float = 0f
    private var baseRunSpeed: Float = 9.0f
    private var boostTimer: Float = 0f
    private var fluxSurgeCountdown: Float = 0f
    private var nextMilestoneReward = 100f
    var isFluxSurgeActive = false
        private set
    var fluxSurgeTimer = 0f
        private set

    // Active powerups timers
    var shieldTimer = 0f
        private set
    var magnetTimer = 0f
        private set
    var invisibilityTimer = 0f
        private set

    val isShieldActive: Boolean get() = shieldTimer > 0f
    val isMagnetActive: Boolean get() = magnetTimer > 0f
    val isInvisibilityActive: Boolean get() = invisibilityTimer > 0f

    // Retired boost mode state kept internal for renderer compatibility.
    var isOverchargeActive = false
        private set
    var overchargeTimer = 0f
    private val overchargeDuration = 10f
    var hyperfluxCharge = 0f
        private set
    var skillCallout = ""
        private set
    private var skillCalloutTimer = 0f
    var orbEvolutionLevel = 1
        private set
    var isLuckyRun = false
        private set
    var dailyChallengeText = ""
        private set
    var dailyChallengeProgress = 0f
        private set
    var isDailyChallengeFailed = false
        private set
    var isDailyChallengeCompleted = false
        private set

    private val random = Random(System.currentTimeMillis())
    private val synergyPairs = mapOf(
        Element.FIRE to Element.ELECTRIC,
        Element.ICE to Element.TOXIC
    )
    private var lastSuccessfulGateElement: Element? = null
    private var dailyChallengeTarget = 500f
    private var dailyAllowedElements = setOf(Element.FIRE, Element.ICE)

    // Callbacks for UI updates
    var onGameStateChanged: ((GameState) -> Unit)? = null
    var onGameOver: ((score: Int, distance: Float, coins: Int) -> Unit)? = null

    // Track which obstacles the player has already passed/checked
    private val passedObstacleIds = mutableSetOf<Int>()

    fun init() {
        missionManager.initMissions()
        // Load active skin
        val activeSkinId = saveManager.getActiveSkin()
        player.activeSkin = Skin.getById(activeSkinId)
    }

    fun startRun(seed: Int? = null) {
        score = 0
        coinsCollected = 0
        shardsCollected = 0
        combo = 0
        maxCombo = 0
        distanceTraveled = 0f
        runTimer = 0f
        speedTimer = 0f
        baseRunSpeed = 9.0f
        nextMilestoneReward = 100f
        boostTimer = 0f
        fluxSurgeCountdown = random.nextInt(30, 46).toFloat()
        isFluxSurgeActive = false
        fluxSurgeTimer = 0f
        isOverchargeActive = false
        overchargeTimer = 0f
        shieldTimer = 3.0f
        magnetTimer = 0f
        invisibilityTimer = 0f
        hyperfluxCharge = 0f
        skillCallout = ""
        skillCalloutTimer = 0f
        orbEvolutionLevel = 1
        isLuckyRun = random.nextInt(20) == 0
        lastSuccessfulGateElement = null
        setupDailyChallenge()
        passedObstacleIds.clear()

        player.reset()
        trackGenerator.reset(seed)
        
        gameState = GameState.PLAYING
        onGameStateChanged?.invoke(gameState)
        
        audioManager.startMusic()
        audioManager.setTempoAndIntensity(0)
        if (isLuckyRun) {
            showSkillCallout("LUCKY RUN")
            saveManager.addCoins(25)
            coinsCollected += 25
        }
    }

    fun pauseGame() {
        if (gameState == GameState.PLAYING) {
            gameState = GameState.PAUSED
            onGameStateChanged?.invoke(gameState)
            audioManager.stopMusic()
        }
    }

    fun resumeGame() {
        if (gameState == GameState.PAUSED) {
            gameState = GameState.PLAYING
            onGameStateChanged?.invoke(gameState)
            audioManager.startMusic()
        }
    }

    fun update(deltaTime: Float) {
        if (gameState != GameState.PLAYING) return
        runTimer += deltaTime

        // 1. Update powerup timers
        if (shieldTimer > 0f) {
            shieldTimer -= deltaTime
        }
        if (magnetTimer > 0f) {
            magnetTimer -= deltaTime
        }
        if (invisibilityTimer > 0f) {
            invisibilityTimer -= deltaTime
        }

        // 2. Update systems
        val prevZ = player.worldZ
        player.update(deltaTime)
        distanceTraveled = player.worldZ
        updateDistanceMilestones()
        updateDailyChallengeState()
        
        // Progress distance mission
        missionManager.progressMission(MissionType.DISTANCE, (player.forwardSpeed * deltaTime).toInt())

        trackGenerator.update(player.worldZ)
        particleSystem.update(deltaTime)
        updateTimedCallouts(deltaTime)

        // 3. Spawn element trail particles
        if (isInvisibilityActive) {
            particleSystem.spawnTrailParticle(
                Vector3D(player.worldX, 0.5f, player.worldZ),
                Element.TOXIC // Bloom/Toxic purple-green particles for invisibility
            )
        } else {
            particleSystem.spawnTrailParticle(
                Vector3D(player.worldX, 0.5f, player.worldZ),
                player.activeElement
            )
        }

        // 4. First stretch stays gentle, then speed ramps steadily.
        val rampDistance = maxOf(0f, distanceTraveled - 60f)
        baseRunSpeed = minOf(8.5f + (rampDistance / 120f) * 1.8f, 32.0f)

        updateFluxSurge(deltaTime)
        updateSpeedState(deltaTime)

        // 5. Retired boost timer
        if (isOverchargeActive) {
            overchargeTimer -= deltaTime
            player.targetSpeed = maxOf(player.targetSpeed, 32f)
            if (overchargeTimer <= 0f) {
                isOverchargeActive = false
                player.targetSpeed = minOf(player.targetSpeed, player.maxSpeed)
                audioManager.setTempoAndIntensity(combo)
            }
        }

        // 6. Handle collisions & scoring updates
        checkCollisions(prevZ, distanceTraveled, deltaTime)

        // Calculate score increment = distance difference * multiplier
        val multiplier = getScoreMultiplier()
        val distDiff = distanceTraveled - prevZ
        score += (distDiff * multiplier).toInt()
    }

    private fun getScoreMultiplier(): Float {
        var mult = 1.0f
        if (trackGenerator.isRiskRouteActive) {
            mult *= 2.0f // risk path bonus
        }
        if (isOverchargeActive) {
            mult *= 1.0f
        }
        // Combo multiplier: every combo point adds +5% score multiplier
        if (combo >= 2) {
            mult *= (1.0f + combo * 0.05f)
        }
        return mult
    }

    private fun updateTimedCallouts(deltaTime: Float) {
        if (skillCalloutTimer > 0f) {
            skillCalloutTimer -= deltaTime
            if (skillCalloutTimer <= 0f) {
                skillCallout = ""
            }
        }
    }

    private fun updateFluxSurge(deltaTime: Float) {
        if (isFluxSurgeActive) {
            fluxSurgeTimer -= deltaTime
            if (fluxSurgeTimer <= 0f) {
                isFluxSurgeActive = false
                fluxSurgeCountdown = random.nextInt(30, 46).toFloat()
            }
            return
        }

        fluxSurgeCountdown -= deltaTime
        if (fluxSurgeCountdown <= 0f) {
            isFluxSurgeActive = true
            fluxSurgeTimer = random.nextInt(5, 9).toFloat()
            audioManager.playBossWarning()
        }
    }

    private fun updateSpeedState(deltaTime: Float) {
        if (boostTimer > 0f) {
            boostTimer -= deltaTime
        }

        player.targetSpeed = when {
            boostTimer > 0f -> minOf(baseRunSpeed + 6f, player.maxSpeed)
            isFluxSurgeActive -> minOf(baseRunSpeed + 5f, player.maxSpeed)
            else -> baseRunSpeed
        }
    }

    private fun showSkillCallout(text: String) {
        skillCallout = text
        skillCalloutTimer = 1.1f
    }

    private fun awardSkillMomentum(amount: Float) {
        hyperfluxCharge = 0f
    }

    private fun setupDailyChallenge() {
        val dateStr = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault()).format(java.util.Date())
        val pairIndex = (dateStr.takeLast(2).toIntOrNull() ?: 0) % 4
        dailyAllowedElements = when (pairIndex) {
            0 -> setOf(Element.FIRE, Element.ICE)
            1 -> setOf(Element.FIRE, Element.ELECTRIC)
            2 -> setOf(Element.FIRE, Element.TOXIC)
            else -> setOf(Element.FIRE, Element.TOXIC)
        }
        dailyChallengeTarget = 500f
        dailyChallengeProgress = 0f
        isDailyChallengeCompleted = saveManager.isDailyChallengeCompletedToday(dateStr)
        isDailyChallengeFailed = false
        val names = dailyAllowedElements.joinToString(" + ") { it.displayName.uppercase() }
        dailyChallengeText = "DAILY: ${dailyChallengeTarget.toInt()}m / $names"
    }

    private fun updateDistanceMilestones() {
        if (distanceTraveled < nextMilestoneReward) return

        val rewardCoins = 5 + (nextMilestoneReward / 500f).toInt() * 2
        coinsCollected += rewardCoins
        saveManager.addCoins(rewardCoins)
        showSkillCallout("${nextMilestoneReward.toInt()}M BONUS")
        audioManager.playCombo()
        nextMilestoneReward += 100f
    }

    private fun updateDailyChallengeState() {
        if (isDailyChallengeFailed || isDailyChallengeCompleted) return

        if (player.activeElement !in dailyAllowedElements) {
            isDailyChallengeFailed = true
            return
        }

        dailyChallengeProgress = (distanceTraveled / dailyChallengeTarget).coerceIn(0f, 1f)
        if (distanceTraveled >= dailyChallengeTarget) {
            val dateStr = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault()).format(java.util.Date())
            saveManager.setDailyChallengeCompletedToday(dateStr)
            saveManager.unlockSkin(Skin.CHALLENGER.id)
            saveManager.addCoins(300)
            saveManager.addShards(3)
            coinsCollected += 300
            shardsCollected += 3
            isDailyChallengeCompleted = true
            showSkillCallout("DAILY CLEARED")
        }
    }

    private fun maybeAwardShard(chance: Float, reason: String) {
        val boostedChance = if (isLuckyRun) chance * 3f else chance
        if (random.nextFloat() < boostedChance) {
            shardsCollected++
            saveManager.addShards(1)
            showSkillCallout(reason)
        }
    }

    private fun applyElementSynergy(currentElement: Element) {
        val previous = lastSuccessfulGateElement
        lastSuccessfulGateElement = currentElement
        val expected = synergyPairs[previous] ?: return
        if (expected == currentElement) {
            val label = when (previous) {
                Element.FIRE -> "PLASMA"
                Element.ICE -> "CORROSION"
                else -> "SYNERGY"
            }
            combo += 3
            maxCombo = maxOf(maxCombo, combo)
            score += 120
            awardSkillMomentum(16f)
            updateOrbEvolution()
            showSkillCallout(label)
        }
    }

    private fun updateOrbEvolution() {
        orbEvolutionLevel = when {
            combo >= 40 -> 5
            combo >= 25 -> 4
            combo >= 14 -> 3
            combo >= 6 -> 2
            else -> 1
        }
    }

    private fun multiBoardElement(obstacleId: Int, panelIndex: Int): Element {
        val all = Element.values()
        val offset = obstacleId % all.size
        return all[(offset + panelIndex) % all.size]
    }

    private fun checkCollisions(prevZ: Float, currZ: Float, deltaTime: Float) {
        val px = player.worldX
        val playerRadius = 0.5f

        // Check PowerUp Collisions
        for (p in trackGenerator.powerUpPool) {
            if (!p.active) continue

            p.update(deltaTime)

            // 1. Magnetic pull: draw powerup towards player if magnet is active
            if (isMagnetActive && p.position.z - currZ in -2f..15.0f) {
                p.position.x += (px - p.position.x) * 5.5f * deltaTime
                p.position.y += (0.6f + getTrackY(currZ) - p.position.y) * 5.5f * deltaTime
                p.position.z += (currZ - p.position.z) * 5.5f * deltaTime
            }

            // 2. Collision check
            if (abs(currZ - p.position.z) <= 0.8f) {
                if (abs(px - p.position.x) <= 0.8f) {
                    p.active = false
                    when (p.type) {
                        PowerUpType.SHIELD -> {
                            shieldTimer = 8.0f
                            showSkillCallout("SHIELD ACTIVE")
                        }
                        PowerUpType.MAGNET -> {
                            magnetTimer = 10.0f
                            showSkillCallout("MAGNET ACTIVE")
                        }
                        PowerUpType.INVISIBILITY -> {
                            invisibilityTimer = 8.0f
                            showSkillCallout("PHASE ACTIVE")
                        }
                    }
                    audioManager.playCombo()
                    particleSystem.spawnBurst(p.position, player.activeElement, 12)
                }
            }
        }

        // Check Coin Collisions
        for (c in trackGenerator.coinPool) {
            if (!c.active) continue

            // 1. Spin the coins for visual polish
            c.update(deltaTime)

            // 2. Magnetic pull: draw coin towards player if in range
            val inRange = isMagnetActive && (c.position.z - currZ in -2f..15.0f)
            if (inRange) {
                c.position.x += (px - c.position.x) * 5.5f * deltaTime
                c.position.y += (0.5f + getTrackY(currZ) - c.position.y) * 5.5f * deltaTime
                c.position.z += (currZ - c.position.z) * 5.5f * deltaTime
            }

            // 3. Collision check
            if (abs(currZ - c.position.z) <= 0.8f) {
                if (abs(px - c.position.x) <= 0.8f) {
                    // Collect!
                    c.active = false
                    coinsCollected++
                    saveManager.addCoins(1)
                    missionManager.progressMission(MissionType.COINS, 1)
                    audioManager.playCoin()
                    
                    // Spawn coin collection particles
                    particleSystem.spawnBurst(c.position, player.activeElement, 5)
                }
            }
        }

        // Check Obstacle Collisions
        for (obs in trackGenerator.obstaclePool) {
            if (!obs.active) continue

            // Update obstacles (handles rotating/sliding/dynamic cycling) using the real deltaTime
            val dynamicGateDuration = (3.5f - (distanceTraveled / 600f) * 1.7f).coerceAtLeast(1.8f)
            obs.update(deltaTime, dynamicGateDuration)

            val obstacleZ = obs.position.z
            val obstacleDepth = obs.depth
            
            // Swept Collision Check: Detect if the player crossed the obstacle front face in this frame,
            // or is currently overlapping its volume.
            val wasBefore = prevZ < (obstacleZ - 0.2f)
            val isAfter = currZ >= (obstacleZ - 0.2f)
            val isOverlap = currZ >= (obstacleZ - 0.2f) && currZ <= (obstacleZ + obstacleDepth + 0.5f)

            if (isOverlap || (wasBefore && isAfter)) {
                val obsId = obs.id
                if (passedObstacleIds.contains(obsId)) continue

                // Check collision based on obstacle type
                var isCollision = false
                var isCorrectMatch = false
                var isPortal = false

                when (obs.type) {
                    ObstacleType.ELEMENT_GATE, ObstacleType.ROTATING_GATE, ObstacleType.DYNAMIC_GATE -> {
                        // Check if in correct lane
                        if (abs(px - obs.position.x) < (playerRadius + obs.width * 0.5f)) {
                            isCollision = true
                            if (obs.rotationSpeed == -999f) {
                                isPortal = true
                            } else if (player.activeElement == obs.requiredElement) {
                                isCorrectMatch = true
                            }
                        }
                    }
                    ObstacleType.RIFT_ORB -> {
                        // Avoid the unstable rift orb.
                        if (abs(px - obs.position.x) < (playerRadius + obs.width * 0.55f)) {
                            isCollision = true
                            isCorrectMatch = false
                        }
                    }
                    ObstacleType.MULTI_TUNNEL -> {
                        // Three clean lane panels, one per lane.
                        isCollision = true
                        val sliceWidth = obs.width / 3.0f
                        val leftBorder = -obs.width / 2.0f
                        val relativeX = px - leftBorder
                        val sliceIndex = (relativeX / sliceWidth).toInt().coerceIn(0, 2)
                        val requiredEl = multiBoardElement(obs.id, sliceIndex)
                        
                        if (player.activeElement == requiredEl) {
                            isCorrectMatch = true
                        }
                    }
                    ObstacleType.BOOST_PAD -> {
                        // Ground pad: check if in the same lane
                        if (abs(px - obs.position.x) < (playerRadius + obs.width * 0.5f)) {
                            isCollision = true
                            if (player.activeElement == obs.requiredElement) {
                                isCorrectMatch = true
                            }
                        }
                    }
                    ObstacleType.CANNON -> {
                        // Cannon shoots down target lane obs.lane
                        if (player.lane == obs.lane) {
                            isCollision = true
                            isCorrectMatch = false
                        }
                    }
                    ObstacleType.FLOOR_HAZARD -> {
                        // Player safe only if in lane obs.lane
                        if (player.lane != obs.lane) {
                            isCollision = true
                            isCorrectMatch = false
                        }
                    }
                    ObstacleType.HOLE_WALL -> {
                        // Player safe only if in hole lane obs.lane
                        if (player.lane != obs.lane) {
                            isCollision = true
                            isCorrectMatch = false
                        }
                    }
                }

                if (!isCollision && obs.type == ObstacleType.RIFT_ORB && (wasBefore && isAfter)) {
                    val nearMissGap = abs(px - obs.position.x) - (playerRadius + obs.width * 0.5f)
                    if (nearMissGap in 0f..0.45f) {
                        passedObstacleIds.add(obsId)
                        combo += 5
                        maxCombo = maxOf(maxCombo, combo)
                        score += 75
                        awardSkillMomentum(12f)
                        updateOrbEvolution()
                        showSkillCallout("NEAR MISS")
                        audioManager.playCombo()
                        particleSystem.spawnBurst(Vector3D(px, 0.5f, currZ), player.activeElement, 18)
                    }
                }

                if (isCollision) {
                    if (isPortal) {
                        // Passed through risk portal!
                        passedObstacleIds.add(obsId)
                        trackGenerator.triggerRiskRoute(150f) // 150 meters risk route
                        audioManager.playCombo()
                        particleSystem.spawnBurst(obs.position, player.activeElement, 20)
                    } else if (isCorrectMatch) {
                        // Correct Element match! Pass through
                        passedObstacleIds.add(obsId)
                        combo++
                        maxCombo = maxOf(maxCombo, combo)
                        updateOrbEvolution()
                        val switchAge = player.lastElementSwitchAge
                        val isCloseCall = switchAge <= 0.12f
                        val isPerfectSwitch = switchAge <= 0.30f
                        val gateBonus = when {
                            obs.isGolden -> 500
                            isCloseCall -> 85
                            isPerfectSwitch -> 45
                            else -> 10
                        }
                        score += gateBonus
                        awardSkillMomentum(
                            when {
                            obs.isGolden -> 35f
                            isCloseCall -> 20f
                            isPerfectSwitch -> 14f
                            else -> if (isLuckyRun) 10f else 7f
                            }
                        )
                        missionManager.progressMission(MissionType.COMBO, 1)
                        applyElementSynergy(player.activeElement)
                        
                        if (obs.type == ObstacleType.BOOST_PAD) {
                            // Boost pads work for the current core and expire cleanly.
                            score += 50
                            boostTimer = 2.0f
                            player.forwardSpeed = maxOf(player.forwardSpeed, minOf(baseRunSpeed + 6f, player.maxSpeed))
                            audioManager.playCombo()
                            particleSystem.spawnBurst(obs.position, player.activeElement, 10)
                        } else {
                            if (obs.isGolden) {
                                val rewardCoins = if (isLuckyRun) 25 else 10
                                coinsCollected += rewardCoins
                                saveManager.addCoins(rewardCoins)
                                maybeAwardShard(0.35f, "FLUX SHARD")
                                showSkillCallout("GOLDEN GATE")
                            } else if (isCloseCall) {
                                showSkillCallout(listOf("CLOSE CALL", "INSANE", "CLUTCH").random(random))
                                combo += 4
                                maxCombo = maxOf(maxCombo, combo)
                                maybeAwardShard(0.04f, "FLUX SHARD")
                            } else if (isPerfectSwitch) {
                                showSkillCallout("PERFECT SWITCH")
                                combo += 2
                                maxCombo = maxOf(maxCombo, combo)
                                maybeAwardShard(0.015f, "FLUX SHARD")
                            }
                            audioManager.playCombo()
                            audioManager.setTempoAndIntensity(combo)
                            val burstCount = if (obs.isGolden || isCloseCall) 28 else 12
                            particleSystem.spawnBurst(obs.position, player.activeElement, burstCount)
                            updateComboEffects()
                        }
                    } else {
                        // Wrong element or hit obstacle!
                        if (obs.type == ObstacleType.BOOST_PAD) {
                            // Boost pads are neutral: any current core can use them.
                            passedObstacleIds.add(obsId)
                            score += 50
                            boostTimer = 2.0f
                            player.forwardSpeed = maxOf(player.forwardSpeed, minOf(baseRunSpeed + 6f, player.maxSpeed))
                            audioManager.playCombo()
                            particleSystem.spawnBurst(obs.position, player.activeElement, 10)
                        } else {
                            if (isOverchargeActive || isInvisibilityActive) {
                                // Temporary invincibility / phase mode: phase through
                                passedObstacleIds.add(obsId)
                                score += 25
                                showSkillCallout("PHASED")
                                audioManager.playCombo()
                                particleSystem.spawnCollisionExplosion(obs.position, Color.WHITE)
                            } else if (isShieldActive) {
                                // Shield absorbs the hit!
                                passedObstacleIds.add(obsId)
                                shieldTimer = 0f // consume shield
                                showSkillCallout("SHIELD BROKEN")
                                audioManager.playCombo()
                                particleSystem.spawnCollisionExplosion(obs.position, Color.parseColor("#35F3FF"))
                            } else {
                                // Player Dies!
                                triggerDeath()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun updateComboEffects() {
        if (combo >= 50 && skillCallout != "MAX COMBO") showSkillCallout("MAX COMBO")
    }

    private fun triggerDeath() {
        gameState = GameState.GAME_OVER
        onGameStateChanged?.invoke(gameState)
        
        // Halt player speed immediately to stop road scrolling
        player.forwardSpeed = 0f
        player.targetSpeed = 0f
        
        audioManager.stopMusic()
        audioManager.playDeath()

        // Spawn massive explosion
        particleSystem.spawnCollisionExplosion(Vector3D(player.worldX, 0.5f, player.worldZ), player.activeElement.primaryColor)

        // Save high scores & statistics
        saveManager.saveHighScore(score)
        saveManager.saveLongestDistance(distanceTraveled)
        saveManager.saveHighestCombo(maxCombo)

        onGameOver?.invoke(score, distanceTraveled, coinsCollected)
    }

    fun reviveRun() {
        gameState = GameState.PLAYING
        onGameStateChanged?.invoke(gameState)
        
        baseRunSpeed = maxOf(9.0f + (distanceTraveled / 150f) * 1.5f, 9.0f)
        player.forwardSpeed = baseRunSpeed
        player.targetSpeed = baseRunSpeed
        
        // Move player back from collision point to avoid instant repeat death
        player.worldZ = maxOf(0f, player.worldZ - 6f)
        distanceTraveled = player.worldZ
        
        // Temporary shield after revive to prevent repeat hits.
        shieldTimer = 4.0f
        
        audioManager.startMusic()
        audioManager.playCombo()
    }
}
