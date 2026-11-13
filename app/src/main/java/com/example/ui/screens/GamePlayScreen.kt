package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.DrawCounterView
import com.example.ui.components.GameBoardView
import com.example.ui.components.HowToPlayDialog
import com.example.ui.components.NotebookScaffold
import com.example.ui.components.PieceTrackerView
import com.example.ui.sound.SoundEffects
import com.example.ui.theme.ChalkboardGreen
import com.example.ui.theme.ChalkWhite
import com.example.ui.theme.GraphitePencil
import com.example.ui.theme.GraphitePencilMuted
import com.example.ui.theme.HighlighterYellow
import com.example.ui.theme.PatrickHandFamily
import com.example.ui.theme.PermanentMarkerFamily
import com.example.ui.theme.PlayerAtBlue
import com.example.ui.theme.PlayerHashRed

@Composable
fun GamePlayScreen(
    title: String,
    roomCode: String?,
    player1Name: String,
    player2Name: String,
    board: List<String>,
    currentTurn: String,
    phase: String,
    atPiecesPlaced: Long,
    hashPiecesPlaced: Long,
    winner: String,
    winPattern: List<Long>,
    selectedPieceIndex: Int?,
    validDestinations: List<Int>,
    repeatMoveCount: Long,
    isInteractive: Boolean,
    turnHint: String?,
    rematchPending: Boolean = false,
    onSlotClicked: (Int) -> Unit,
    onResetOrRematch: () -> Unit,
    onExitMatch: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showHowToPlay by remember { mutableStateOf(false) }

    if (showHowToPlay) {
        HowToPlayDialog(onDismiss = { showHowToPlay = false })
    }

    NotebookScaffold(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onExitMatch,
                    modifier = Modifier.testTag("exit_match_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = GraphitePencil
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = title,
                        fontFamily = PermanentMarkerFamily,
                        fontSize = 20.sp,
                        color = PlayerAtBlue
                    )
                    if (roomCode != null) {
                        Text(
                            text = "ROOM: $roomCode",
                            fontFamily = PermanentMarkerFamily,
                            fontSize = 12.sp,
                            color = GraphitePencilMuted
                        )
                    }
                }

                IconButton(
                    onClick = {
                        SoundEffects.playPencilScribble()
                        showHowToPlay = true
                    },
                    modifier = Modifier.testTag("match_rules_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.HelpOutline,
                        contentDescription = "Rules",
                        tint = GraphitePencil
                    )
                }
            }

            // Piece and Player chips
            PieceTrackerView(
                player1Name = player1Name,
                player2Name = player2Name,
                currentTurn = currentTurn,
                phase = phase,
                atPiecesPlaced = atPiecesPlaced,
                hashPiecesPlaced = hashPiecesPlaced
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Turn / Status banner
            StatusBanner(
                winner = winner,
                currentTurn = currentTurn,
                phase = phase,
                turnHint = turnHint
            )

            Spacer(modifier = Modifier.height(8.dp))

            // The Interactive Game Board
            GameBoardView(
                board = board,
                currentTurn = currentTurn,
                phase = phase,
                selectedPieceIndex = selectedPieceIndex,
                validDestinations = validDestinations,
                winPattern = winPattern,
                isInteractive = isInteractive,
                onSlotClicked = onSlotClicked,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Draw Counter Component (tracking 5-repeat moves rule)
            DrawCounterView(
                repeatMoveCount = repeatMoveCount
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Game finished overlay / rematch controls
            AnimatedVisibility(
                visible = winner.isNotEmpty(),
                enter = fadeIn() + slideInVertically()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val winnerTitle = when (winner) {
                        "draw" -> "🤝 STALEMATE DRAW!"
                        "@" -> "🎉 @ WINS THE MATCH!"
                        "#" -> "🎉 # WINS THE MATCH!"
                        else -> "MATCH OVER"
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (winner == "draw") Color(0xFFECEFF1) else HighlighterYellow)
                            .border(2.dp, GraphitePencil, RoundedCornerShape(12.dp))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = winnerTitle,
                                fontFamily = PermanentMarkerFamily,
                                fontSize = 24.sp,
                                color = GraphitePencil,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.testTag("winner_banner_text")
                            )

                            if (rematchPending) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Waiting for other player to accept rematch...",
                                    fontFamily = PatrickHandFamily,
                                    fontSize = 14.sp,
                                    color = GraphitePencilMuted
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = onResetOrRematch,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PlayerAtBlue
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .testTag("rematch_button")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (rematchPending) "REMATCH REQUESTED" else "PLAY AGAIN (REMATCH)",
                                        fontFamily = PermanentMarkerFamily,
                                        fontSize = 16.sp,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBanner(
    winner: String,
    currentTurn: String,
    phase: String,
    turnHint: String?
) {
    if (winner.isNotEmpty()) return

    val isAtTurn = currentTurn == "@"
    val bannerColor = if (isAtTurn) PlayerAtBlue else PlayerHashRed
    val bannerText = if (isAtTurn) "TURN: @ (PLAYER 1)" else "TURN: # (PLAYER 2)"

    val phaseDescription = if (phase == "drop") {
        "Phase 1: Tap empty slot to drop piece (3 pieces each)"
    } else {
        "Phase 2: Tap your piece, then tap highlighted slot to slide"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bannerColor.copy(alpha = 0.12f))
            .border(1.5.dp, bannerColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("status_banner")
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = bannerText,
                    fontFamily = PermanentMarkerFamily,
                    fontSize = 17.sp,
                    color = bannerColor,
                    fontWeight = FontWeight.Bold
                )
                if (turnHint != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "($turnHint)",
                        fontFamily = PatrickHandFamily,
                        fontSize = 14.sp,
                        color = bannerColor
                    )
                }
            }
            Text(
                text = phaseDescription,
                fontFamily = PatrickHandFamily,
                fontSize = 13.sp,
                color = GraphitePencilMuted
            )
        }
    }
}
