package com.example.fluxrunner.engine

import android.graphics.*
import com.example.fluxrunner.model.Coin
import com.example.fluxrunner.model.Element
import com.example.fluxrunner.model.Obstacle
import com.example.fluxrunner.model.ObstacleType
import com.example.fluxrunner.model.PowerUp
import com.example.fluxrunner.model.PowerUpType
import com.example.fluxrunner.model.Skin
import com.example.fluxrunner.model.Vector3D
import com.example.fluxrunner.model.getTrackY
import kotlin.math.*
import com.example.fluxrunner.engine.ElementSymbolDrawer

class Renderer3D {
    // Screen dimensions
    private var width: Int = 0
    private var height: Int = 0
    private var centerX: Float = 0f
    private var centerY: Float = 0f

    // Camera parameters
    val cameraPos = Vector3D(0f, 6f, -10f)
    private val cameraTarget = Vector3D(0f, 0f, 5f)
    private var fov: Float = 1000f

    // Camera angles
    private var cosPitch = 1f
    private var sinPitch = 0f
    private var cosYaw = 1f
    private var sinYaw = 0f

    // Rendering Paints
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 4f
        style = Paint.Style.STROKE
    }
    
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 12f
    }
    
    private val objectPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL_AND_STROKE
        strokeWidth = 2f
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    // Temporary storage to prevent GC allocation
    private val p1 = PointF()
    private val p2 = PointF()
    private val p3 = PointF()
    private val p4 = PointF()
    
    private class Star(var x: Float, var y: Float, val speed: Float, val size: Float, val alpha: Float)
    private val stars = ArrayList<Star>()
    private var skyShader: LinearGradient? = null
    private var lastThemeColor = 0

    fun setScreenSize(w: Int, h: Int) {
        width = w
        height = h
        centerX = w / 2f
        centerY = h * 0.65f // Move horizon slightly up
        fov = h * 0.85f     // Focal length
        skyShader = null    // Force sky shader recreation
        
        // Initialize scrolling stars
        val random = java.util.Random()
        stars.clear()
        for (i in 0 until 50) {
            stars.add(Star(
                x = random.nextFloat() * w,
                y = random.nextFloat() * h * 0.65f,
                speed = 12f + random.nextFloat() * 25f,
                size = 1f + random.nextFloat() * 3f,
                alpha = 0.2f + random.nextFloat() * 0.8f
            ))
        }
    }

    fun updateCamera(playerX: Float, playerZ: Float, bossActive: Boolean) {
        val trackY = getTrackY(playerZ)
        // Camera follows player smoothly
        val targetCamX = playerX * 0.6f // slightly lag behind player lane
        val targetCamY = trackY + (if (bossActive) 8f else 6f)
        val targetCamZ = playerZ - 9f

        // Smooth follow: lerp camera position
        cameraPos.x += (targetCamX - cameraPos.x) * 0.15f
        cameraPos.y += (targetCamY - cameraPos.y) * 0.15f
        cameraPos.z += (targetCamZ - cameraPos.z) * 0.2f

        // Look at player position slightly ahead
        cameraTarget.x = playerX
        cameraTarget.y = trackY + 0.5f // look at player height
        cameraTarget.z = playerZ + 4f

        // Calculate pitch/yaw angles
        val dx = cameraTarget.x - cameraPos.x
        val dy = cameraTarget.y - cameraPos.y
        val dz = cameraTarget.z - cameraPos.z

        val horizDist = sqrt(dx * dx + dz * dz)
        val pitch = -atan2(dy, horizDist)
        val yaw = atan2(dx, dz)

        cosPitch = cos(pitch)
        sinPitch = sin(pitch)
        cosYaw = cos(yaw)
        sinYaw = sin(yaw)
    }

    // Projects a 3D coordinate to 2D screen coordinates
    fun project(world: Vector3D, out: PointF): Boolean {
        // 1. Translate relative to camera
        val tx = world.x - cameraPos.x
        val ty = world.y - cameraPos.y
        val tz = world.z - cameraPos.z

        // 2. Rotate Yaw (Y axis)
        val rx = tx * cosYaw - tz * sinYaw
        val rz1 = tx * sinYaw + tz * cosYaw

        // 3. Rotate Pitch (X axis)
        val ry = ty * cosPitch + rz1 * sinPitch
        val rz = -ty * sinPitch + rz1 * cosPitch

        // Clip if behind camera (with small buffer)
        if (rz <= 0.2f) return false

        // 4. Perspective Divide
        val factor = fov / rz
        val px = centerX + rx * factor
        val py = centerY - ry * factor
        if (px.isNaN() || py.isNaN() || px.isInfinite() || py.isInfinite()) return false
        out.x = px.coerceIn(-5000f, 5000f)
        out.y = py.coerceIn(-5000f, 5000f)
        return true
    }

    fun draw(canvas: Canvas, playerX: Float, playerZ: Float, activeElement: Element, activeSkin: Skin,
             obstacles: Array<Obstacle>, coins: Array<Coin>, powerUps: Array<PowerUp>, particles: List<Particle>, themeColor: Int, 
             deltaTime: Float, shieldTimer: Float, magnetTimer: Float, invisibilityTimer: Float,
             overchargeActive: Boolean, orbEvolutionLevel: Int, fluxSurgeActive: Boolean) {
        
        // 1. Draw Background Grid & Nebular Horizon
        drawBackground(canvas, themeColor, deltaTime)

        // 2. Draw Track Grid (Futuristic Highway)
        drawTrack(canvas, playerZ, themeColor)

        // 3. Draw Coins
        for (c in coins) {
            if (c.active) {
                drawCoin(canvas, c)
            }
        }

        // 4. Draw PowerUps
        for (p in powerUps) {
            if (p.active) {
                drawPowerUp(canvas, p)
            }
        }

        // 5. Draw Obstacles
        for (obs in obstacles) {
            if (obs.active) {
                drawObstacle(canvas, obs)
            }
        }

        // 6. Draw Particles
        drawParticles(canvas, particles)

        // 7. Draw Player Orb
        drawPlayer(canvas, playerX, playerZ, activeElement, activeSkin, overchargeActive, orbEvolutionLevel, shieldTimer, magnetTimer, invisibilityTimer)
    }

    private fun drawBackground(canvas: Canvas, themeColor: Int, deltaTime: Float) {
        // Set deep dark background
        canvas.drawColor(Color.parseColor("#090A15"))

        // Set beautiful sky gradient horizon
        if (skyShader == null || lastThemeColor != themeColor) {
            lastThemeColor = themeColor
            val colorTop = Color.parseColor("#030308")
            val colorHorizon = adjustAlpha(themeColor, 0.14f)
            skyShader = LinearGradient(0f, 0f, 0f, centerY, colorTop, colorHorizon, Shader.TileMode.CLAMP)
        }

        objectPaint.reset()
        objectPaint.isAntiAlias = true
        objectPaint.style = Paint.Style.FILL
        objectPaint.shader = skyShader
        canvas.drawRect(0f, 0f, width.toFloat(), centerY, objectPaint)
        objectPaint.shader = null

        // Update & Draw scrolling stars
        for (star in stars) {
            star.y = (star.y + star.speed * deltaTime) % centerY
            objectPaint.color = adjustAlpha(Color.WHITE, star.alpha)
            canvas.drawCircle(star.x, star.y, star.size, objectPaint)
        }
    }

    private fun drawTrack(canvas: Canvas, playerZ: Float, themeColor: Int) {
        val step = 5f
        val renderDistance = 80f
        val startZ = (playerZ / step).toInt() * step - 20f
        val endZ = playerZ + renderDistance

        var z = startZ
        while (z < endZ) {
            val nextZ = z + step
            val depthFactor = max(0f, 1f - (z - playerZ) / renderDistance)
            if (depthFactor <= 0) {
                z += step
                continue
            }

            val y1 = getTrackY(z)
            val y2 = getTrackY(nextZ)
            val biome = getBiome(z)

            // Select colors based on biome
            val isEven = (z / step).toInt() % 2 == 0
            val roadColor = when (biome) {
                Biome.GREENERY -> if (isEven) Color.parseColor("#0B2411") else Color.parseColor("#051408")
                Biome.DESERT -> if (isEven) Color.parseColor("#2A1604") else Color.parseColor("#1C0F02")
                Biome.SNOWY -> if (isEven) Color.parseColor("#0E2230") else Color.parseColor("#06131C")
            }

            val riverColor1: Int
            val riverColor2: Int
            val borderColor: Int
            val laneColor: Int

            when (biome) {
                Biome.GREENERY -> {
                    riverColor1 = Color.parseColor("#00A2FF")
                    riverColor2 = Color.parseColor("#0066FF")
                    borderColor = Color.parseColor("#4CAF50")
                    laneColor = Color.parseColor("#00FF8A")
                }
                Biome.DESERT -> {
                    riverColor1 = Color.parseColor("#FF4500")
                    riverColor2 = Color.parseColor("#FF9100")
                    borderColor = Color.parseColor("#FFCC00")
                    laneColor = Color.parseColor("#FFE45C")
                }
                Biome.SNOWY -> {
                    riverColor1 = Color.parseColor("#80DEEA")
                    riverColor2 = Color.parseColor("#E0F7FA")
                    borderColor = Color.parseColor("#B2EBF2")
                    laneColor = Color.parseColor("#35F3FF")
                }
            }

            val rb1 = Vector3D(-4.5f, y1, z)
            val rb2 = Vector3D(-4.5f, y2, nextZ)
            val rb3 = Vector3D(4.5f, y2, nextZ)
            val rb4 = Vector3D(4.5f, y1, z)

            val prb1 = PointF()
            val prb2 = PointF()
            val prb3 = PointF()
            val prb4 = PointF()

            if (project(rb1, prb1) && project(rb2, prb2) && project(rb3, prb3) && project(rb4, prb4)) {
                val path = Path().apply {
                    moveTo(prb1.x, prb1.y)
                    lineTo(prb2.x, prb2.y)
                    lineTo(prb3.x, prb3.y)
                    lineTo(prb4.x, prb4.y)
                    close()
                }
                objectPaint.reset()
                objectPaint.isAntiAlias = true
                objectPaint.style = Paint.Style.FILL
                objectPaint.color = roadColor
                canvas.drawPath(path, objectPaint)
            }

            // 2. Draw Side Rivers (left and right)
            val lavaPulse1 = sin(System.currentTimeMillis() * 0.005f - z * 0.08f) * 0.5f + 0.5f
            val lavaPulse2 = sin(System.currentTimeMillis() * 0.005f - nextZ * 0.08f) * 0.5f + 0.5f

            val r1Red = (Color.red(riverColor1) + (Color.red(riverColor2) - Color.red(riverColor1)) * lavaPulse1).toInt().coerceIn(0, 255)
            val r1Green = (Color.green(riverColor1) + (Color.green(riverColor2) - Color.green(riverColor1)) * lavaPulse1).toInt().coerceIn(0, 255)
            val r1Blue = (Color.blue(riverColor1) + (Color.blue(riverColor2) - Color.blue(riverColor1)) * lavaPulse1).toInt().coerceIn(0, 255)
            val avgColor1 = Color.rgb(r1Red, r1Green, r1Blue)

            val r2Red = (Color.red(riverColor1) + (Color.red(riverColor2) - Color.red(riverColor1)) * lavaPulse2).toInt().coerceIn(0, 255)
            val r2Green = (Color.green(riverColor1) + (Color.green(riverColor2) - Color.green(riverColor1)) * lavaPulse2).toInt().coerceIn(0, 255)
            val r2Blue = (Color.blue(riverColor1) + (Color.blue(riverColor2) - Color.blue(riverColor1)) * lavaPulse2).toInt().coerceIn(0, 255)
            val avgColor2 = Color.rgb(r2Red, r2Green, r2Blue)

            // Left River
            val lr1 = Vector3D(-9.0f, y1 - 0.15f, z)
            val lr2 = Vector3D(-9.0f, y2 - 0.15f, nextZ)
            val lr3 = Vector3D(-4.5f, y2 - 0.15f, nextZ)
            val lr4 = Vector3D(-4.5f, y1 - 0.15f, z)

            val plr1 = PointF()
            val plr2 = PointF()
            val plr3 = PointF()
            val plr4 = PointF()

            if (project(lr1, plr1) && project(lr2, plr2) && project(lr3, plr3) && project(lr4, plr4)) {
                val path = Path().apply {
                    moveTo(plr1.x, plr1.y)
                    lineTo(plr2.x, plr2.y)
                    lineTo(plr3.x, plr3.y)
                    lineTo(plr4.x, plr4.y)
                    close()
                }
                objectPaint.reset()
                objectPaint.isAntiAlias = true
                objectPaint.style = Paint.Style.FILL
                val avgColor = Color.rgb((r1Red + r2Red) / 2, (r1Green + r2Green) / 2, (r1Blue + r2Blue) / 2)
                objectPaint.color = adjustAlpha(avgColor, depthFactor)
                canvas.drawPath(path, objectPaint)

                trackPaint.style = Paint.Style.STROKE
                trackPaint.color = adjustAlpha(borderColor, depthFactor * 0.5f)
                trackPaint.strokeWidth = max(1f, 3f * depthFactor)
                canvas.drawLine(plr4.x, plr4.y, plr3.x, plr3.y, trackPaint)
            }

            // Right River
            val rr1 = Vector3D(4.5f, y1 - 0.15f, z)
            val rr2 = Vector3D(4.5f, y2 - 0.15f, nextZ)
            val rr3 = Vector3D(9.0f, y2 - 0.15f, nextZ)
            val rr4 = Vector3D(9.0f, y1 - 0.15f, z)

            val prr1 = PointF()
            val prr2 = PointF()
            val prr3 = PointF()
            val prr4 = PointF()

            if (project(rr1, prr1) && project(rr2, prr2) && project(rr3, prr3) && project(rr4, prr4)) {
                val path = Path().apply {
                    moveTo(prr1.x, prr1.y)
                    lineTo(prr2.x, prr2.y)
                    lineTo(prr3.x, prr3.y)
                    lineTo(prr4.x, prr4.y)
                    close()
                }
                objectPaint.reset()
                objectPaint.isAntiAlias = true
                objectPaint.style = Paint.Style.FILL
                val avgColor = Color.rgb((r1Red + r2Red) / 2, (r1Green + r2Green) / 2, (r1Blue + r2Blue) / 2)
                objectPaint.color = adjustAlpha(avgColor, depthFactor)
                canvas.drawPath(path, objectPaint)

                trackPaint.style = Paint.Style.STROKE
                trackPaint.color = adjustAlpha(borderColor, depthFactor * 0.5f)
                trackPaint.strokeWidth = max(1f, 3f * depthFactor)
                canvas.drawLine(prr1.x, prr1.y, prr2.x, prr2.y, trackPaint)
            }

            // 3. Draw Lane Dividers
            val laneX = floatArrayOf(-4.5f, -1.5f, 1.5f, 4.5f)
            for (x in laneX) {
                val pStart = Vector3D(x, y1, z)
                val pEnd = Vector3D(x, y2, nextZ)

                val ps = PointF()
                val pe = PointF()

                if (project(pStart, ps) && project(pEnd, pe)) {
                    glowPaint.color = adjustAlpha(laneColor, 0.15f * depthFactor)
                    glowPaint.strokeWidth = max(2f, 14f * depthFactor)
                    canvas.drawLine(ps.x, ps.y, pe.x, pe.y, glowPaint)

                    trackPaint.color = adjustAlpha(laneColor, 0.7f * depthFactor)
                    trackPaint.strokeWidth = max(1f, 4f * depthFactor)
                    canvas.drawLine(ps.x, ps.y, pe.x, pe.y, trackPaint)
                }
            }

            // 4. Draw horizontal divider lines
            val hLeft = Vector3D(-4.5f, y1, z)
            val hRight = Vector3D(4.5f, y1, z)
            val phs = PointF()
            val phe = PointF()
            if (project(hLeft, phs) && project(hRight, phe)) {
                trackPaint.color = adjustAlpha(laneColor, depthFactor * 0.4f)
                trackPaint.strokeWidth = max(1f, 4f * depthFactor)
                canvas.drawLine(phs.x, phs.y, phe.x, phe.y, trackPaint)
            }

            z += step
        }
    }

    private fun drawPlayer(canvas: Canvas, x: Float, z: Float, element: Element, skin: Skin, overchargeActive: Boolean, evolutionLevel: Int, shieldTimer: Float, magnetTimer: Float, invisibilityTimer: Float) {
        val playerPos = Vector3D(x, 0.5f + getTrackY(z), z)
        if (project(playerPos, p1)) {
            val rz = playerPos.z - cameraPos.z
            if (rz <= 0) return
            val radius = (fov / rz) * 0.5f // player radius is 0.5 units (1.0 scale)

            val isInvis = invisibilityTimer > 0f
            val baseAlpha = if (isInvis) 0.35f else 1.0f

            // Select color based on element, skin, overcharge
            val coreColor = if (overchargeActive) Color.WHITE else skin.primaryColor
            var glowColor = if (overchargeActive) Color.parseColor("#FF00FF") else element.primaryColor
            if (isInvis) {
                glowColor = Color.parseColor("#B388FF")
            }

            // 1. Draw Outer Glow
            glowPaint.style = Paint.Style.FILL
            glowPaint.shader = RadialGradient(
                p1.x, p1.y, radius * 2.5f,
                adjustAlpha(glowColor, if (isInvis) 0.3f else 0.6f), Color.TRANSPARENT, Shader.TileMode.CLAMP
            )
            canvas.drawCircle(p1.x, p1.y, radius * 2.5f, glowPaint)
            glowPaint.shader = null

            // 2. Draw Pulse Animation Effect
            val pulse = 1f + 0.15f * sin((System.currentTimeMillis() % 1000) / 1000f * 2 * Math.PI.toFloat())
            glowPaint.style = Paint.Style.STROKE
            glowPaint.strokeWidth = 3f
            glowPaint.color = adjustAlpha(glowColor, if (isInvis) 0.4f else 0.8f)
            canvas.drawCircle(p1.x, p1.y, radius * pulse, glowPaint)

            // 3. Draw Core Orb
            objectPaint.reset()
            objectPaint.isAntiAlias = true
            objectPaint.style = Paint.Style.FILL
            objectPaint.shader = RadialGradient(
                p1.x - radius * 0.2f, p1.y - radius * 0.2f, radius,
                adjustAlpha(coreColor, baseAlpha), adjustAlpha(skin.secondaryColor, baseAlpha * 0.8f), Shader.TileMode.CLAMP
            )
            canvas.drawCircle(p1.x, p1.y, radius, objectPaint)
            objectPaint.shader = null

            drawSkinDetails(canvas, p1.x, p1.y, radius, skin, glowColor)

            if (evolutionLevel >= 2) {
                trackPaint.style = Paint.Style.STROKE
                trackPaint.strokeWidth = 4f
                trackPaint.color = adjustAlpha(glowColor, 0.9f)
                canvas.drawCircle(p1.x, p1.y, radius * 1.45f, trackPaint)
            }
            if (evolutionLevel >= 3) {
                objectPaint.style = Paint.Style.FILL
                objectPaint.color = adjustAlpha(glowColor, 0.55f)
                val wingY = p1.y + radius * 0.05f
                canvas.drawOval(p1.x - radius * 2.8f, wingY - radius * 0.55f, p1.x - radius * 1.1f, wingY + radius * 0.35f, objectPaint)
                canvas.drawOval(p1.x + radius * 1.1f, wingY - radius * 0.55f, p1.x + radius * 2.8f, wingY + radius * 0.35f, objectPaint)
            }
            if (evolutionLevel >= 4) {
                trackPaint.style = Paint.Style.STROKE
                trackPaint.strokeWidth = 5f
                trackPaint.color = Color.WHITE
                val crownY = p1.y - radius * 1.55f
                canvas.drawLine(p1.x - radius * 0.8f, crownY + radius * 0.4f, p1.x - radius * 0.35f, crownY - radius * 0.25f, trackPaint)
                canvas.drawLine(p1.x - radius * 0.35f, crownY - radius * 0.25f, p1.x, crownY + radius * 0.35f, trackPaint)
                canvas.drawLine(p1.x, crownY + radius * 0.35f, p1.x + radius * 0.35f, crownY - radius * 0.25f, trackPaint)
                canvas.drawLine(p1.x + radius * 0.35f, crownY - radius * 0.25f, p1.x + radius * 0.8f, crownY + radius * 0.4f, trackPaint)
            }
            if (evolutionLevel >= 5) {
                glowPaint.style = Paint.Style.STROKE
                glowPaint.strokeWidth = 8f
                glowPaint.color = adjustAlpha(Color.WHITE, 0.65f)
                canvas.drawCircle(p1.x, p1.y, radius * 2.1f, glowPaint)
            }

            // Draw active Shield bubble
            if (shieldTimer > 0f) {
                val shieldColor = Color.parseColor("#35F3FF")
                val sPulse = 1f + 0.08f * sin((System.currentTimeMillis() % 600) / 600f * 2 * Math.PI.toFloat())
                glowPaint.style = Paint.Style.STROKE
                glowPaint.strokeWidth = 6f
                glowPaint.color = adjustAlpha(shieldColor, 0.8f)
                canvas.drawCircle(p1.x, p1.y, radius * 1.35f * sPulse, glowPaint)

                objectPaint.reset()
                objectPaint.isAntiAlias = true
                objectPaint.style = Paint.Style.FILL
                objectPaint.color = adjustAlpha(shieldColor, 0.12f)
                canvas.drawCircle(p1.x, p1.y, radius * 1.35f * sPulse, objectPaint)
            }

            // Draw active Magnet arcs
            if (magnetTimer > 0f) {
                val magnetColor = Color.parseColor("#FFD700")
                val mPulse = (System.currentTimeMillis() % 800) / 800f
                glowPaint.style = Paint.Style.STROKE
                glowPaint.strokeWidth = 5f
                glowPaint.color = adjustAlpha(magnetColor, 1.0f - mPulse)
                val arcRadius = radius * (1.2f + mPulse * 0.6f)
                val rectF = RectF(p1.x - arcRadius, p1.y - arcRadius, p1.x + arcRadius, p1.y + arcRadius)
                canvas.drawArc(rectF, -140f, 100f, false, glowPaint)
                canvas.drawArc(rectF, -260f, 100f, false, glowPaint)
            }
        }
    }

    private fun drawSkinDetails(canvas: Canvas, cx: Float, cy: Float, radius: Float, skin: Skin, glowColor: Int) {
        objectPaint.shader = null
        objectPaint.style = Paint.Style.FILL
        trackPaint.style = Paint.Style.STROKE

        when (skin.id) {
            "kitty_pop" -> {
                objectPaint.color = skin.primaryColor
                val leftEar = Path().apply {
                    moveTo(cx - radius * 0.85f, cy - radius * 0.45f)
                    lineTo(cx - radius * 0.45f, cy - radius * 1.35f)
                    lineTo(cx - radius * 0.08f, cy - radius * 0.55f)
                    close()
                }
                val rightEar = Path().apply {
                    moveTo(cx + radius * 0.85f, cy - radius * 0.45f)
                    lineTo(cx + radius * 0.45f, cy - radius * 1.35f)
                    lineTo(cx + radius * 0.08f, cy - radius * 0.55f)
                    close()
                }
                canvas.drawPath(leftEar, objectPaint)
                canvas.drawPath(rightEar, objectPaint)
                objectPaint.color = Color.parseColor("#FF5FB7")
                canvas.drawCircle(cx + radius * 0.42f, cy - radius * 0.55f, radius * 0.18f, objectPaint)
            }
            "dragon_fire" -> {
                objectPaint.color = Color.parseColor("#FFD166")
                val horns = Path().apply {
                    moveTo(cx - radius * 0.48f, cy - radius * 0.78f)
                    lineTo(cx - radius * 0.22f, cy - radius * 1.55f)
                    lineTo(cx, cy - radius * 0.82f)
                    lineTo(cx + radius * 0.22f, cy - radius * 1.55f)
                    lineTo(cx + radius * 0.48f, cy - radius * 0.78f)
                }
                canvas.drawPath(horns, objectPaint)
                objectPaint.color = adjustAlpha(glowColor, 0.55f)
                canvas.drawOval(cx - radius * 2.1f, cy - radius * 0.42f, cx - radius * 0.88f, cy + radius * 0.55f, objectPaint)
                canvas.drawOval(cx + radius * 0.88f, cy - radius * 0.42f, cx + radius * 2.1f, cy + radius * 0.55f, objectPaint)
            }
            "dino_mint" -> {
                objectPaint.color = Color.parseColor("#B9FFD0")
                for (i in -2..2) {
                    val x = cx + i * radius * 0.25f
                    val spike = Path().apply {
                        moveTo(x - radius * 0.1f, cy - radius * 0.82f)
                        lineTo(x, cy - radius * 1.28f)
                        lineTo(x + radius * 0.1f, cy - radius * 0.82f)
                        close()
                    }
                    canvas.drawPath(spike, objectPaint)
                }
            }
            "shadow_ninja" -> {
                objectPaint.color = Color.parseColor("#DD050712")
                canvas.drawRect(cx - radius * 0.85f, cy - radius * 0.18f, cx + radius * 0.85f, cy + radius * 0.18f, objectPaint)
                objectPaint.color = Color.parseColor("#8C5CFF")
                canvas.drawCircle(cx - radius * 0.28f, cy, radius * 0.1f, objectPaint)
                canvas.drawCircle(cx + radius * 0.28f, cy, radius * 0.1f, objectPaint)
            }
            "royal_panda" -> {
                objectPaint.color = Color.parseColor("#20242E")
                canvas.drawCircle(cx - radius * 0.62f, cy - radius * 0.62f, radius * 0.28f, objectPaint)
                canvas.drawCircle(cx + radius * 0.62f, cy - radius * 0.62f, radius * 0.28f, objectPaint)
                objectPaint.color = Color.parseColor("#FFD166")
                canvas.drawCircle(cx, cy - radius * 0.12f, radius * 0.14f, objectPaint)
            }
        }
    }

    private fun drawObstacle(canvas: Canvas, obs: Obstacle) {
        // Draw obstacle based on its type
        when (obs.type) {
            ObstacleType.ELEMENT_GATE, ObstacleType.DYNAMIC_GATE, ObstacleType.ROTATING_GATE -> {
                drawGate(canvas, obs)
            }
            ObstacleType.RIFT_ORB -> {
                drawRiftOrb(canvas, obs)
            }
            ObstacleType.MULTI_TUNNEL -> {
                drawMultiTunnel(canvas, obs)
            }
            ObstacleType.BOOST_PAD -> {
                drawBoostPad(canvas, obs)
            }
            ObstacleType.CANNON -> {
                drawCannon(canvas, obs)
            }
            ObstacleType.FLOOR_HAZARD -> {
                drawFloorHazard(canvas, obs)
            }
            ObstacleType.HOLE_WALL -> {
                drawHoleWall(canvas, obs)
            }
        }
    }

    private fun drawGate(canvas: Canvas, obs: Obstacle) {
        val color = if (obs.isGolden) Color.parseColor("#FFD700") else obs.requiredElement.primaryColor
        
        // Define 4 corners of the gate in local 3D coordinates
        // Width is 2.2f (spans the lane), height is 3.5f
        val w = obs.width * 0.5f
        val h = obs.height
        val z = obs.position.z
        val trackY = getTrackY(z)

        // Define corners in local-to-track coordinates (i.e. Y offset by trackY)
        val corners = arrayOf(
            Vector3D(-w, trackY, z),
            Vector3D(-w, trackY + h, z),
            Vector3D(w, trackY + h, z),
            Vector3D(w, trackY, z)
        )

        if (obs.type == ObstacleType.ROTATING_GATE) {
            val rad = obs.rotationAngle * Math.PI.toFloat() / 180f
            val cosR = cos(rad)
            val sinR = sin(rad)
            val centerY = trackY + h / 2f
            for (c in corners) {
                // Rotate relative to local gate center (0, centerY)
                val dy = c.y - centerY
                val dx = c.x // c.x is already local
                
                val rx = dx * cosR - dy * sinR
                val ry = centerY + dx * sinR + dy * cosR
                c.x = obs.position.x + rx
                c.y = ry
            }
        } else {
            // Apply lane offset
            for (c in corners) {
                c.x += obs.position.x
            }
        }

        // Project corners
        val sc = Array(4) { PointF() }
        var allProjected = true
        for (i in 0..3) {
            if (!project(corners[i], sc[i])) {
                allProjected = false
                break
            }
        }

        if (allProjected) {
            // Draw neon glow outline
            val path = Path().apply {
                moveTo(sc[0].x, sc[0].y)
                lineTo(sc[1].x, sc[1].y)
                lineTo(sc[2].x, sc[2].y)
                lineTo(sc[3].x, sc[3].y)
                close()
            }

            objectPaint.reset()
            objectPaint.isAntiAlias = true
            objectPaint.style = Paint.Style.FILL
            objectPaint.shader = LinearGradient(
                sc[0].x, sc[1].y, sc[2].x, sc[3].y,
                adjustAlpha(color, 0.20f),
                adjustAlpha(Color.WHITE, 0.04f),
                Shader.TileMode.CLAMP
            )
            canvas.drawPath(path, objectPaint)
            objectPaint.shader = null

            val biome = getBiome(z)
            // Outer Neon Glow
            glowPaint.style = Paint.Style.STROKE
            glowPaint.strokeWidth = 18f
            glowPaint.color = adjustAlpha(color, 0.22f)
            canvas.drawPath(path, glowPaint)

            // Inner Core Line (Frosted Frame if snowy)
            trackPaint.style = Paint.Style.STROKE
            trackPaint.color = when (biome) {
                Biome.SNOWY -> Color.parseColor("#EAF8FF") // Frosted white/cyan
                Biome.DESERT -> adjustAlpha(color, 0.8f) // Orange sand blend
                else -> color
            }
            trackPaint.strokeWidth = 5f
            canvas.drawPath(path, trackPaint)

            // Draw Element Icon (vector symbol) in center of gate
            val center3D = Vector3D(obs.position.x, trackY + h / 2f, z)
            if (project(center3D, p1)) {
                val rz = (z - cameraPos.z).coerceAtLeast(0.1f)
                val iconRadius = (fov / rz) * 0.55f
                if (obs.isGolden) {
                    textPaint.color = Color.parseColor("#FFD700")
                    textPaint.textSize = max(14f, iconRadius * 0.8f)
                    canvas.drawText("GOLD", p1.x, p1.y + textPaint.textSize * 0.35f, textPaint)
                } else {
                    ElementSymbolDrawer.draw(canvas, obs.requiredElement, p1.x, p1.y, iconRadius.coerceIn(8f, 54f))
                }
            }
        }
    }

    private fun drawRiftOrb(canvas: Canvas, obs: Obstacle) {
        val center = Vector3D(obs.position.x, 1.05f + getTrackY(obs.position.z), obs.position.z)
        if (!project(center, p1)) return

        val rz = center.z - cameraPos.z
        if (rz <= 0) return
        val radius = (fov / rz) * 0.72f
        val time = (System.currentTimeMillis() % 2400) / 2400f * 2f * Math.PI.toFloat()
        val coreColor = Color.parseColor("#FF3157")
        val arcColor = Color.parseColor("#B388FF")

        glowPaint.style = Paint.Style.FILL
        glowPaint.shader = RadialGradient(
            p1.x, p1.y, radius * 3.6f,
            adjustAlpha(coreColor, 0.52f),
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(p1.x, p1.y, radius * 3.6f, glowPaint)
        glowPaint.shader = null

        objectPaint.reset()
        objectPaint.isAntiAlias = true
        objectPaint.style = Paint.Style.FILL
        objectPaint.shader = RadialGradient(
            p1.x - radius * 0.25f, p1.y - radius * 0.25f, radius * 1.4f,
            Color.WHITE,
            coreColor,
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(p1.x, p1.y, radius, objectPaint)
        objectPaint.shader = null

        trackPaint.style = Paint.Style.STROKE
        trackPaint.strokeWidth = 4f
        trackPaint.color = arcColor
        canvas.drawOval(
            p1.x - radius * 1.7f,
            p1.y - radius * 0.62f + sin(time) * radius * 0.1f,
            p1.x + radius * 1.7f,
            p1.y + radius * 0.62f + sin(time) * radius * 0.1f,
            trackPaint
        )
        trackPaint.color = Color.WHITE
        trackPaint.strokeWidth = 3f
        canvas.drawOval(
            p1.x - radius * 0.72f,
            p1.y - radius * 1.55f,
            p1.x + radius * 0.72f,
            p1.y + radius * 1.55f,
            trackPaint
        )

    }

    private fun drawRetiredBarrier(canvas: Canvas, obs: Obstacle) {
        val color = Color.parseColor("#E0E0E0") // metallic/neutral obstacle
        val w = obs.width * 0.5f
        val h = obs.height
        val z = obs.position.z
        
        // Define barrier vertices
        val corners = arrayOf(
            Vector3D(obs.position.x - w, 0f, z),
            Vector3D(obs.position.x - w, h, z),
            Vector3D(obs.position.x + w, h, z),
            Vector3D(obs.position.x + w, 0f, z)
        )

        val sc = Array(4) { PointF() }
        var allProjected = true
        for (i in 0..3) {
            if (!project(corners[i], sc[i])) {
                allProjected = false
                break
            }
        }

        if (allProjected) {
            val path = Path().apply {
                moveTo(sc[0].x, sc[0].y)
                lineTo(sc[1].x, sc[1].y)
                lineTo(sc[2].x, sc[2].y)
                lineTo(sc[3].x, sc[3].y)
                close()
            }

            // Fill solid metallic red/orange barrier warning
            objectPaint.reset()
            objectPaint.isAntiAlias = true
            objectPaint.style = Paint.Style.FILL
            objectPaint.shader = LinearGradient(
                sc[0].x, sc[1].y, sc[2].x, sc[3].y,
                Color.parseColor("#AA2A0707"),
                Color.parseColor("#55300018"),
                Shader.TileMode.CLAMP
            )
            canvas.drawPath(path, objectPaint)
            objectPaint.shader = null

            // Glow red outline
            glowPaint.style = Paint.Style.STROKE
            glowPaint.strokeWidth = 18f
            glowPaint.color = adjustAlpha(Color.parseColor("#FF3157"), 0.35f)
            canvas.drawPath(path, glowPaint)

            // Core outline
            trackPaint.style = Paint.Style.STROKE
            trackPaint.color = Color.parseColor("#FF3157")
            trackPaint.strokeWidth = 5f
            canvas.drawPath(path, trackPaint)
            trackPaint.strokeWidth = 3f
            trackPaint.color = adjustAlpha(Color.WHITE, 0.75f)
            canvas.drawLine(sc[0].x, sc[0].y, sc[2].x, sc[2].y, trackPaint)
            canvas.drawLine(sc[1].x, sc[1].y, sc[3].x, sc[3].y, trackPaint)
            
            // Retired barrier warning marker.
            val center3D = Vector3D(obs.position.x, h / 2f, z)
            if (project(center3D, p1)) {
                textPaint.color = Color.WHITE
                textPaint.textSize = max(16f, (fov / (z - cameraPos.z)) * 0.45f)
                canvas.drawText("", p1.x, p1.y + textPaint.textSize * 0.35f, textPaint)
            }
        }
    }

    private fun drawMultiTunnel(canvas: Canvas, obs: Obstacle) {
        // Three clean lane panels, one per playable lane.
        val z = obs.position.z
        val h = obs.height
        val trackY = getTrackY(z)
        val allElements = Element.values()
        val offset = obs.id % allElements.size
        val elements = arrayOf(
            allElements[offset],
            allElements[(offset + 1) % allElements.size],
            allElements[(offset + 2) % allElements.size]
        )
        val totalW = 9f // wide tunnel spanning all 3 lanes (X from -4.5 to +4.5)
        val sliceW = totalW / elements.size

        for (i in elements.indices) {
            val el = elements[i]
            val leftX = -totalW/2f + i * sliceW
            val rightX = leftX + sliceW
            val color = el.primaryColor

            // Draw a vertical frame slice for each element
            val corners = arrayOf(
                Vector3D(leftX, trackY, z),
                Vector3D(leftX, trackY + h, z),
                Vector3D(rightX, trackY + h, z),
                Vector3D(rightX, trackY, z)
            )

            val sc = Array(4) { PointF() }
            var allProjected = true
            for (j in 0..3) {
                if (!project(corners[j], sc[j])) {
                    allProjected = false
                    break
                }
            }

            if (allProjected) {
                val path = Path().apply {
                    moveTo(sc[0].x, sc[0].y)
                    lineTo(sc[1].x, sc[1].y)
                    lineTo(sc[2].x, sc[2].y)
                    lineTo(sc[3].x, sc[3].y)
                    close()
                }

                // Semi-transparent glass fill for each lane panel
                objectPaint.reset()
                objectPaint.isAntiAlias = true
                objectPaint.style = Paint.Style.FILL
                objectPaint.shader = LinearGradient(
                    sc[0].x, sc[1].y, sc[2].x, sc[3].y,
                    adjustAlpha(color, 0.28f),
                    adjustAlpha(Color.WHITE, 0.04f),
                    Shader.TileMode.CLAMP
                )
                canvas.drawPath(path, objectPaint)
                objectPaint.shader = null

                // Neon borders
                glowPaint.style = Paint.Style.STROKE
                glowPaint.strokeWidth = 10f
                glowPaint.color = adjustAlpha(color, 0.18f)
                canvas.drawPath(path, glowPaint)
                trackPaint.style = Paint.Style.STROKE
                trackPaint.color = color
                trackPaint.strokeWidth = 4f
                canvas.drawPath(path, trackPaint)

                // Draw element vector symbol in center of each tunnel slice
                val center3D = Vector3D(leftX + sliceW/2f, trackY + h/2f, z)
                if (project(center3D, p1)) {
                    val rz = (z - cameraPos.z).coerceAtLeast(0.1f)
                    val iconRadius = (fov / rz * 0.46f).coerceIn(8f, 50f)
                    ElementSymbolDrawer.draw(canvas, el, p1.x, p1.y, iconRadius)
                }
            }
        }
    }

    private fun drawCoin(canvas: Canvas, coin: Coin) {
        val center = Vector3D(coin.position.x, coin.position.y + getTrackY(coin.position.z), coin.position.z)
        if (project(center, p1)) {
            val rz = center.z - cameraPos.z
            if (rz <= 0) return
            val radius = (fov / rz) * coin.radius

            glowPaint.style = Paint.Style.FILL
            glowPaint.shader = RadialGradient(
                p1.x, p1.y, radius * 3.0f,
                adjustAlpha(Color.parseColor("#FFD54A"), 0.55f),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
            canvas.drawCircle(p1.x, p1.y, radius * 3.0f, glowPaint)
            glowPaint.shader = null

            objectPaint.reset()
            objectPaint.isAntiAlias = true
            objectPaint.style = Paint.Style.FILL
            objectPaint.shader = RadialGradient(
                p1.x - radius * 0.3f, p1.y - radius * 0.35f, radius * 1.2f,
                Color.WHITE,
                Color.parseColor("#FFC400"),
                Shader.TileMode.CLAMP
            )
            canvas.drawCircle(p1.x, p1.y, radius * 1.05f, objectPaint)
            objectPaint.shader = null

            // Project a 3D-like spinning diamond shape
            val rad = coin.rotationAngle * Math.PI.toFloat() / 180f
            val cosR = cos(rad)
            val sinR = sin(rad)

            // Diamond vertices
            val dWidth = radius
            val dHeight = radius * 1.3f

            val pts = arrayOf(
                PointF(p1.x, p1.y - dHeight),                 // Top
                PointF(p1.x + dWidth * cosR, p1.y),          // Right
                PointF(p1.x, p1.y + dHeight),                 // Bottom
                PointF(p1.x - dWidth * cosR, p1.y)           // Left
            )

            val path = Path().apply {
                moveTo(pts[0].x, pts[0].y)
                lineTo(pts[1].x, pts[1].y)
                lineTo(pts[2].x, pts[2].y)
                lineTo(pts[3].x, pts[3].y)
                close()
            }

            // Fill yellow gold gradient
            objectPaint.reset()
            objectPaint.isAntiAlias = true
            objectPaint.style = Paint.Style.FILL
            objectPaint.shader = RadialGradient(
                p1.x, p1.y, radius * 1.5f,
                Color.parseColor("#FFFFE0"), Color.parseColor("#FFD700"), Shader.TileMode.CLAMP
            )
            canvas.drawPath(path, objectPaint)
            objectPaint.shader = null

            // Glow yellow
            glowPaint.style = Paint.Style.STROKE
            glowPaint.strokeWidth = 6f
            glowPaint.color = adjustAlpha(Color.parseColor("#FFD700"), 0.4f)
            canvas.drawPath(path, glowPaint)

            // Outline
            trackPaint.style = Paint.Style.STROKE
            trackPaint.color = Color.parseColor("#FFB300")
            trackPaint.strokeWidth = 3f
            canvas.drawPath(path, trackPaint)

            trackPaint.color = Color.WHITE
            trackPaint.strokeWidth = 2f
            canvas.drawCircle(p1.x, p1.y, radius * 0.55f, trackPaint)
        }
    }

    private fun drawParticles(canvas: Canvas, particles: List<Particle>) {
        trackPaint.style = Paint.Style.FILL
        val tempPos = Vector3D()
        for (p in particles) {
            if (!p.active) continue
            tempPos.set(p.position.x, p.position.y + getTrackY(p.position.z), p.position.z)
            if (project(tempPos, p1)) {
                val rz = p.position.z - cameraPos.z
                if (rz <= 0) continue
                
                val sizeOnScreen = max(1f, (fov / rz) * p.size)
                trackPaint.color = adjustAlpha(p.color, p.alpha)
                canvas.drawCircle(p1.x, p1.y, sizeOnScreen, trackPaint)
            }
        }
    }

    private fun drawPowerUp(canvas: Canvas, p: PowerUp) {
        val center = Vector3D(p.position.x, p.position.y + getTrackY(p.position.z), p.position.z)
        if (project(center, p1)) {
            val rz = center.z - cameraPos.z
            if (rz <= 0) return
            val radius = (fov / rz) * p.radius

            val color = Color.parseColor(p.type.colorCode)

            // Outer glow
            glowPaint.style = Paint.Style.FILL
            glowPaint.shader = RadialGradient(
                p1.x, p1.y, radius * 2.8f,
                adjustAlpha(color, 0.45f),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
            canvas.drawCircle(p1.x, p1.y, radius * 2.8f, glowPaint)
            glowPaint.shader = null

            // Spin core
            val pulse = 1f + 0.12f * sin((System.currentTimeMillis() % 800) / 800f * 2 * Math.PI.toFloat())
            val rad = p.rotationAngle * Math.PI.toFloat() / 180f
            val cosR = cos(rad)
            val sinR = sin(rad)

            val dWidth = radius * pulse
            val dHeight = radius * 1.2f * pulse

            val pts = arrayOf(
                PointF(p1.x, p1.y - dHeight),
                PointF(p1.x + dWidth * cosR, p1.y),
                PointF(p1.x, p1.y + dHeight),
                PointF(p1.x - dWidth * cosR, p1.y)
            )

            val path = Path().apply {
                moveTo(pts[0].x, pts[0].y)
                lineTo(pts[1].x, pts[1].y)
                lineTo(pts[2].x, pts[2].y)
                lineTo(pts[3].x, pts[3].y)
                close()
            }

            objectPaint.reset()
            objectPaint.isAntiAlias = true
            objectPaint.style = Paint.Style.FILL
            objectPaint.color = adjustAlpha(color, 0.75f)
            canvas.drawPath(path, objectPaint)

            trackPaint.style = Paint.Style.STROKE
            trackPaint.color = Color.WHITE
            trackPaint.strokeWidth = 3f
            canvas.drawPath(path, trackPaint)

            // Draw short label tag instead of emoji
            textPaint.color = Color.WHITE
            textPaint.textSize = max(10f, (fov / (center.z - cameraPos.z)) * 0.32f)
            canvas.drawText(p.type.label, p1.x, p1.y + textPaint.textSize * 0.38f, textPaint)
        }
    }

    private fun drawBoostPad(canvas: Canvas, obs: Obstacle) {
        val color = obs.requiredElement.primaryColor
        val w = obs.width * 0.45f
        val z = obs.position.z
        val d = 2.5f
        val trackY = getTrackY(z)
        val trackY_d = getTrackY(z + d)
        val trackY_d35 = getTrackY(z + d * 0.35f)

        val corners = arrayOf(
            Vector3D(obs.position.x - w, trackY + 0.02f, z),
            Vector3D(obs.position.x, trackY_d + 0.02f, z + d),
            Vector3D(obs.position.x + w, trackY + 0.02f, z),
            Vector3D(obs.position.x, trackY_d35 + 0.02f, z + d * 0.35f)
        )

        val sc = Array(4) { PointF() }
        var allProjected = true
        for (i in 0..3) {
            if (!project(corners[i], sc[i])) {
                allProjected = false
                break
            }
        }

        if (allProjected) {
            val path = Path().apply {
                moveTo(sc[0].x, sc[0].y)
                lineTo(sc[1].x, sc[1].y)
                lineTo(sc[2].x, sc[2].y)
                lineTo(sc[3].x, sc[3].y)
                close()
            }

            objectPaint.reset()
            objectPaint.isAntiAlias = true
            objectPaint.style = Paint.Style.FILL
            objectPaint.color = adjustAlpha(color, 0.35f)
            canvas.drawPath(path, objectPaint)

            glowPaint.style = Paint.Style.STROKE
            glowPaint.strokeWidth = 10f
            glowPaint.color = color
            canvas.drawPath(path, glowPaint)
            
            // Draw flame jets on the pad instead of a symbol.
            val center3D = Vector3D(obs.position.x, getTrackY(z + d * 0.3f) + 0.02f, z + d * 0.3f)
            if (project(center3D, p1)) {
                val rz = (z - cameraPos.z).coerceAtLeast(0.1f)
                val ar = (fov / rz * 0.34f).coerceIn(8f, 34f)
                val flamePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.FILL
                }
                val time = System.currentTimeMillis() * 0.008f
                for (k in -1..1) {
                    val cx = p1.x + k * ar * 0.7f
                    val baseY = p1.y + ar * 0.55f
                    val sway = sin(time + k * 1.7f) * ar * 0.16f
                    val flameH = ar * (1.15f + 0.22f * sin(time * 1.3f + k))
                    val flameW = ar * 0.38f

                    val outer = Path().apply {
                        moveTo(cx - flameW, baseY)
                        cubicTo(cx - flameW + sway, baseY - flameH * 0.35f, cx - flameW * 0.2f + sway, baseY - flameH * 0.75f, cx + sway, baseY - flameH)
                        cubicTo(cx + flameW * 0.25f + sway, baseY - flameH * 0.72f, cx + flameW + sway, baseY - flameH * 0.35f, cx + flameW, baseY)
                        close()
                    }
                    flamePaint.shader = LinearGradient(
                        cx, baseY - flameH, cx, baseY,
                        Color.WHITE,
                        color,
                        Shader.TileMode.CLAMP
                    )
                    canvas.drawPath(outer, flamePaint)

                    val inner = Path().apply {
                        moveTo(cx - flameW * 0.45f, baseY)
                        cubicTo(cx - flameW * 0.24f, baseY - flameH * 0.32f, cx + sway, baseY - flameH * 0.58f, cx + sway * 0.5f, baseY - flameH * 0.72f)
                        cubicTo(cx + flameW * 0.22f, baseY - flameH * 0.46f, cx + flameW * 0.45f, baseY - flameH * 0.22f, cx + flameW * 0.45f, baseY)
                        close()
                    }
                    flamePaint.shader = LinearGradient(
                        cx, baseY - flameH * 0.72f, cx, baseY,
                        Color.WHITE,
                        Color.parseColor("#FFFFC247"),
                        Shader.TileMode.CLAMP
                    )
                    canvas.drawPath(inner, flamePaint)
                    flamePaint.shader = null
                }
            }
        }
    }

    private fun drawFloorHazard(canvas: Canvas, obs: Obstacle) {
        val z = obs.position.z
        val d = obs.depth
        val trackY = getTrackY(z)
        val trackY_d = getTrackY(z + d)
        val timeMs = System.currentTimeMillis()

        val blockedLanes = listOf(-1, 0, 1).filter { it != obs.lane }

        blockedLanes.forEach { lane ->
            val w = 1.35f
            val c1 = Vector3D(lane * 3f - w, trackY + 0.05f, z)
            val c2 = Vector3D(lane * 3f - w, trackY_d + 0.05f, z + d)
            val c3 = Vector3D(lane * 3f + w, trackY_d + 0.05f, z + d)
            val c4 = Vector3D(lane * 3f + w, trackY + 0.05f, z)

            val pA = PointF(); val pB = PointF(); val pC = PointF(); val pD = PointF()

            if (project(c1, pA) && project(c2, pB) && project(c3, pC) && project(c4, pD)) {
                // --- Ground base glow ---
                val basePath = Path().apply {
                    moveTo(pA.x, pA.y); lineTo(pB.x, pB.y)
                    lineTo(pC.x, pC.y); lineTo(pD.x, pD.y); close()
                }
                val pulse = sin(timeMs * 0.008f + lane * 1.5f) * 0.25f + 0.75f
                objectPaint.reset(); objectPaint.isAntiAlias = true
                objectPaint.style = Paint.Style.FILL
                objectPaint.color = adjustAlpha(Color.parseColor("#CC2200"), pulse * 0.55f)
                canvas.drawPath(basePath, objectPaint)

                glowPaint.style = Paint.Style.STROKE
                glowPaint.strokeWidth = 5f
                glowPaint.color = adjustAlpha(Color.parseColor("#FF6600"), 0.7f)
                canvas.drawPath(basePath, glowPaint)

                // --- Animated flame polygons along the hazard strip ---
                val laneScreenCx = (pA.x + pD.x) / 2f
                val baseY = (pA.y + pD.y) / 2f        // screen Y of front edge
                val stripW = (pD.x - pA.x).coerceAtLeast(8f)
                val numFlames = 5

                val flamePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.FILL
                    isAntiAlias = true
                }

                for (fi in 0 until numFlames) {
                    val frac = fi / (numFlames - 1).toFloat()
                    val fcx = laneScreenCx + (frac - 0.5f) * stripW * 0.85f
                    // sway offset: each flame sways slightly differently
                    val sway = sin(timeMs * 0.006f + fi * 1.2f + lane * 2.0f) * stripW * 0.045f
                    // flicker: height changes over time
                    val flicker = 0.7f + 0.3f * sin(timeMs * 0.012f + fi * 0.8f)
                    val flameH = stripW * 0.55f * flicker
                    val flameW = stripW * 0.14f

                    // outer flame (orange/red)
                    val outerPath = Path()
                    outerPath.moveTo(fcx - flameW, baseY)
                    outerPath.cubicTo(
                        fcx - flameW * 1.1f + sway, baseY - flameH * 0.4f,
                        fcx + flameW * 0.3f + sway, baseY - flameH * 0.8f,
                        fcx + sway * 1.2f, baseY - flameH
                    )
                    outerPath.cubicTo(
                        fcx - flameW * 0.3f + sway * 0.8f, baseY - flameH * 0.8f,
                        fcx + flameW * 1.1f + sway, baseY - flameH * 0.4f,
                        fcx + flameW, baseY
                    )
                    outerPath.close()

                    flamePaint.shader = LinearGradient(
                        fcx, baseY - flameH, fcx, baseY,
                        Color.parseColor("#FFFF4400"),
                        Color.parseColor("#88FF0000"),
                        Shader.TileMode.CLAMP
                    )
                    canvas.drawPath(outerPath, flamePaint)

                    // inner flame core (yellow/white)
                    val innerH = flameH * 0.55f
                    val innerW = flameW * 0.55f
                    val innerPath = Path()
                    innerPath.moveTo(fcx - innerW, baseY)
                    innerPath.cubicTo(
                        fcx - innerW + sway * 0.5f, baseY - innerH * 0.5f,
                        fcx + innerW * 0.5f + sway * 0.4f, baseY - innerH * 0.9f,
                        fcx + sway * 0.6f, baseY - innerH
                    )
                    innerPath.cubicTo(
                        fcx - innerW * 0.5f + sway * 0.3f, baseY - innerH * 0.9f,
                        fcx + innerW + sway * 0.5f, baseY - innerH * 0.5f,
                        fcx + innerW, baseY
                    )
                    innerPath.close()

                    flamePaint.shader = LinearGradient(
                        fcx, baseY - innerH, fcx, baseY,
                        Color.parseColor("#FFFFFFFF"),
                        Color.parseColor("#CCFFDD00"),
                        Shader.TileMode.CLAMP
                    )
                    canvas.drawPath(innerPath, flamePaint)
                    flamePaint.shader = null
                }
            }
        }
    }

    private fun drawHoleWall(canvas: Canvas, obs: Obstacle) {
        val z = obs.position.z
        val trackY = getTrackY(z)
        val h = obs.height
        val w = 4.5f
        
        val wallColor = Color.parseColor("#121824")
        val glowColor = Color.parseColor("#35F3FF")
        
        // 1. Draw Top Header Wall (spans -4.5 to 4.5, Y from trackY + 2.2 to trackY + h)
        val headerCorners = arrayOf(
            Vector3D(-w, trackY + 2.2f, z),
            Vector3D(-w, trackY + h, z),
            Vector3D(w, trackY + h, z),
            Vector3D(w, trackY + 2.2f, z)
        )
        val ph = Array(4) { PointF() }
        if (project(headerCorners[0], ph[0]) && project(headerCorners[1], ph[1]) && 
            project(headerCorners[2], ph[2]) && project(headerCorners[3], ph[3])) {
            val path = Path().apply {
                moveTo(ph[0].x, ph[0].y)
                lineTo(ph[1].x, ph[1].y)
                lineTo(ph[2].x, ph[2].y)
                lineTo(ph[3].x, ph[3].y)
                close()
            }
            objectPaint.reset()
            objectPaint.isAntiAlias = true
            objectPaint.style = Paint.Style.FILL
            objectPaint.color = wallColor
            canvas.drawPath(path, objectPaint)
            
            trackPaint.style = Paint.Style.STROKE
            trackPaint.color = adjustAlpha(glowColor, 0.5f)
            trackPaint.strokeWidth = 3f
            canvas.drawPath(path, trackPaint)
        }
        
        // 2. Draw Lower Blocked Panels (Y from trackY to trackY + 2.2)
        val blockedLanes = listOf(-1, 0, 1).filter { it != obs.lane }
        blockedLanes.forEach { lane ->
            val lX = lane * 3f - 1.5f
            val rX = lane * 3f + 1.5f
            val panelCorners = arrayOf(
                Vector3D(lX, trackY, z),
                Vector3D(lX, trackY + 2.2f, z),
                Vector3D(rX, trackY + 2.2f, z),
                Vector3D(rX, trackY, z)
            )
            val pp = Array(4) { PointF() }
            if (project(panelCorners[0], pp[0]) && project(panelCorners[1], pp[1]) && 
                project(panelCorners[2], pp[2]) && project(panelCorners[3], pp[3])) {
                val path = Path().apply {
                    moveTo(pp[0].x, pp[0].y)
                    lineTo(pp[1].x, pp[1].y)
                    lineTo(pp[2].x, pp[2].y)
                    lineTo(pp[3].x, pp[3].y)
                    close()
                }
                objectPaint.reset()
                objectPaint.isAntiAlias = true
                objectPaint.style = Paint.Style.FILL
                objectPaint.color = wallColor
                canvas.drawPath(path, objectPaint)
                
                trackPaint.style = Paint.Style.STROKE
                trackPaint.color = adjustAlpha(glowColor, 0.5f)
                trackPaint.strokeWidth = 3f
                canvas.drawPath(path, trackPaint)
            }
        }
        
        // 3. Draw Neon circular ring around the hole at obs.lane
        val center = Vector3D(obs.lane * 3f, trackY + 1.1f, z)
        if (project(center, p1)) {
            val rz = center.z - cameraPos.z
            if (rz > 0) {
                val rad = (fov / rz) * 1.05f
                glowPaint.style = Paint.Style.STROKE
                glowPaint.strokeWidth = 8f
                glowPaint.color = adjustAlpha(glowColor, 0.8f)
                canvas.drawCircle(p1.x, p1.y, rad, glowPaint)
                
                trackPaint.style = Paint.Style.STROKE
                trackPaint.strokeWidth = 3f
                trackPaint.color = Color.WHITE
                canvas.drawCircle(p1.x, p1.y, rad, trackPaint)
            }
        }
    }

    private fun drawCannon(canvas: Canvas, obs: Obstacle) {
        val z = obs.position.z
        val trackY = getTrackY(z)
        val sideX = if (obs.lane <= 0) -5.2f else 5.2f
        
        // 1. Draw Cannon Base & Head
        val basePt = Vector3D(sideX, trackY, z)
        val headPt = Vector3D(sideX, trackY + 0.8f, z)
        
        val pBase = PointF()
        val pHead = PointF()
        
        if (project(basePt, pBase) && project(headPt, pHead)) {
            // Draw Turret Post
            trackPaint.style = Paint.Style.STROKE
            trackPaint.strokeWidth = 8f
            trackPaint.color = Color.parseColor("#455A64")
            canvas.drawLine(pBase.x, pBase.y, pHead.x, pHead.y, trackPaint)
            
            // Draw Turret Head (Glowing red)
            val rz = headPt.z - cameraPos.z
            if (rz > 0) {
                val rad = (fov / rz) * 0.4f
                glowPaint.style = Paint.Style.FILL
                glowPaint.shader = RadialGradient(
                    pHead.x, pHead.y, rad * 2f,
                    adjustAlpha(Color.RED, 0.6f), Color.TRANSPARENT, Shader.TileMode.CLAMP
                )
                canvas.drawCircle(pHead.x, pHead.y, rad * 2f, glowPaint)
                glowPaint.shader = null
                
                objectPaint.reset()
                objectPaint.isAntiAlias = true
                objectPaint.style = Paint.Style.FILL
                objectPaint.color = Color.RED
                canvas.drawCircle(pHead.x, pHead.y, rad, objectPaint)
            }
        }
        
        // 2. Draw Lane Laser Warning/Beam targeting obs.lane
        val playerZ = cameraPos.z + 10f // Use camera or player Z position
        val distToPlayer = z - playerZ
        if (distToPlayer in 0f..45f) {
            // Diagonal beam from cannon head to the lane center
            val diagonalStart = Vector3D(sideX, trackY + 0.8f, z)
            val diagonalEnd = Vector3D(obs.lane * 3f, trackY + 0.5f, z)
            
            // Lane beam
            val laserStart = Vector3D(obs.lane * 3f, trackY + 0.5f, z)
            val laserEnd = Vector3D(obs.lane * 3f, getTrackY(playerZ) + 0.5f, playerZ)
            
            val pDiagStart = PointF()
            val pDiagEnd = PointF()
            val pStart = PointF()
            val pEnd = PointF()
            
            val drawDiag = project(diagonalStart, pDiagStart) && project(diagonalEnd, pDiagEnd)
            val drawLane = project(laserStart, pStart) && project(laserEnd, pEnd)
            
            val beamColor = Color.parseColor("#FF0055") // high-visibility pinkish-red
            
            // Helper paint for custom laser glow
            val laserPaint = Paint().apply {
                isAntiAlias = true
                style = Paint.Style.STROKE
            }
            
            if (distToPlayer < 18f) {
                // Thick glowing active plasma beam
                
                // 1. Draw diagonal beam from head to floor
                if (drawDiag) {
                    laserPaint.strokeWidth = 16f
                    laserPaint.color = adjustAlpha(beamColor, 0.4f)
                    canvas.drawLine(pDiagStart.x, pDiagStart.y, pDiagEnd.x, pDiagEnd.y, laserPaint)
                    
                    laserPaint.strokeWidth = 6f
                    laserPaint.color = Color.WHITE
                    canvas.drawLine(pDiagStart.x, pDiagStart.y, pDiagEnd.x, pDiagEnd.y, laserPaint)
                }
                
                // 2. Draw main lane beam
                if (drawLane) {
                    // Outer bloom
                    laserPaint.strokeWidth = 26f
                    laserPaint.color = adjustAlpha(beamColor, 0.35f)
                    canvas.drawLine(pStart.x, pStart.y, pEnd.x, pEnd.y, laserPaint)
                    
                    // Inner glow
                    laserPaint.strokeWidth = 14f
                    laserPaint.color = adjustAlpha(beamColor, 0.7f)
                    canvas.drawLine(pStart.x, pStart.y, pEnd.x, pEnd.y, laserPaint)
                    
                    // White hot core
                    laserPaint.strokeWidth = 5f
                    laserPaint.color = Color.WHITE
                    canvas.drawLine(pStart.x, pStart.y, pEnd.x, pEnd.y, laserPaint)
                }
            } else {
                // Warning lasers (translucent thinner lines)
                if (drawDiag) {
                    laserPaint.strokeWidth = 6f
                    laserPaint.color = adjustAlpha(beamColor, 0.6f)
                    canvas.drawLine(pDiagStart.x, pDiagStart.y, pDiagEnd.x, pDiagEnd.y, laserPaint)
                }
                if (drawLane) {
                    laserPaint.strokeWidth = 8f
                    laserPaint.color = adjustAlpha(beamColor, 0.4f)
                    canvas.drawLine(pStart.x, pStart.y, pEnd.x, pEnd.y, laserPaint)
                    
                    laserPaint.strokeWidth = 3f
                    laserPaint.color = adjustAlpha(beamColor, 0.8f)
                    canvas.drawLine(pStart.x, pStart.y, pEnd.x, pEnd.y, laserPaint)
                }
            }
        }
    }

    private enum class Biome {
        GREENERY,
        DESERT,
        SNOWY
    }

    private fun getBiome(z: Float): Biome {
        return when {
            z < 250f -> Biome.GREENERY
            z < 500f -> Biome.DESERT
            else -> Biome.SNOWY
        }
    }

    private fun adjustAlpha(color: Int, factor: Float): Int {
        val alpha = (Color.alpha(color) * factor).roundToInt().coerceIn(0, 255)
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
    }
}
