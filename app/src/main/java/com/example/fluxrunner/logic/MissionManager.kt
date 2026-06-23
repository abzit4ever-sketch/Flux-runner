package com.example.fluxrunner.logic

import com.example.fluxrunner.model.Mission
import com.example.fluxrunner.model.MissionType
import java.text.SimpleDateFormat
import java.util.*

class MissionManager(private val saveManager: SaveManager) {
    val activeMissions = mutableListOf<Mission>()

    fun initMissions() {
        val todayStr = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        val savedDate = saveManager.getDailyMissionsDate()

        if (todayStr != savedDate) {
            // New day! Create fresh daily missions
            generateMissions()
            saveManager.saveDailyMissionsDate(todayStr)
            saveMissionsState()
        } else {
            // Same day, load progress
            loadMissionsState()
        }
    }

    private fun generateMissions() {
        activeMissions.clear()
        activeMissions.add(Mission("dist_2k", MissionType.DISTANCE, "Travel 2000 meters", 2000, coinReward = 150, tokenReward = 15))
        activeMissions.add(Mission("coins_100", MissionType.COINS, "Collect 100 coins", 100, coinReward = 100, tokenReward = 10))
        activeMissions.add(Mission("combo_30", MissionType.COMBO, "Reach a 30x combo", 30, coinReward = 200, tokenReward = 20))
    }

    fun progressMission(type: MissionType, amount: Int): Boolean {
        var anyCompleted = false
        for (mission in activeMissions) {
            if (mission.type == type && !mission.completed) {
                if (mission.progress(amount)) {
                    anyCompleted = true
                }
                saveManager.saveMissionProgress(mission.id, mission.currentValue)
            }
        }
        return anyCompleted
    }

    fun claimReward(mission: Mission): Boolean {
        if (mission.completed && !mission.claimed) {
            mission.claimed = true
            saveManager.setMissionClaimed(mission.id, true)
            saveManager.addCoins(mission.coinReward)
            saveManager.addTokens(mission.tokenReward)
            return true
        }
        return false
    }

    private fun loadMissionsState() {
        generateMissions() // sets up structure
        for (mission in activeMissions) {
            mission.currentValue = saveManager.getMissionProgress(mission.id)
            mission.completed = mission.currentValue >= mission.targetValue
            mission.claimed = saveManager.isMissionClaimed(mission.id)
        }
    }

    private fun saveMissionsState() {
        for (mission in activeMissions) {
            saveManager.saveMissionProgress(mission.id, mission.currentValue)
            saveManager.setMissionClaimed(mission.id, mission.claimed)
        }
    }
}
