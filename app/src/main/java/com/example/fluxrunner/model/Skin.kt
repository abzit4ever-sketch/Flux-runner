package com.example.fluxrunner.model

import android.graphics.Color

enum class Skin(
    val id: String,
    val displayName: String,
    val cost: Int,
    val primaryColor: Int,
    val secondaryColor: Int,
    val description: String
) {
    DEFAULT("default", "Default Orb", 0, Color.parseColor("#FFFFFF"), Color.parseColor("#00E5FF"), "Standard energy core."),
    LAVA("lava", "Lava Core", 100, Color.parseColor("#FF3D00"), Color.parseColor("#FF9100"), "Molten volcanic essence."),
    FROST("frost", "Frost Core", 250, Color.parseColor("#D4FC79"), Color.parseColor("#96E6A1"), "Sub-zero crystalline core."),
    THUNDER("thunder", "Thunder Core", 500, Color.parseColor("#FFFF00"), Color.parseColor("#00E5FF"), "Overcharged electrical node."),
    BIO("bio", "Bio Core", 750, Color.parseColor("#A8FF78"), Color.parseColor("#78FFD6"), "Radioactive chemical core."),
    KITTY_POP("kitty_pop", "Kitty Pop", 900, Color.parseColor("#FFE4F3"), Color.parseColor("#FF5FB7"), "Cute bow-core with soft pink trails."),
    DRAGON_FIRE("dragon_fire", "Dragon Fire", 1200, Color.parseColor("#FF2A1F"), Color.parseColor("#FFB000"), "Horned drake core with wing sparks."),
    DINO_MINT("dino_mint", "Dino Mint", 1200, Color.parseColor("#76FF9F"), Color.parseColor("#1B6B4A"), "Tiny prehistoric core with dorsal spikes."),
    SHADOW_NINJA("shadow_ninja", "Shadow Ninja", 1350, Color.parseColor("#151927"), Color.parseColor("#8C5CFF"), "Masked stealth core with violet cuts."),
    ROYAL_PANDA("royal_panda", "Royal Panda", 1400, Color.parseColor("#F8F8F8"), Color.parseColor("#20242E"), "Black-and-white charm core with gold trim."),
    GALAXY("galaxy", "Galaxy Core", 1000, Color.parseColor("#8A2387"), Color.parseColor("#F27121"), "Swirling nebular plasma."),
    DARK_MATTER("dark_matter", "Dark Matter", 1500, Color.parseColor("#2C3E50"), Color.parseColor("#000000"), "Gravitational singularity."),
    COSMIC_CROWN("cosmic_crown", "Cosmic Crown", 15, Color.parseColor("#00E5FF"), Color.parseColor("#FFFF00"), "Legendary crown of pure flux energy. Requires 15 Shards."),
    CHALLENGER("challenger", "Challenger Core", 0, Color.parseColor("#00FFFF"), Color.parseColor("#FF00FF"), "Unlocked via Daily Impossible Challenge.");

    companion object {
        fun getById(id: String): Skin {
            return values().firstOrNull { it.id == id } ?: DEFAULT
        }
    }
}
