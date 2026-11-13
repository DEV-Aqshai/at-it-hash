package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.GraphitePencil
import com.example.ui.theme.GraphitePencilMuted
import com.example.ui.theme.NotebookMarginRed
import com.example.ui.theme.NotebookPaper
import com.example.ui.theme.PatrickHandFamily
import com.example.ui.theme.PermanentMarkerFamily
import com.example.ui.theme.PlayerAtBlue
import com.example.ui.theme.PlayerHashRed

@Composable
fun HowToPlayDialog(
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(NotebookPaper)
                .border(2.5.dp, GraphitePencil, RoundedCornerShape(16.dp))
                .padding(20.dp)
                .testTag("how_to_play_dialog")
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CLASSROOM RULES",
                        fontFamily = PermanentMarkerFamily,
                        fontSize = 24.sp,
                        color = PlayerAtBlue,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_rules_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close rules",
                            tint = GraphitePencil
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                RuleItem(
                    number = "1",
                    title = "Only 3 Pieces Each (@ vs #)",
                    description = "Player 1 uses @ (blue ink), Player 2 uses # (red ink). Players take turns placing their 3 pieces on any empty intersection."
                )

                RuleItem(
                    number = "2",
                    title = "Win by 3 in a Row",
                    description = "Align your 3 pieces horizontally, vertically, or diagonally at ANY point during the match to claim victory!"
                )

                RuleItem(
                    number = "3",
                    title = "Slide to Near Empty Slots",
                    description = "Once all 3 pieces are on the board, on your turn, select one of your pieces and slide it along the grid lines to an adjacent empty slot."
                )

                RuleItem(
                    number = "4",
                    title = "5-Repeat Stalemate Draw",
                    description = "If players repeat the same move back-and-forth 5 times, the match is officially declared a DRAW! Track this on the Draw Counter."
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("got_it_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PlayerAtBlue
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "GOT IT! LET'S PLAY",
                        fontFamily = PermanentMarkerFamily,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun RuleItem(
    number: String,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(PlayerHashRed),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                color = Color.White,
                fontSize = 14.sp,
                fontFamily = PermanentMarkerFamily,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontFamily = PermanentMarkerFamily,
                fontSize = 16.sp,
                color = GraphitePencil
            )
            Text(
                text = description,
                fontFamily = PatrickHandFamily,
                fontSize = 14.sp,
                color = GraphitePencilMuted,
                lineHeight = 18.sp
            )
        }
    }
}
