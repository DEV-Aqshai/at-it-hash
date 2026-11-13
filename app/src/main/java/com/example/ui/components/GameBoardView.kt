package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.sound.SoundEffects
import com.example.ui.theme.GraphitePencil
import com.example.ui.theme.HighlighterYellow
import com.example.ui.theme.PermanentMarkerFamily
import com.example.ui.theme.PlayerAtBlue
import com.example.ui.theme.PlayerHashRed

@Composable
fun GameBoardView(
    board: List<String>,
    currentTurn: String,
    phase: String, // "drop" or "move"
    selectedPieceIndex: Int?,
    validDestinations: List<Int>,
    winPattern: List<Long>,
    isInteractive: Boolean,
    onSlotClicked: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "destPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    BoxWithConstraints(
        modifier = modifier
            .aspectRatio(1f)
            .padding(16.dp)
            .testTag("game_board_container")
    ) {
        val totalSize = maxWidth

        // Background canvas: drawn graph lines (horizontal, vertical, and diagonals)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            val colX = listOf(w * 0.16f, w * 0.5f, w * 0.84f)
            val rowY = listOf(h * 0.16f, h * 0.5f, h * 0.84f)

            val gridLineColor = GraphitePencil.copy(alpha = 0.55f)
            val strokeWidth = 3.5.dp.toPx()

            // 1. Horizontal lines
            for (y in rowY) {
                drawLine(
                    color = gridLineColor,
                    start = Offset(colX[0], y),
                    end = Offset(colX[2], y),
                    strokeWidth = strokeWidth
                )
            }

            // 2. Vertical lines
            for (x in colX) {
                drawLine(
                    color = gridLineColor,
                    start = Offset(x, rowY[0]),
                    end = Offset(x, rowY[2]),
                    strokeWidth = strokeWidth
                )
            }

            // 3. Diagonal lines (Corners through Center)
            // Top-left to bottom-right
            drawLine(
                color = gridLineColor,
                start = Offset(colX[0], rowY[0]),
                end = Offset(colX[2], rowY[2]),
                strokeWidth = strokeWidth
            )
            // Top-right to bottom-left
            drawLine(
                color = gridLineColor,
                start = Offset(colX[2], rowY[0]),
                end = Offset(colX[0], rowY[2]),
                strokeWidth = strokeWidth
            )

            // 4. Subtle circular node dots at each of the 9 positions
            for (r in 0..2) {
                for (c in 0..2) {
                    val idx = r * 3 + c
                    val center = Offset(colX[c], rowY[r])
                    val isWinning = winPattern.contains(idx.toLong())

                    if (isWinning) {
                        drawCircle(
                            color = HighlighterYellow.copy(alpha = 0.8f),
                            radius = 28.dp.toPx(),
                            center = center
                        )
                    }

                    drawCircle(
                        color = GraphitePencil.copy(alpha = 0.35f),
                        radius = 6.dp.toPx(),
                        center = center
                    )
                }
            }
        }

        // Overlay 9 interactive slot nodes
        val slotSize = totalSize * 0.28f
        val positions = listOf(
            Offset(0.02f, 0.02f), Offset(0.36f, 0.02f), Offset(0.70f, 0.02f),
            Offset(0.02f, 0.36f), Offset(0.36f, 0.36f), Offset(0.70f, 0.36f),
            Offset(0.02f, 0.70f), Offset(0.36f, 0.70f), Offset(0.70f, 0.70f)
        )

        for (index in 0..8) {
            val piece = board.getOrElse(index) { "" }
            val isSelected = selectedPieceIndex == index
            val isValidDest = validDestinations.contains(index)
            val isWinningNode = winPattern.contains(index.toLong())
            val offset = positions[index]

            Box(
                modifier = Modifier
                    .fillMaxSize(0.28f)
                    .align(
                        when (index) {
                            0 -> Alignment.TopStart
                            1 -> Alignment.TopCenter
                            2 -> Alignment.TopEnd
                            3 -> Alignment.CenterStart
                            4 -> Alignment.Center
                            5 -> Alignment.CenterEnd
                            6 -> Alignment.BottomStart
                            7 -> Alignment.BottomCenter
                            else -> Alignment.BottomEnd
                        }
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = isInteractive
                    ) {
                        onSlotClicked(index)
                    }
                    .testTag("slot_$index"),
                contentAlignment = Alignment.Center
            ) {
                // Highlighting rings / effects
                Canvas(modifier = Modifier.size(slotSize)) {
                    val center = Offset(size.width / 2, size.height / 2)
                    val r = size.width * 0.42f

                    if (isSelected) {
                        // Highlight selected piece with solid ink ring
                        drawCircle(
                            color = (if (piece == "@") PlayerAtBlue else PlayerHashRed).copy(alpha = 0.25f),
                            radius = r * 1.1f,
                            center = center
                        )
                        drawCircle(
                            color = if (piece == "@") PlayerAtBlue else PlayerHashRed,
                            radius = r,
                            center = center,
                            style = Stroke(
                                width = 3.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f)
                            )
                        )
                    }

                    if (isValidDest) {
                        // Highlight valid destination with pulsing yellow/green sketch circle
                        drawCircle(
                            color = HighlighterYellow.copy(alpha = pulseAlpha * 0.5f),
                            radius = r * 1.05f,
                            center = center
                        )
                        drawCircle(
                            color = Color(0xFFFBC02D).copy(alpha = pulseAlpha),
                            radius = r,
                            center = center,
                            style = Stroke(
                                width = 3.5.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                            )
                        )
                    }
                }

                // Render Piece text (@ or #)
                if (piece.isNotEmpty()) {
                    val pieceColor = if (piece == "@") PlayerAtBlue else PlayerHashRed
                    val isAtPiece = piece == "@"

                    Text(
                        text = piece,
                        fontFamily = PermanentMarkerFamily,
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Bold,
                        color = pieceColor,
                        modifier = Modifier.testTag("piece_${piece}_$index")
                    )
                }
            }
        }
    }
}
