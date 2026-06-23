package com.example.fluxrunner.model

import android.graphics.Color

enum class Element(
    val displayName: String,
    val primaryColor: Int,
    val glowColor: Int,
    val particleColor: Int,
    val symbolTag: String   // used as a tag to drive custom vector drawing; NO emojis
) {
    FIRE("Ember",    Color.parseColor("#FF5E36"), Color.parseColor("#40FF5E36"), Color.parseColor("#FFFF7E5F"), "EMBER"),
    ICE("Frost",     Color.parseColor("#35F3FF"), Color.parseColor("#4035F3FF"), Color.parseColor("#FFD4FFEC"), "FROST"),
    ELECTRIC("Volt", Color.parseColor("#FFE45C"), Color.parseColor("#40FFE45C"), Color.parseColor("#FFFFFFA0"), "VOLT"),
    TOXIC("Bloom",   Color.parseColor("#00FF8A"), Color.parseColor("#4000FF8A"), Color.parseColor("#FFA8FFB2"), "BLOOM");

    fun next(): Element {
        val vals = values()
        return vals[(this.ordinal + 1) % vals.size]
    }
}
