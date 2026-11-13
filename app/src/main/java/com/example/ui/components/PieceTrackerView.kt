package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GraphitePencil
import com.example.ui.theme.GraphitePencilMuted
import com.example.ui.theme.HighlighterYellow
import com.example.ui.theme.PatrickHandFamily
import com.example.ui.theme.PermanentMarkerFamily
import com.example.ui.theme.PlayerAtBlue
import com.example.ui.theme.PlayerHashRed

@Composable
fun PieceTrackerView(
    player1Name: String,
    player2Name: String,
    currentTurn: String,
    phase: String,
    atPiecesPlaced: Long,
    hashPiecesPlaced: Long,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Player 1 (@) Card
        PlayerChip(
            symbol = "@",
            name = player1Name,
            color = PlayerAtBlue,
            isTurn = currentTurn == "@",
            piecesPlaced = atPiecesPlaced.toInt(),
            phase = phase,
            modifier = Modifier.weight(1f).testTag("player_at_chip")
        )

        Spacer(modifier = Modifier.width(8.dp))

        // VS badge
        Text(
            text = "VS",
            fontFamily = PermanentMarkerFamily,
            fontSize = 16.sp,
            color = GraphitePencilMuted
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Player 2 (#) Card
        PlayerChip(
            symbol = "#",
            name = player2Name,
            color = PlayerHashRed,
            isTurn = currentTurn == "#",
            piecesPlaced = hashPiecesPlaced.toInt(),
            phase = phase,
            modifier = Modifier.weight(1f).testTag("player_hash_chip")
        )
    }
}

@Composable
private fun PlayerChip(
    symbol: String,
    name: String,
    color: Color,
    isTurn: Boolean,
    piecesPlaced: Int,
    phase: String,
    modifier: Modifier = Modifier
) {
    val bg = if (isTurn) HighlighterYellow.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.8f)
    val borderCol = if (isTurn) color else Color(0xFFCFD8DC)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(if (isTurn) 2.dp else 1.dp, borderCol, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = symbol,
                    fontFamily = PermanentMarkerFamily,
                    fontSize = 20.sp,
                    color = color,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = name,
                    fontFamily = PatrickHandFamily,
                    fontSize = 15.sp,
                    fontWeight = if (isTurn) FontWeight.Bold else FontWeight.Normal,
                    color = GraphitePencil,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 3 piece dots
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 1..3) {
                    val isPlaced = i <= piecesPlaced
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(if (isPlaced) color else Color.Transparent)
                            .border(1.5.dp, color, CircleShape)
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = if (phase == "drop") {
                    val remaining = 3 - piecesPlaced
                    if (remaining > 0) "$remaining in hand" else "Ready to slide"
                } else {
                    "3 on board"
                },
                fontFamily = PatrickHandFamily,
                fontSize = 12.sp,
                color = if (isTurn) color else GraphitePencilMuted
            )
        }
    }
}
