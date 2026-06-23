package com.example.fluxrunner.engine

import android.content.Context
import android.graphics.Canvas
import android.graphics.SurfaceTexture
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.TextureView
import com.example.fluxrunner.logic.GameManager
import com.example.fluxrunner.logic.GameState
import kotlin.math.abs

class GameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : TextureView(context, attrs, defStyleAttr), TextureView.SurfaceTextureListener, Runnable {

    lateinit var gameManager: GameManager
    val renderer = Renderer3D()
    
    private var gameThread: Thread? = null
    @Volatile private var isRunning = false
    
    private val gestureDetector: GestureDetector

    init {
        surfaceTextureListener = this
        isOpaque = true // Hardware optimization: disable transparency blending
        
        // Gesture detector to handle single tap (element cycle) and swiping (lane change)
        val gestureListener = object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapUp(e: MotionEvent): Boolean {
                if (gameManager.gameState == GameState.PLAYING) {
                    if (gameManager.player.cycleElement()) {
                        gameManager.audioManager.playSwitch()
                        performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    }
                    return true
                }
                return false
            }

            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (e1 == null || gameManager.gameState != GameState.PLAYING) return false
                
                val diffX = e2.x - e1.x
                val diffY = e2.y - e1.y
                val swipeThreshold = 80
                val swipeVelocityThreshold = 80

                if (abs(diffX) > abs(diffY)) {
                    if (abs(diffX) > swipeThreshold && abs(velocityX) > swipeVelocityThreshold) {
                        if (diffX > 0) {
                            gameManager.player.moveRight()
                        } else {
                            gameManager.player.moveLeft()
                        }
                        performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        return true
                    }
                }
                return false
            }
        }
        
        gestureDetector = GestureDetector(context, gestureListener)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)
        return true
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        renderer.setScreenSize(width, height)
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
        renderer.setScreenSize(width, height)
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        stopLoop()
        return true
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
        // Do nothing
    }

    fun startLoop() {
        if (isRunning) return
        isRunning = true
        gameThread = Thread(this).apply { start() }
    }

    fun stopLoop() {
        if (!isRunning) return
        isRunning = false
        try {
            gameThread?.join(500)
        } catch (e: InterruptedException) {
            // ignore
        }
        gameThread = null
    }

    override fun run() {
        var lastTime = System.nanoTime()
        val targetFrameTimeNs = 16_666_666L // 60 FPS target
        
        while (isRunning) {
            val startTime = System.nanoTime()
            val now = System.nanoTime()
            var deltaTime = (now - lastTime) / 1_000_000_000f
            lastTime = now

            // Cap delta time to prevent physics anomalies on sudden lag spikes
            if (deltaTime > 0.1f) deltaTime = 0.1f

            val canvas: Canvas? = lockCanvas()
            
            if (canvas != null) {
                try {
                    // 1. Update Game Logic
                    gameManager.update(deltaTime)
                    
                    // 2. Smoothly track camera behind player
                    renderer.updateCamera(
                        gameManager.player.worldX,
                        gameManager.player.worldZ,
                        false
                    )

                    // 3. Draw entire 3D world onto Canvas
                    val themeColor = gameManager.player.activeElement.primaryColor
                    renderer.draw(
                        canvas,
                        gameManager.player.worldX,
                        gameManager.player.worldZ,
                        gameManager.player.activeElement,
                        gameManager.player.activeSkin,
                        gameManager.trackGenerator.obstaclePool,
                        gameManager.trackGenerator.coinPool,
                        gameManager.trackGenerator.powerUpPool,
                        gameManager.particleSystem.getActiveParticles(),
                        themeColor,
                        deltaTime,
                        gameManager.shieldTimer,
                        gameManager.magnetTimer,
                        gameManager.invisibilityTimer,
                        gameManager.isOverchargeActive,
                        gameManager.orbEvolutionLevel,
                        gameManager.isFluxSurgeActive
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    unlockCanvasAndPost(canvas)
                }
            } else {
                // Keep physics simulation running even if the canvas is temporarily unavailable
                try {
                    gameManager.update(deltaTime)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // High precision frame-rate limiter
            val elapsedTimeNs = System.nanoTime() - startTime
            val sleepTimeMs = (targetFrameTimeNs - elapsedTimeNs) / 1_000_000L
            if (sleepTimeMs > 0) {
                try {
                    Thread.sleep(sleepTimeMs)
                } catch (e: InterruptedException) {
                    // ignore
                }
            }
        }
    }
}
