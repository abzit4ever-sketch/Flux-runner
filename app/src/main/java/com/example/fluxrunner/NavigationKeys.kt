package com.example.fluxrunner

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object Main : NavKey
@Serializable data object GamePlay : NavKey
@Serializable data object Shop : NavKey
@Serializable data object Missions : NavKey
@Serializable data object Settings : NavKey
@Serializable data object EarnCash : NavKey
@Serializable data object WithdrawCash : NavKey
@Serializable data object PvPArena : NavKey
@Serializable data class PvPGame(val matchId: String, val playerId: String, val seed: Int) : NavKey
