package com.example.fluxrunner.engine

import android.graphics.Color
import com.example.fluxrunner.model.Element
import com.example.fluxrunner.model.Vector3D
import kotlin.random.Random

class Particle {
    val position = Vector3D()
    val velocity = Vector3D()
    var color: Int = Color.WHITE
    var alpha: Float = 1.0f
    var life: Float = 0.0f
    var maxLife: Float = 1.0f
    var size: Float = 0.5f
    var active: Boolean = false

    fun update(deltaTime: Float) {
        if (!active) return
        life += deltaTime
        if (life >= maxLife) {
            active = false
            return
        }
        // Move particle
        position.x += velocity.x * deltaTime
        position.y += velocity.y * deltaTime
        position.z += velocity.z * deltaTime
        
        // Fade out
        alpha = 1.0f - (life / maxLife)
    }
}

class ParticleSystem(maxParticles: Int = 260) {
    private val particles = Array(maxParticles) { Particle() }
    private val particleList: List<Particle> = particles.asList()
    private val random = Random(System.currentTimeMillis())

    fun update(deltaTime: Float) {
        for (p in particles) {
            if (p.active) {
                p.update(deltaTime)
            }
        }
    }

    fun getActiveParticles(): List<Particle> {
        return particleList
    }

    fun spawnTrailParticle(pos: Vector3D, element: Element) {
        val p = findFreeParticle() ?: return
        p.active = true
        // Spread slightly around player position
        p.position.set(
            pos.x + (random.nextFloat() - 0.5f) * 0.4f,
            pos.y + (random.nextFloat() - 0.5f) * 0.4f,
            pos.z - 0.2f // trail is slightly behind
        )
        
        // Velocity: moves backwards relative to the player, with some spread
        p.velocity.set(
            (random.nextFloat() - 0.5f) * 1.5f,
            (random.nextFloat() - 0.5f) * 1.5f,
            -random.nextFloat() * 2.0f
        )
        
        p.color = element.particleColor
        p.life = 0.0f
        p.maxLife = 0.4f + random.nextFloat() * 0.4f
        p.size = 0.15f + random.nextFloat() * 0.2f
    }

    fun spawnCollisionExplosion(pos: Vector3D, color: Int) {
        // Spawn a burst of particles
        for (i in 0 until 16) {
            val p = findFreeParticle() ?: break
            p.active = true
            p.position.set(pos.x, pos.y, pos.z)
            
            // Explode outwards in all directions
            val angle = random.nextFloat() * 2f * Math.PI.toFloat()
            val speed = 2.0f + random.nextFloat() * 5.0f
            p.velocity.set(
                Math.cos(angle.toDouble()).toFloat() * speed,
                (random.nextFloat() - 0.2f) * speed * 0.5f,
                Math.sin(angle.toDouble()).toFloat() * speed
            )
            
            p.color = color
            p.life = 0.0f
            p.maxLife = 0.35f + random.nextFloat() * 0.35f
            p.size = 0.14f + random.nextFloat() * 0.18f
        }
    }

    fun spawnBurst(pos: Vector3D, element: Element, count: Int = 15) {
        for (i in 0 until count) {
            val p = findFreeParticle() ?: break
            p.active = true
            p.position.set(
                pos.x + (random.nextFloat() - 0.5f) * 0.8f,
                pos.y + (random.nextFloat() - 0.5f) * 0.8f,
                pos.z
            )
            
            p.velocity.set(
                (random.nextFloat() - 0.5f) * 4.0f,
                (random.nextFloat() - 0.5f) * 4.0f,
                (random.nextFloat() - 0.5f) * 2.0f
            )
            
            p.color = element.particleColor
            p.life = 0.0f
            p.maxLife = 0.3f + random.nextFloat() * 0.4f
            p.size = 0.15f + random.nextFloat() * 0.2f
        }
    }

    private fun findFreeParticle(): Particle? {
        for (p in particles) {
            if (!p.active) return p
        }
        return null
    }
}
