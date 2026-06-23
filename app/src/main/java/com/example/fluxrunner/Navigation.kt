package com.example.fluxrunner

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.fluxrunner.engine.ParticleSystem
import com.example.fluxrunner.logic.GameManager
import com.example.fluxrunner.logic.MissionManager
import com.example.fluxrunner.logic.PlayerController
import com.example.fluxrunner.logic.SaveManager
import com.example.fluxrunner.logic.TrackGenerator
import com.example.fluxrunner.media.AudioManager
import com.example.fluxrunner.ui.earn.EarnCashScreen
import com.example.fluxrunner.ui.earn.WithdrawCashScreen
import com.example.fluxrunner.ui.game.GamePlayScreen
import com.example.fluxrunner.ui.main.MainMenuScreen
import com.example.fluxrunner.ui.missions.MissionsScreen
import com.example.fluxrunner.ui.pvp.PvPArenaScreen
import com.example.fluxrunner.ui.settings.SettingsScreen
import com.example.fluxrunner.ui.shop.ShopScreen

@Composable
fun MainNavigation() {
  val context = LocalContext.current.applicationContext
  
  // Instantiate all long-lived gameplay managers
  val saveManager = remember { SaveManager(context) }
  val player = remember { PlayerController() }
  val trackGenerator = remember { TrackGenerator() }
  val audioManager = remember { AudioManager(saveManager) }
  val missionManager = remember { MissionManager(saveManager) }
  val particleSystem = remember { ParticleSystem() }
  
  val gameManager = remember {
    GameManager(
      player = player,
      trackGenerator = trackGenerator,
      audioManager = audioManager,
      saveManager = saveManager,
      missionManager = missionManager,
      particleSystem = particleSystem
    ).apply { init() }
  }

  val backStack = rememberNavBackStack(Main)

  NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    entryProvider =
      entryProvider {
        entry<Main> {
          MainMenuScreen(
            saveManager = saveManager,
            onPlayClick = { backStack.add(GamePlay) },
            onShopClick = { backStack.add(Shop) },
            onSettingsClick = { backStack.add(Settings) },
            onMissionsClick = { backStack.add(Missions) },
            onEarnCashClick = { backStack.add(EarnCash) },
            onWithdrawCashClick = { backStack.add(WithdrawCash) },
            onArenaClick = { backStack.add(PvPArena) },
            modifier = Modifier.fillMaxSize()
          )
        }
        entry<GamePlay> {
          GamePlayScreen(
            gameManager = gameManager,
            onBackClick = { backStack.removeLastOrNull() },
            modifier = Modifier.fillMaxSize()
          )
        }
        entry<Shop> {
          ShopScreen(
            saveManager = saveManager,
            gameManager = gameManager,
            onBackClick = { backStack.removeLastOrNull() },
            modifier = Modifier.fillMaxSize()
          )
        }
        entry<Missions> {
          MissionsScreen(
            missionManager = missionManager,
            onBackClick = { backStack.removeLastOrNull() },
            modifier = Modifier.fillMaxSize()
          )
        }
        entry<Settings> {
          SettingsScreen(
            saveManager = saveManager,
            audioManager = audioManager,
            onBackClick = { backStack.removeLastOrNull() },
            modifier = Modifier.fillMaxSize()
          )
        }
        entry<EarnCash> {
          EarnCashScreen(
            saveManager = saveManager,
            onBackClick = { backStack.removeLastOrNull() },
            modifier = Modifier.fillMaxSize()
          )
        }
        entry<WithdrawCash> {
          WithdrawCashScreen(
            saveManager = saveManager,
            onBackClick = { backStack.removeLastOrNull() },
            modifier = Modifier.fillMaxSize()
          )
        }
        entry<PvPArena> {
          PvPArenaScreen(
            saveManager = saveManager,
            onBackClick = { backStack.removeLastOrNull() },
            onStartMatch = { matchId, playerId, seed ->
              backStack.add(PvPGame(matchId, playerId, seed))
            },
            modifier = Modifier.fillMaxSize()
          )
        }
        entry<PvPGame> { key ->
          GamePlayScreen(
            gameManager = gameManager,
            onBackClick = { backStack.removeLastOrNull() },
            pvpMatchId = key.matchId,
            pvpPlayerId = key.playerId,
            pvpSeed = key.seed,
            modifier = Modifier.fillMaxSize()
          )
        }
      },
  )
}
