package com.example.fluxrunner.logic

import com.example.fluxrunner.model.Element
import com.example.fluxrunner.model.Skin

class PlayerController {
    // Positioning
    var lane: Int = 0 // -1: Left, 0: Center, 1: Right
    var worldX: Float = 0f // smooth X position
    var worldZ: Float = 0f // distance traveled (Y is always close to 0)

    // Elements & customisation
    var activeElement: Element = Element.FIRE
    var activeSkin: Skin = Skin.DEFAULT

    // Movement speeds
    var forwardSpeed: Float = 9.0f
    var targetSpeed: Float = 9.0f
    val maxSpeed: Float = 32.0f

    // Switch mechanics
    var switchCooldown: Float = 0.0f
    val switchCooldownDuration: Float = 0.15f // 0.15s cooldown
    var pulseTimer: Float = 0.0f
    val pulseDuration: Float = 0.1f // 0.1s pulse animation
    var lastElementSwitchAge: Float = Float.MAX_VALUE

    // Special modes
    var isInvincible: Boolean = false // overcharge invincibility (or one hit shield)

    fun reset() {
        lane = 0
        worldX = 0f
        worldZ = 0f
        activeElement = Element.FIRE
        forwardSpeed = 9.0f
        targetSpeed = 9.0f
        switchCooldown = 0.0f
        pulseTimer = 0.0f
        lastElementSwitchAge = Float.MAX_VALUE
        isInvincible = false
    }

    fun update(deltaTime: Float) {
        // 1. Automatic forward movement
        worldZ += forwardSpeed * deltaTime

        // 2. Smooth lane interpolation
        val targetX = lane * 3.0f
        worldX += (targetX - worldX) * 12.0f * deltaTime

        // 3. Update timers
        if (switchCooldown > 0f) {
            switchCooldown -= deltaTime
        }
        if (pulseTimer > 0f) {
            pulseTimer -= deltaTime
        }
        if (lastElementSwitchAge < Float.MAX_VALUE) {
            lastElementSwitchAge += deltaTime
        }

        // 4. Smoothly interpolate speed towards target speed
        forwardSpeed += (targetSpeed - forwardSpeed) * 2.0f * deltaTime
    }

    // Cycles to the next element if not on cooldown
    // Returns true if switch was successful
    fun cycleElement(): Boolean {
        if (switchCooldown > 0f) return false
        
        activeElement = activeElement.next()
        switchCooldown = switchCooldownDuration
        pulseTimer = pulseDuration
        lastElementSwitchAge = 0f
        return true
    }

    fun moveLeft(): Boolean {
        if (lane > -1) {
            lane--
            return true
        }
        return false
    }

    fun moveRight(): Boolean {
        if (lane < 1) {
            lane++
            return true
        }
        return false
    }
}
