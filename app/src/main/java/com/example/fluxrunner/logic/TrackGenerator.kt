package com.example.fluxrunner.logic

import com.example.fluxrunner.model.Coin
import com.example.fluxrunner.model.Element
import com.example.fluxrunner.model.Obstacle
import com.example.fluxrunner.model.ObstacleType
import com.example.fluxrunner.model.PowerUp
import com.example.fluxrunner.model.PowerUpType
import com.example.fluxrunner.model.Vector3D
import kotlin.random.Random

class TrackGenerator {
    // Pools
    val obstaclePool = Array(40) { Obstacle() }
    val coinPool = Array(120) { Coin() }
    val powerUpPool = Array(15) { PowerUp() }

    private var random = Random(System.currentTimeMillis())

    // Tracks procedural generation state
    var maxGeneratedZ: Float = 0f
    private val renderDistance = 90f // how far ahead we generate

    // Unique ID generation counters to prevent collision bypass glitches
    private var nextObstacleId = 1
    private var nextCoinId = 1
    private var nextPowerUpId = 1

    // Risk path tracking
    var isRiskRouteActive = false
    private var riskRouteEndRefZ = 0f
    var portalZSpawned = 0f // tracks last portal spawn to prevent rapid portal stacking

    fun reset(seed: Int? = null) {
        random = seed?.let { Random(it) } ?: Random(System.currentTimeMillis())
        maxGeneratedZ = 0f
        isRiskRouteActive = false
        riskRouteEndRefZ = 0f
        portalZSpawned = 0f
        nextObstacleId = 1
        nextCoinId = 1
        nextPowerUpId = 1
        
        for (obs in obstaclePool) {
            obs.active = false
        }
        for (c in coinPool) {
            c.active = false
        }
        for (p in powerUpPool) {
            p.active = false
        }
    }

    fun update(playerZ: Float) {
        // 1. Recycle obstacles/coins/powerups that are far behind the player (e.g., 15 units behind)
        recycleObjectsBehindPlayer(playerZ)

        // 2. Turn off risk route if we passed its end
        if (isRiskRouteActive && playerZ > riskRouteEndRefZ) {
            isRiskRouteActive = false
        }

        // 3. Spawning: Check if we need to generate more track sections
        if (maxGeneratedZ < playerZ + renderDistance) {
            generateNextTrackSection(playerZ)
        }
    }

    private fun recycleObjectsBehindPlayer(playerZ: Float) {
        val recycleLimitZ = playerZ - 15f
        for (obs in obstaclePool) {
            if (obs.active && obs.position.z < recycleLimitZ) {
                obs.active = false
            }
        }
        for (c in coinPool) {
            if (c.active && c.position.z < recycleLimitZ) {
                c.active = false
            }
        }
        for (p in powerUpPool) {
            if (p.active && p.position.z < recycleLimitZ) {
                p.active = false
            }
        }
    }

    private fun generateNextTrackSection(playerZ: Float) {
        val sectionLength = random.nextInt(25, 45).toFloat()
        val startZ = maxGeneratedZ
        val endZ = startZ + sectionLength

        // Calculate current difficulty factor based on player Z (maxes out at 600m)
        val diffFactor = (playerZ / 600f).coerceIn(0f, 1f)

        // 12% chance of Risk Portal if none spawned recently (only if diffFactor > 0.1)
        if (startZ - portalZSpawned > 150f && random.nextFloat() < 0.12f) {
            spawnRiskPortalSection(startZ, sectionLength)
        } else if (startZ > 500f && random.nextFloat() < 0.15f) {
            spawnMazeSection(startZ)
            maxGeneratedZ = startZ + 45f
            return
        } else {
            // Normal section: choose obstacle complexity based on risk route and difficulty
            val spawnRate = when {
                isRiskRouteActive -> 0.65f
                playerZ < 80f -> 0.18f
                else -> 0.32f + diffFactor * 0.28f
            }
            val count = (sectionLength / 10f).toInt()
            
            val safeStartZ = 60f
            var currentZ = maxOf(startZ + 10f, safeStartZ)
            for (i in 0 until count) {
                if (currentZ > endZ - 5f) break
                
                if (random.nextFloat() < spawnRate) {
                    // Decide whether to spawn a powerup or a standard obstacle
                    val spawnPowerUpChance = 0.05f + (1f - diffFactor) * 0.04f
                    if (random.nextFloat() < spawnPowerUpChance) {
                        spawnPowerUp(currentZ)
                    } else {
                        spawnRandomObstacle(currentZ, diffFactor)
                    }
                } else {
                    if (random.nextFloat() < 0.35f) {
                        spawnCoinPattern(currentZ)
                    }
                }
                // Spacing decreases as difficulty increases
                val minSpacing = 7.5f - diffFactor * 2.0f
                currentZ += random.nextFloat() * 6f + minSpacing
            }
        }

        maxGeneratedZ = endZ
    }

