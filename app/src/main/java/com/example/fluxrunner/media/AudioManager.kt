package com.example.fluxrunner.media

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.AudioManager as AndroidAudioManager
import com.example.fluxrunner.logic.SaveManager
import kotlin.math.sin

class AudioManager(private val saveManager: SaveManager) {
    private var musicThread: Thread? = null
    @Volatile private var isMusicPlaying = false
    private var tempoBpm = 110f
    private var intensityScale = 1.0f

    // Synthesis constants
    private val sampleRate = 22050

    fun startMusic() {
        if (!saveManager.isSoundEnabled()) return
        if (isMusicPlaying) return
        
        isMusicPlaying = true
        musicThread = Thread {
            runMusicSynthLoop()
        }.apply { start() }
    }

    fun stopMusic() {
        isMusicPlaying = false
        musicThread?.join(500)
        musicThread = null
    }

    fun setTempoAndIntensity(combo: Int) {
        // Increase tempo and scale complexity based on combo
        tempoBpm = when {
            combo >= 50 -> 140f
            combo >= 25 -> 125f
            combo >= 10 -> 115f
            else -> 110f
        }
        intensityScale = when {
            combo >= 50 -> 1.8f
            combo >= 25 -> 1.4f
            combo >= 10 -> 1.1f
            else -> 0.8f
        }
    }

    // Programmatic synthwave music loop running in background thread
    private fun runMusicSynthLoop() {
        val minBufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        
        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(minBufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        audioTrack.play()

        // Synthwave chord notes (frequencies in Hz)
        // Progression: Am -> F -> C -> G
        val chords = arrayOf(
            floatArrayOf(220.0f, 261.63f, 329.63f), // Am
            floatArrayOf(174.61f, 220.00f, 261.63f), // F
            floatArrayOf(261.63f, 329.63f, 392.00f), // C
            floatArrayOf(196.00f, 246.94f, 293.66f)  // G
        )

        val bassChords = floatArrayOf(110.00f, 87.31f, 130.81f, 98.00f) // Sub-bass frequencies

        var chordIndex = 0
        var phase = 0.0
        var phaseBass = 0.0

        // PCM write buffer
        val bufferSize = 1024
        val buffer = ShortArray(bufferSize)

        while (isMusicPlaying) {
            val currentBpm = tempoBpm
            // Length of an 8th note in seconds
            val noteDuration = 30f / currentBpm
            val totalNoteSamples = (sampleRate * noteDuration).toInt()
            
            val currentBassFreq = bassChords[chordIndex]
            val currentTriad = chords[chordIndex]

            var samplesWritten = 0
            while (samplesWritten < totalNoteSamples && isMusicPlaying) {
                val chunk = minOf(bufferSize, totalNoteSamples - samplesWritten)
                for (i in 0 until chunk) {
                    val time = (samplesWritten + i).toDouble() / sampleRate
                    
                    // 1. Synthesize Bass (Triangle-like wave via Sine approximation)
                    val bassVal = sin(phaseBass)
                    phaseBass += 2.0 * Math.PI * currentBassFreq / sampleRate
                    if (phaseBass > 2.0 * Math.PI) phaseBass -= 2.0 * Math.PI

                    // 2. Synthesize Arpeggios (pulse/square lead)
                    // Arpeggiate notes of the chord over 4 notes in a bar
                    val arpStep = ((time / noteDuration) * 2).toInt() % 4
                    val leadFreq = if (arpStep < 3) currentTriad[arpStep] else currentTriad[1] * 2.0f
                    
                    val leadVal = if (sin(phase) > 0) 0.15f else -0.15f // square wave
                    phase += 2.0 * Math.PI * leadFreq / sampleRate
                    if (phase > 2.0 * Math.PI) phase -= 2.0 * Math.PI

                    // Mix lead + bass
                    val mixedVal = (bassVal * 0.45f + leadVal * 0.2f * intensityScale)
                    
                    // Clamp and write
                    val pcmVal = (mixedVal * 32767.0).coerceIn(-32768.0, 32767.0).toInt().toShort()
                    buffer[i] = pcmVal
                }
                audioTrack.write(buffer, 0, chunk)
                samplesWritten += chunk
            }

            // Move to next chord in progression every 4 beats (8 note intervals)
            chordIndex = (chordIndex + 1) % chords.size
        }

        try {
            audioTrack.stop()
            audioTrack.release()
        } catch (e: Exception) {
            // ignore
        }
    }

    // Sound effect trigger methods. We generate sound waves in short bursts.
    fun playSwitch() {
        if (!saveManager.isSoundEnabled()) return
        Thread {
            playToneSweep(500f, 1000f, 0.08f, 0.2f)
        }.start()
    }

    fun playCoin() {
        if (!saveManager.isSoundEnabled()) return
        Thread {
            // Arpeggiated high double chime (gold collector style)
            playTone(987.77f, 0.06f, 0.15f) // B5
            playTone(1318.51f, 0.08f, 0.15f) // E6
        }.start()
    }

    fun playCombo() {
        if (!saveManager.isSoundEnabled()) return
        Thread {
            playTone(659.25f, 0.12f, 0.25f) // E5 high blip
        }.start()
    }

    fun playBossWarning() {
        if (!saveManager.isSoundEnabled()) return
        Thread {
            // Dark warning horn
            playToneSweep(150f, 90f, 0.4f, 0.5f)
            playToneSweep(150f, 90f, 0.4f, 0.5f)
        }.start()
    }

    fun playDeath() {
        if (!saveManager.isSoundEnabled()) return
        Thread {
            // Decelerating descending low explosion rumble
            playToneSweep(200f, 40f, 0.5f, 0.6f)
        }.start()
    }

    // Helper to generate a constant pitch tone
    private fun playTone(frequency: Float, durationSec: Float, volume: Float) {
        val numSamples = (sampleRate * durationSec).toInt()
        val buffer = ShortArray(numSamples)
        var phase = 0.0
        for (i in 0 until numSamples) {
            buffer[i] = (sin(phase) * 32767.0 * volume).toInt().toShort()
            phase += 2.0 * Math.PI * frequency / sampleRate
        }
        writeToTrackDirectly(buffer)
    }

    // Helper to generate a sweep tone (frequency sweep)
    private fun playToneSweep(startFreq: Float, endFreq: Float, durationSec: Float, volume: Float) {
        val numSamples = (sampleRate * durationSec).toInt()
        val buffer = ShortArray(numSamples)
        var phase = 0.0
        for (i in 0 until numSamples) {
            val t = i.toFloat() / numSamples
            val currentFreq = startFreq + (endFreq - startFreq) * t
            buffer[i] = (sin(phase) * 32767.0 * volume * (1f - t)).toInt().toShort() // fade out sweep
            phase += 2.0 * Math.PI * currentFreq / sampleRate
        }
        writeToTrackDirectly(buffer)
    }

    private fun writeToTrackDirectly(buffer: ShortArray) {
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(buffer.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        track.write(buffer, 0, buffer.size)
        track.play()
        
        // Wait for sound to play, then clean up
        Thread.sleep((buffer.size.toFloat() / sampleRate * 1000).toLong() + 100)
        try {
            track.stop()
            track.release()
        } catch (e: Exception) {
            // ignore
        }
    }
}
