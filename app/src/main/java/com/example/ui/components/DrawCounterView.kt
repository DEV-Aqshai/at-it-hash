package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
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
import com.example.ui.theme.PlayerHashRed

@Composable
fun DrawCounterView(
    repeatMoveCount: Long,
    modifier: Modifier = Modifier
) {
    val isNearDraw = repeatMoveCount >= 3
    val isDraw = repeatMoveCount >= 5

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isNearDraw && !isDraw) 1.05f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val cardBg by animateColorAsState(
        targetValue = when {
            isDraw -> Color(0xFFFFEBEE)
            isNearDraw -> Color(0xFFFFFDE7)
            else -> Color(0xFFF9F7E8)
        },
        label = "bgColor"
    )

    val borderColor by animateColorAsState(
        targetValue = when {
            isDraw -> PlayerHashRed
            isNearDraw -> Color(0xFFFBC02D)
            else -> Color(0xFFD7CCC8)
        },
        label = "borderColor"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(cardBg)
            .border(1.5.dp, borderColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag("draw_counter_card")
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "REPEATED MOVE DRAW TRACKER",
                    fontFamily = PermanentMarkerFamily,
                    fontSize = 13.sp,
                    color = if (isNearDraw) PlayerHashRed else GraphitePencil
                )
                Text(
                    text = "$repeatMoveCount / 5",
                    fontFamily = PermanentMarkerFamily,
                    fontSize = 16.sp,
                    color = if (isDraw) PlayerHashRed else GraphitePencil,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("draw_counter_text")
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Tally Marks representation (5 boxes)
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 1..5) {
                    val isChecked = i <= repeatMoveCount
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(28.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isChecked) HighlighterYellow else Color.White)
                            .border(
                                width = 1.dp,
                                color = if (isChecked) GraphitePencil else Color(0xFFCFD8DC),
                                shape = RoundedCornerShape(4.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isChecked) {
                            if (i == 5) {
                                // 5th tally cross
                                Text(
                                    text = "✕",
                                    fontFamily = PermanentMarkerFamily,
                                    fontSize = 18.sp,
                                    color = PlayerHashRed,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Text(
                                    text = "/",
                                    fontFamily = PermanentMarkerFamily,
                                    fontSize = 18.sp,
                                    color = GraphitePencil,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.rotate(15f)
                                )
                            }
                        } else {
                            Text(
                                text = "·",
                                fontSize = 16.sp,
                                color = GraphitePencilMuted
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = when {
                    isDraw -> "STALEMATE! Repeated move reached 5 times."
                    repeatMoveCount >= 4 -> "WARNING: 1 more repeated move triggers DRAW!"
                    repeatMoveCount >= 2 -> "Note: Repeating the same move back-and-forth increments tally."
                    else -> "5 consecutive repeated moves triggers a draw."
                },
                fontFamily = PatrickHandFamily,
                fontSize = 13.sp,
                color = if (isNearDraw) PlayerHashRed else GraphitePencilMuted
            )
        }
    }
}
