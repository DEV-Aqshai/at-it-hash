package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.SmartToy
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.HowToPlayDialog
import com.example.ui.components.NotebookScaffold
import com.example.ui.sound.SoundEffects
import com.example.ui.theme.GraphitePencil
import com.example.ui.theme.GraphitePencilMuted
import com.example.ui.theme.HighlighterYellow
import com.example.ui.theme.PatrickHandFamily
import com.example.ui.theme.PermanentMarkerFamily
import com.example.ui.theme.PlayerAtBlue
import com.example.ui.theme.PlayerHashRed
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

@Composable
fun HomeScreen(
    onPlaySoloVsBot: () -> Unit,
    onPlayPassAndPlay: () -> Unit,
    onOpenOnlineLobby: () -> Unit,
    onSignInClicked: () -> Unit,
    onSignOutClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showHowToPlay by remember { mutableStateOf(false) }
    val currentUser = Firebase.auth.currentUser

    if (showHowToPlay) {
        HowToPlayDialog(onDismiss = { showHowToPlay = false })
    }

    NotebookScaffold(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar with Rules button & User Profile
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Auth status pill
                if (currentUser != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White.copy(alpha = 0.85f))
                            .border(1.dp, Color(0xFFCFD8DC), RoundedCornerShape(20.dp))
                            .clickable { onSignOutClicked() }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                            .testTag("user_profile_pill")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "User",
                            tint = PlayerAtBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = (currentUser.displayName ?: "Student").take(14),
                            fontFamily = PatrickHandFamily,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = GraphitePencil
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "(Sign Out)",
                            fontFamily = PatrickHandFamily,
                            fontSize = 12.sp,
                            color = PlayerHashRed
                        )
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(PlayerAtBlue.copy(alpha = 0.1f))
                            .border(1.dp, PlayerAtBlue, RoundedCornerShape(20.dp))
                            .clickable { onSignInClicked() }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                            .testTag("sign_in_pill")
                    ) {
                        Text(
                            text = "Sign In for Online",
                            fontFamily = PermanentMarkerFamily,
                            fontSize = 13.sp,
                            color = PlayerAtBlue
                        )
                    }
                }

                // How to play button
                IconButton(
                    onClick = {
                        SoundEffects.playPencilScribble()
                        showHowToPlay = true
                    },
                    modifier = Modifier.testTag("how_to_play_icon_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.HelpOutline,
                        contentDescription = "Rules",
                        tint = GraphitePencil,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Game Logo
            Image(
                painter = painterResource(id = R.drawable.at_it_hash_logo),
                contentDescription = "at it hash logo",
                modifier = Modifier
                    .size(110.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .border(2.5.dp, GraphitePencil, RoundedCornerShape(24.dp))
                    .testTag("home_logo")
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "at it hash",
                fontFamily = PermanentMarkerFamily,
                fontSize = 42.sp,
                color = PlayerAtBlue,
                letterSpacing = 1.sp
            )

            // Highlighter subtitle
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(HighlighterYellow)
                    .padding(horizontal = 10.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "3-PIECE SLIDING TIC TAC TOE",
                    fontFamily = PermanentMarkerFamily,
                    fontSize = 15.sp,
                    color = GraphitePencil
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Place 3 pieces, slide adjacent, draw at 5 repeat moves!",
                fontFamily = PatrickHandFamily,
                fontSize = 15.sp,
                color = GraphitePencilMuted
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Game Mode Cards
            ModeCard(
                title = "SOLO VS BOT",
                subtitle = "Practice against Doodle Bot in offline class mode",
                badge = "@ vs #",
                icon = Icons.Default.SmartToy,
                accentColor = PlayerAtBlue,
                onClick = {
                    SoundEffects.playPiecePlace(true)
                    onPlaySoloVsBot()
                },
                testTag = "mode_solo_vs_bot"
            )

            Spacer(modifier = Modifier.height(14.dp))

            ModeCard(
                title = "PASS & PLAY",
                subtitle = "Play with a friend sitting next to you on 1 phone",
                badge = "Local 2P",
                icon = Icons.Default.PhoneAndroid,
                accentColor = Color(0xFF2E7D32),
                onClick = {
                    SoundEffects.playPiecePlace(false)
                    onPlayPassAndPlay()
                },
                testTag = "mode_pass_and_play"
            )

            Spacer(modifier = Modifier.height(14.dp))

            ModeCard(
                title = "ONLINE MULTIPLAYER",
                subtitle = "Create or join a room code with optional pass",
                badge = "Firebase Live",
                icon = Icons.Default.Public,
                accentColor = PlayerHashRed,
                onClick = {
                    SoundEffects.playValidMove()
                    onOpenOnlineLobby()
                },
                testTag = "mode_online_multiplayer"
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Rules button at bottom
            Button(
                onClick = {
                    SoundEffects.playPencilScribble()
                    showHowToPlay = true
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, GraphitePencil, RoundedCornerShape(10.dp))
                    .testTag("how_to_play_card_button")
            ) {
                Text(
                    text = "📖 HOW TO PLAY & DRAW RULES",
                    fontFamily = PermanentMarkerFamily,
                    fontSize = 16.sp,
                    color = GraphitePencil
                )
            }
        }
    }
}

@Composable
private fun ModeCard(
    title: String,
    subtitle: String,
    badge: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.95f))
            .border(2.dp, accentColor, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(16.dp)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        fontFamily = PermanentMarkerFamily,
                        fontSize = 18.sp,
                        color = GraphitePencil
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(accentColor)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badge,
                            fontFamily = PermanentMarkerFamily,
                            fontSize = 10.sp,
                            color = Color.White
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontFamily = PatrickHandFamily,
                    fontSize = 14.sp,
                    color = GraphitePencilMuted
                )
            }
        }
    }
}
