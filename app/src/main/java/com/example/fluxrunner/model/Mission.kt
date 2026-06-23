package com.example.fluxrunner.model

enum class MissionType {
    DISTANCE,
    COINS,
    COMBO,
    BOSS_BEATEN
}

data class Mission(
    val id: String,
    val type: MissionType,
    val description: String,
    val targetValue: Int,
    var currentValue: Int = 0,
    val coinReward: Int = 100,
    val tokenReward: Int = 10,
    var completed: Boolean = false,
    var claimed: Boolean = false
) {
    fun progress(amount: Int): Boolean {
        if (completed) return false
        currentValue += amount
        if (currentValue >= targetValue) {
            currentValue = targetValue
            completed = true
            return true
        }
        return false
    }
}