    private fun spawnPowerUp(z: Float) {
        val p = findFreePowerUp() ?: return
        p.active = true
        p.id = nextPowerUpId++
        p.type = PowerUpType.values()[random.nextInt(PowerUpType.values().size)]
        p.lane = random.nextInt(-1, 2)
        p.position.set(p.lane * 3.0f, 0.6f, z)
        p.rotationAngle = random.nextFloat() * 360f
    }

    private fun spawnRandomObstacle(z: Float, diffFactor: Float) {
        val allowedTypes = mutableListOf<ObstacleType>()
        allowedTypes.add(ObstacleType.ELEMENT_GATE)
        allowedTypes.add(ObstacleType.BOOST_PAD)

        if (z > 150f) {
            allowedTypes.add(ObstacleType.ROTATING_GATE)
            allowedTypes.add(ObstacleType.DYNAMIC_GATE)
            allowedTypes.add(ObstacleType.RIFT_ORB)
            allowedTypes.add(ObstacleType.MULTI_TUNNEL)
        }

        if (z > 320f) {
            allowedTypes.add(ObstacleType.CANNON)
            allowedTypes.add(ObstacleType.FLOOR_HAZARD)
        }

        if (z > 620f) {
            allowedTypes.add(ObstacleType.HOLE_WALL)
        }

        val type = allowedTypes[random.nextInt(allowedTypes.size)]

        val obs = findFreeObstacle() ?: return
        obs.active = true
        obs.id = nextObstacleId++
        obs.type = type
        obs.position.set(0f, 0.5f, z)
        obs.requiredElement = Element.values()[random.nextInt(4)]
        obs.width = 2.2f
        obs.height = 3.5f
        obs.depth = 0.5f
        obs.rotationAngle = 0f
        obs.rotationSpeed = 0f
        obs.cycleTimer = 0f
        obs.isGolden = false

        when (type) {
            ObstacleType.ELEMENT_GATE -> {
                obs.lane = random.nextInt(-1, 2)
                obs.position.x = obs.lane * 3.0f
                obs.isGolden = false
            }
            ObstacleType.ROTATING_GATE -> {
                obs.lane = random.nextInt(-1, 2)
                obs.position.x = obs.lane * 3.0f
                val baseRotSpeed = 30f + diffFactor * 45f
                obs.rotationSpeed = baseRotSpeed * (if (random.nextBoolean()) 1f else -1f)
                obs.isGolden = false
            }
            ObstacleType.DYNAMIC_GATE -> {
                obs.lane = random.nextInt(-1, 2)
                obs.position.x = obs.lane * 3.0f
                obs.isGolden = false
                obs.cycleTimer = random.nextFloat()
            }
            ObstacleType.RIFT_ORB -> {
                obs.lane = 0
                obs.position.x = (random.nextFloat() - 0.5f) * 6f
                val baseSlideSpeed = 2.8f + diffFactor * 3.5f
                obs.slideSpeed = if (isRiskRouteActive) baseSlideSpeed + 1.5f else baseSlideSpeed
                obs.slideDir = if (random.nextBoolean()) 1f else -1f
                obs.slideRangeX = 3.0f
                obs.width = 1.15f
                obs.height = 1.15f
                obs.depth = 1.15f
            }
            ObstacleType.MULTI_TUNNEL -> {
                obs.lane = 0
                obs.position.x = 0f
                obs.width = 9.0f
                obs.height = 3.0f
            }
            ObstacleType.BOOST_PAD -> {
                obs.lane = random.nextInt(-1, 2)
                obs.position.x = obs.lane * 3.0f
                obs.width = 1.8f
                obs.height = 0.1f
            }
            ObstacleType.CANNON -> {
                obs.lane = random.nextInt(-1, 2)
                obs.position.set(obs.lane * 3.0f, 0.5f, z)
                obs.width = 1.0f
                obs.height = 2.0f
                obs.depth = 1.0f
            }
            ObstacleType.FLOOR_HAZARD -> {
                obs.lane = random.nextInt(-1, 2)
                obs.position.set(0f, 0.1f, z)
                obs.width = 9.0f
                obs.height = 0.2f
                obs.depth = 3.0f
            }
            ObstacleType.HOLE_WALL -> {
                obs.lane = random.nextInt(-1, 2)
                obs.position.set(0f, 0.5f, z)
                obs.width = 9.0f
                obs.height = 3.5f
                obs.depth = 0.5f
            }
        }

        // TWIST: At higher difficulty, sometimes spawn a "Double Gate Combo"
        if (diffFactor > 0.35f && random.nextFloat() < 0.28f) {
            val secondZ = z + 12f
            val secondObs = findFreeObstacle() ?: return
            secondObs.active = true
            secondObs.id = nextObstacleId++
            secondObs.type = if (random.nextBoolean()) ObstacleType.ELEMENT_GATE else ObstacleType.DYNAMIC_GATE
            secondObs.lane = (obs.lane + (if (random.nextBoolean()) 1 else -1)).coerceIn(-1, 1)
            secondObs.position.set(secondObs.lane * 3.0f, 0.5f, secondZ)
            secondObs.requiredElement = Element.values()[random.nextInt(4)]
            secondObs.width = 2.2f
            secondObs.height = 3.5f
            secondObs.depth = 0.5f
            secondObs.isGolden = false
        }
    }

