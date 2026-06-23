package com.example.fluxrunner.model

data class Coin(
    var id: Int = 0,
    val position: Vector3D = Vector3D(),
    var lane: Int = 0,
    var radius: Float = 0.5f,
    var active: Boolean = false,
    var rotationAngle: Float = 0f,
    var isShard: Boolean = false
) {
    fun update(deltaTime: Float) {
        if (!active) return
        // Spin coins for visual polish
        rotationAngle = (rotationAngle + 180f * deltaTime) % 360f
    }
}
