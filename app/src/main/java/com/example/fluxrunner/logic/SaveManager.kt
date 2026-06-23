package com.example.fluxrunner.logic

import android.content.Context
import android.content.SharedPreferences

class SaveManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("flux_runner_prefs", Context.MODE_PRIVATE)

    fun getHighScore(): Int = prefs.getInt("high_score", 0)
    fun saveHighScore(score: Int) {
        if (score > getHighScore()) {
            prefs.edit().putInt("high_score", score).apply()
        }
    }

    fun getLongestDistance(): Float = prefs.getFloat("longest_distance", 0f)
    fun saveLongestDistance(distance: Float) {
        if (distance > getLongestDistance()) {
            prefs.edit().putFloat("longest_distance", distance).apply()
        }
    }

    fun getHighestCombo(): Int = prefs.getInt("highest_combo", 0)
    fun saveHighestCombo(combo: Int) {
        if (combo > getHighestCombo()) {
            prefs.edit().putInt("highest_combo", combo).apply()
        }
    }

    fun getCoins(): Int = prefs.getInt("coins", 0)
    fun addCoins(amount: Int) {
        prefs.edit().putInt("coins", getCoins() + amount).apply()
    }
    fun spendCoins(amount: Int): Boolean {
        val current = getCoins()
        if (current >= amount) {
            prefs.edit().putInt("coins", current - amount).apply()
            return true
        }
        return false
    }

    fun getActiveSkin(): String = prefs.getString("active_skin", "default") ?: "default"
    fun setActiveSkin(skinId: String) {
        prefs.edit().putString("active_skin", skinId).apply()
    }

    fun isSkinUnlocked(skinId: String): Boolean {
        if (skinId == "default") return true
        val unlocked = prefs.getStringSet("unlocked_skins", setOf("default")) ?: setOf("default")
        return unlocked.contains(skinId)
    }

    fun unlockSkin(skinId: String) {
        val unlocked = prefs.getStringSet("unlocked_skins", setOf("default"))?.toMutableSet() ?: mutableSetOf("default")
        unlocked.add(skinId)
        prefs.edit().putStringSet("unlocked_skins", unlocked).apply()
    }

    // Settings
    fun isSoundEnabled(): Boolean = prefs.getBoolean("sound_enabled", true)
    fun setSoundEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("sound_enabled", enabled).apply()
    }

    // Daily missions
    fun getMissionProgress(missionId: String): Int = prefs.getInt("mission_progress_$missionId", 0)
    fun saveMissionProgress(missionId: String, value: Int) {
        prefs.edit().putInt("mission_progress_$missionId", value).apply()
    }
    
    fun isMissionClaimed(missionId: String): Boolean = prefs.getBoolean("mission_claimed_$missionId", false)
    fun setMissionClaimed(missionId: String, claimed: Boolean) {
        prefs.edit().putBoolean("mission_claimed_$missionId", claimed).apply()
    }

    fun getDailyMissionsDate(): String = prefs.getString("daily_missions_date", "") ?: ""
    fun saveDailyMissionsDate(dateStr: String) {
        prefs.edit().putString("daily_missions_date", dateStr).apply()
    }

    // Shards balance storage
    fun getShards(): Int = prefs.getInt("shards", 0)
    fun addShards(amount: Int) {
        prefs.edit().putInt("shards", getShards() + amount).apply()
    }
    fun spendShards(amount: Int): Boolean {
        val current = getShards()
        if (current >= amount) {
            prefs.edit().putInt("shards", current - amount).apply()
            return true
        }
        return false
    }

    // Tech Upgrades levels (0..3)
    fun getUpgradeLevel(upgradeId: String): Int = prefs.getInt("upgrade_$upgradeId", 0)
    fun incrementUpgradeLevel(upgradeId: String) {
        prefs.edit().putInt("upgrade_$upgradeId", getUpgradeLevel(upgradeId) + 1).apply()
    }

    // Daily challenge completion status
    fun isDailyChallengeCompletedToday(dateStr: String): Boolean = prefs.getBoolean("daily_completed_$dateStr", false)
    fun setDailyChallengeCompletedToday(dateStr: String) {
        prefs.edit().putBoolean("daily_completed_$dateStr", true).apply()
    }

    // Ghost run Z-history storage (CSV string)
    fun getGhostRunHistory(): String = prefs.getString("ghost_run_history", "") ?: ""
    fun saveGhostRunHistory(historyCsv: String) {
        prefs.edit().putString("ghost_run_history", historyCsv).apply()
    }

    // Tokens currency
    fun getTokens(): Int = prefs.getInt("tokens", 0)
    fun addTokens(amount: Int) {
        prefs.edit().putInt("tokens", getTokens() + amount).apply()
    }
    fun spendTokens(amount: Int): Boolean {
        val current = getTokens()
        if (current >= amount) {
            prefs.edit().putInt("tokens", current - amount).apply()
            return true
        }
        return false
    }

    // Real cash balance (simulated)
    fun getRealCash(): Float = prefs.getFloat("real_cash", 0f)
    fun addRealCash(amount: Float) {
        prefs.edit().putFloat("real_cash", getRealCash() + amount).apply()
    }
    fun spendRealCash(amount: Float): Boolean {
        val current = getRealCash()
        if (current >= amount) {
            prefs.edit().putFloat("real_cash", current - amount).apply()
            return true
        }
        return false
    }
}
