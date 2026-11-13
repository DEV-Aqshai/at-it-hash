package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.repository.RoomRepository
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.GamePlayScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.OnlineRoomDialog
import com.example.ui.theme.AtItHashTheme
import com.example.ui.viewmodel.LocalGameViewModel
import com.example.ui.viewmodel.OnlineGameViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class Screen {
    object Home : Screen()
    data class LocalPlay(val isVsAi: Boolean) : Screen()
    object OnlinePlay : Screen()
    object Auth : Screen()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AtItHashTheme {
                AppRoot()
            }
        }
    }
}

@Composable
fun AppRoot() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    var currentUser by remember { mutableStateOf<FirebaseUser?>(Firebase.auth.currentUser) }
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    var showOnlineDialog by remember { mutableStateOf(false) }

    // Listen to Firebase Auth state
    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    val localViewModel: LocalGameViewModel = viewModel()
    val roomRepository = remember { RoomRepository(context) }
    val onlineViewModel: OnlineGameViewModel = viewModel {
        OnlineGameViewModel(roomRepository)
    }

    val localState by localViewModel.state.collectAsState()
    val onlineState by onlineViewModel.state.collectAsState()

    // If an online room begins playing, automatically transition to OnlinePlay screen
    if (onlineState.room?.status == "playing" && currentScreen !is Screen.OnlinePlay) {
        showOnlineDialog = false
        currentScreen = Screen.OnlinePlay
    }

    // Online Lobby Dialog
    if (showOnlineDialog) {
        OnlineRoomDialog(
            isLoading = onlineState.isLoading,
            errorMessage = onlineState.errorMessage,
            createdRoomCode = onlineState.createdRoomCode,
            isWaitingForGuest = onlineState.createdRoomCode != null && onlineState.room?.status == "waiting",
            onCreateRoom = { passcode ->
                onlineViewModel.createRoom(passcode)
            },
            onJoinRoom = { roomCode, passcode ->
                onlineViewModel.joinRoom(roomCode, passcode)
            },
            onCancelWaiting = {
                onlineViewModel.leaveRoom()
            },
            onDismiss = {
                onlineViewModel.clearError()
                showOnlineDialog = false
            }
        )
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        BoxModifier(modifier = Modifier.padding(innerPadding)) {
            when (val screen = currentScreen) {
                Screen.Home -> {
                    HomeScreen(
                        onPlaySoloVsBot = {
                            localViewModel.setGameMode(vsAi = true)
                            currentScreen = Screen.LocalPlay(isVsAi = true)
                        },
                        onPlayPassAndPlay = {
                            localViewModel.setGameMode(vsAi = false)
                            currentScreen = Screen.LocalPlay(isVsAi = false)
                        },
                        onOpenOnlineLobby = {
                            if (currentUser == null) {
                                currentScreen = Screen.Auth
                            } else {
                                showOnlineDialog = true
                            }
                        },
                        onSignInClicked = {
                            currentScreen = Screen.Auth
                        },
                        onSignOutClicked = {
                            coroutineScope.launch {
                                withContext(NonCancellable) {
                                    try {
                                        credentialManager.clearCredentialState(ClearCredentialStateRequest())
                                    } catch (_: Exception) {
                                    } finally {
                                        Firebase.auth.signOut()
                                        currentUser = null
                                    }
                                }
                            }
                        }
                    )
                }

                Screen.Auth -> {
                    BackHandler {
                        currentScreen = Screen.Home
                    }
                    AuthScreen(
                        onAuthSuccess = {
                            currentScreen = Screen.Home
                            showOnlineDialog = true
                        },
                        onPlayOfflineClicked = {
                            currentScreen = Screen.Home
                        }
                    )
                }

                is Screen.LocalPlay -> {
                    BackHandler {
                        currentScreen = Screen.Home
                    }
                    GamePlayScreen(
                        title = if (screen.isVsAi) "SOLO VS BOT" else "PASS & PLAY",
                        roomCode = null,
                        player1Name = localState.player1Name,
                        player2Name = localState.player2Name,
                        board = localState.board,
                        currentTurn = localState.turn,
                        phase = localState.phase,
                        atPiecesPlaced = localState.atPiecesPlaced.toLong(),
                        hashPiecesPlaced = localState.hashPiecesPlaced.toLong(),
                        winner = localState.winner,
                        winPattern = localState.winPattern,
                        selectedPieceIndex = localState.selectedPieceIndex,
                        validDestinations = localState.validDestinations,
                        repeatMoveCount = localState.repeatMoveCount,
                        isInteractive = localState.winner.isEmpty() && (!localState.isVsAi || localState.turn == "@"),
                        turnHint = if (screen.isVsAi && localState.turn == "#") "Thinking..." else null,
                        onSlotClicked = { slotIdx ->
                            localViewModel.onSlotClicked(slotIdx)
                        },
                        onResetOrRematch = {
                            localViewModel.resetGame()
                        },
                        onExitMatch = {
                            currentScreen = Screen.Home
                        }
                    )
                }

                Screen.OnlinePlay -> {
                    BackHandler {
                        onlineViewModel.leaveRoom()
                        currentScreen = Screen.Home
                    }
                    val room = onlineState.room
                    if (room != null) {
                        val isHost = room.hostUid == onlineViewModel.currentUserId
                        val mySym = onlineViewModel.mySymbol
                        val opponentName = if (isHost) (room.guestName ?: "Waiting...") else room.hostName

                        GamePlayScreen(
                            title = "ONLINE MATCH",
                            roomCode = room.roomId,
                            player1Name = if (isHost) "You (@)" else "${room.hostName} (@)",
                            player2Name = if (!isHost) "You (#)" else "$opponentName (#)",
                            board = room.board,
                            currentTurn = room.turn,
                            phase = room.phase,
                            atPiecesPlaced = room.atPiecesPlaced,
                            hashPiecesPlaced = room.hashPiecesPlaced,
                            winner = room.winner,
                            winPattern = room.winPattern,
                            selectedPieceIndex = onlineState.selectedPieceIndex,
                            validDestinations = onlineState.validDestinations,
                            repeatMoveCount = room.repeatMoveCount,
                            isInteractive = onlineViewModel.isMyTurn && room.status == "playing",
                            turnHint = if (onlineViewModel.isMyTurn) "YOUR MOVE" else "Waiting for opponent...",
                            rematchPending = room.rematchRequestedBy.contains(onlineViewModel.currentUserId),
                            onSlotClicked = { slotIdx ->
                                onlineViewModel.onSlotClicked(slotIdx)
                            },
                            onResetOrRematch = {
                                onlineViewModel.requestRematch()
                            },
                            onExitMatch = {
                                onlineViewModel.leaveRoom()
                                currentScreen = Screen.Home
                            }
                        )
                    } else {
                        currentScreen = Screen.Home
                    }
                }
            }
        }
    }
}

@Composable
private fun BoxModifier(
    modifier: Modifier,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.Box(modifier = modifier.fillMaxSize()) {
        content()
    }
}
