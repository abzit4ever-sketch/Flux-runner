package com.example.fluxrunner.model

enum class PowerUpType(
    val label: String,  // short vector-friendly text label, no emojis
    val colorCode: String
) {
    SHIELD("SHIELD", "#35F3FF"),          // Neon Cyan
    MAGNET("MAGNET", "#FFD700"),          // Gold
    INVISIBILITY("PHASE", "#B388FF")      // Neon Purple
}

data class PowerUp(
    var id: Int = 0,
    var type: PowerUpType = PowerUpType.SHIELD,
    val position: Vector3D = Vector3D(),
    var lane: Int = 0, // -1: Left, 0: Center, 1: Right
    var active: Boolean = false,
    var rotationAngle: Float = 0f,
    var radius: Float = 0.6f
) {
    fun update(deltaTime: Float) {
        if (!active) return
        // Spin powerups for visual polish
        rotationAngle = (rotationAngle + 120f * deltaTime) % 360f
    }
}
