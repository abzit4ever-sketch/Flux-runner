package com.example.fluxrunner.model

import kotlin.math.sqrt

data class Vector3D(
    var x: Float = 0f,
    var y: Float = 0f,
    var z: Float = 0f
) {
    fun set(x: Float, y: Float, z: Float) {
        this.x = x
        this.y = y
        this.z = z
    }

    fun add(v: Vector3D): Vector3D {
        return Vector3D(x + v.x, y + v.y, z + v.z)
    }

    fun subtract(v: Vector3D): Vector3D {
        return Vector3D(x - v.x, y - v.y, z - v.z)
    }

    fun multiply(s: Float): Vector3D {
        return Vector3D(x * s, y * s, z * s)
    }

    fun length(): Float {
        return sqrt(x * x + y * y + z * z)
    }

    fun lerp(target: Vector3D, alpha: Float): Vector3D {
        return Vector3D(
            x + (target.x - x) * alpha,
            y + (target.y - y) * alpha,
            z + (target.z - z) * alpha
        )
    }
}

fun getTrackY(z: Float): Float {
    if (z < 60f) return 0f
    val dz = z - 60f
    val fade = (dz / 30f).coerceIn(0f, 1f)
    val wave = kotlin.math.sin(dz * 0.02f) * 4.5f + kotlin.math.cos(dz * 0.05f) * 1.5f - 1.5f
    return wave * fade
}

