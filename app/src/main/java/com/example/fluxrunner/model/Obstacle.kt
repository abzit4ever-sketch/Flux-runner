package com.example.fluxrunner.model

enum class ObstacleType {
    ELEMENT_GATE,
    ROTATING_GATE,
    MULTI_TUNNEL,
    DYNAMIC_GATE,
    RIFT_ORB,
    BOOST_PAD,
    CANNON,
    FLOOR_HAZARD,
    HOLE_WALL
}

data class Obstacle(
    var id: Int = 0,
    var type: ObstacleType = ObstacleType.ELEMENT_GATE,
    val position: Vector3D = Vector3D(),
    var lane: Int = 0, // -1: Left, 0: Center, 1: Right
    var requiredElement: Element = Element.FIRE,
    var width: Float = 2.0f,
    var height: Float = 3.0f,
    var depth: Float = 0.5f,
    
    // Rotating Gate fields
    var rotationAngle: Float = 0f,
    var rotationSpeed: Float = 0f, // degrees per second
    
    // Rift Orb fields
    var slideSpeed: Float = 2.0f, // units/sec
    var slideDir: Float = 1.0f,
    var slideRangeX: Float = 3.0f, // moves between -3 and +3
    
    // Dynamic Gate fields
    var cycleTimer: Float = 0f,
    
    // Active flag for object pooling
    var active: Boolean = false,
    
    // Golden gate modifier for rare high-reward gates
    var isGolden: Boolean = false
) {
    fun update(deltaTime: Float, dynamicGateDuration: Float = 1.0f) {
        if (!active) return

        when (type) {
            ObstacleType.ROTATING_GATE -> {
                rotationAngle = (rotationAngle + rotationSpeed * deltaTime) % 360f
            }
            ObstacleType.RIFT_ORB -> {
                position.x += slideSpeed * slideDir * deltaTime
                if (position.x >= slideRangeX) {
                    position.x = slideRangeX
                    slideDir = -1.0f
                } else if (position.x <= -slideRangeX) {
                    position.x = -slideRangeX
                    slideDir = 1.0f
                }
            }
            ObstacleType.DYNAMIC_GATE -> {
                cycleTimer += deltaTime
                if (cycleTimer >= dynamicGateDuration) {
                    cycleTimer -= dynamicGateDuration
                    requiredElement = requiredElement.next()
                }
            }
            else -> {}
        }
    }
}