    private fun spawnCoinPattern(z: Float) {
        val patternType = random.nextInt(3)
        when (patternType) {
            0 -> {
                val lane = random.nextInt(-1, 2)
                val count = random.nextInt(2, 4)
                for (i in 0 until count) {
                    val c = findFreeCoin() ?: break
                    c.active = true
                    c.id = nextCoinId++
                    c.lane = lane
                    c.position.set(lane * 3.0f, 0.5f, z + i * 4.0f)
                    c.rotationAngle = random.nextFloat() * 360f
                }
            }
            1 -> {
                val startLane = if (random.nextBoolean()) -1 else 1
                val dir = -startLane
                for (i in 0..1) {
                    val lane = startLane + i * dir
                    val c = findFreeCoin() ?: break
                    c.active = true
                    c.id = nextCoinId++
                    c.lane = lane
                    c.position.set(lane * 3.0f, 0.5f, z + i * 4.0f)
                    c.rotationAngle = random.nextFloat() * 360f
                }
            }
            2 -> {
                val count = 3
                for (i in 0 until count) {
                    val lane = when (i) {
                        0 -> -1
                        1 -> 0
                        else -> 1
                    }
                    val c = findFreeCoin() ?: break
                    c.active = true
                    c.id = nextCoinId++
                    c.lane = lane
                    c.position.set(lane * 3.0f, 0.5f, z + i * 4.0f)
                    c.rotationAngle = random.nextFloat() * 360f
                }
            }
        }
    }

    private fun spawnMazeSection(z: Float) {
        val lanes = listOf(-1, 0, 1).shuffled()
        for (i in 0..2) {
            val obs = findFreeObstacle() ?: break
            obs.active = true
            obs.id = nextObstacleId++
            obs.type = ObstacleType.HOLE_WALL
            obs.lane = lanes[i]
            obs.position.set(0f, 0.5f, z + i * 14f)
            obs.width = 9.0f
            obs.height = 3.5f
            obs.depth = 0.5f
        }
    }

    private fun spawnRiskPortalSection(z: Float, length: Float) {
        portalZSpawned = z
        val portalLane = if (random.nextBoolean()) -1 else 1
        
        val obs = findFreeObstacle() ?: return
        obs.active = true
        obs.id = nextObstacleId++
        obs.type = ObstacleType.ELEMENT_GATE
        obs.lane = portalLane
        obs.position.set(portalLane * 3.0f, 0.5f, z + length / 2f)
        obs.requiredElement = Element.values()[random.nextInt(4)]
        obs.width = 2.2f
        obs.height = 3.5f
        obs.depth = 0.5f
        obs.isGolden = false
        obs.rotationSpeed = -999f 
        
        for (i in 0..4) {
            val c = findFreeCoin() ?: break
            c.active = true
            c.id = nextCoinId++
            c.lane = portalLane
            c.position.set(portalLane * 3.0f, 0.5f, z + i * 3.0f)
        }
    }

    fun triggerRiskRoute(length: Float) {
        isRiskRouteActive = true
        riskRouteEndRefZ = maxGeneratedZ + length
    }

    private fun findFreeObstacle(): Obstacle? {
        for (obs in obstaclePool) {
            if (!obs.active) return obs
        }
        return null
    }

    private fun findFreeCoin(): Coin? {
        for (c in coinPool) {
            if (!c.active) return c
        }
        return null
    }

    private fun findFreePowerUp(): PowerUp? {
        for (p in powerUpPool) {
            if (!p.active) return p
        }
        return null
    }
}
