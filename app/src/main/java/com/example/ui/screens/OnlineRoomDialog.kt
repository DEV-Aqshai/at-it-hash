package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.GraphitePencil
import com.example.ui.theme.GraphitePencilMuted
import com.example.ui.theme.HighlighterYellow
import com.example.ui.theme.NotebookPaper
import com.example.ui.theme.PatrickHandFamily
import com.example.ui.theme.PermanentMarkerFamily
import com.example.ui.theme.PlayerAtBlue
import com.example.ui.theme.PlayerHashRed

@Composable
fun OnlineRoomDialog(
    isLoading: Boolean,
    errorMessage: String?,
    createdRoomCode: String?,
    isWaitingForGuest: Boolean,
    onCreateRoom: (passcode: String?) -> Unit,
    onJoinRoom: (roomCode: String, passcode: String?) -> Unit,
    onCancelWaiting: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Create, 1 = Join
    var roomCodeInput by remember { mutableStateOf("") }
    var passcodeInput by remember { mutableStateOf("") }
    var createPasscodeInput by remember { mutableStateOf("") }

    Dialog(onDismissRequest = {
        if (!isWaitingForGuest) onDismiss()
    }) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(NotebookPaper)
                .border(2.5.dp, GraphitePencil, RoundedCornerShape(16.dp))
                .padding(20.dp)
                .testTag("online_room_dialog")
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ONLINE MATCHROOM",
                        fontFamily = PermanentMarkerFamily,
                        fontSize = 22.sp,
                        color = PlayerAtBlue
                    )
                    IconButton(
                        onClick = {
                            if (isWaitingForGuest) onCancelWaiting()
                            onDismiss()
                        },
                        modifier = Modifier.testTag("close_online_dialog")
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = GraphitePencil)
                    }
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        color = PlayerHashRed,
                        fontFamily = PatrickHandFamily,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                if (isWaitingForGuest && createdRoomCode != null) {
                    // Waiting lobby state
                    Spacer(modifier = Modifier.height(16.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "ROOM CREATED!",
                            fontFamily = PermanentMarkerFamily,
                            fontSize = 18.sp,
                            color = PlayerAtBlue
                        )
                        Text(
                            text = "Share this code with your friend to play:",
                            fontFamily = PatrickHandFamily,
                            fontSize = 15.sp,
                            color = GraphitePencilMuted
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Big room code box with tap to copy
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(HighlighterYellow)
                                .border(2.dp, GraphitePencil, RoundedCornerShape(10.dp))
                                .clickable {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Room Code", createdRoomCode))
                                    Toast.makeText(context, "Room code copied!", Toast.LENGTH_SHORT).show()
                                }
                                .padding(horizontal = 24.dp, vertical = 12.dp)
                                .testTag("copy_room_code_box"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = createdRoomCode,
                                    fontFamily = PermanentMarkerFamily,
                                    fontSize = 32.sp,
                                    color = GraphitePencil,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 2.sp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", tint = GraphitePencil)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        CircularProgressIndicator(
                            color = PlayerAtBlue,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Waiting for Player 2 (#) to connect...",
                            fontFamily = PatrickHandFamily,
                            fontSize = 15.sp,
                            color = GraphitePencil
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = onCancelWaiting,
                            colors = ButtonDefaults.buttonColors(containerColor = PlayerHashRed),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(text = "CANCEL ROOM", fontFamily = PermanentMarkerFamily, color = Color.White)
                        }
                    }
                } else {
                    // Tabs: Create Room / Join Room
                    Spacer(modifier = Modifier.height(10.dp))

                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.Transparent,
                        contentColor = PlayerAtBlue
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = {
                                Text(
                                    text = "CREATE ROOM",
                                    fontFamily = PermanentMarkerFamily,
                                    fontSize = 15.sp
                                )
                            }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = {
                                Text(
                                    text = "JOIN ROOM",
                                    fontFamily = PermanentMarkerFamily,
                                    fontSize = 15.sp
                                )
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (selectedTab == 0) {
                        // Create Room content
                        Text(
                            text = "You will play as @ (Player 1)",
                            fontFamily = PatrickHandFamily,
                            fontSize = 16.sp,
                            color = PlayerAtBlue,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = createPasscodeInput,
                            onValueChange = { createPasscodeInput = it.take(8) },
                            label = { Text("Passcode / PIN (Optional)") },
                            singleLine = true,
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = GraphitePencilMuted)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("create_passcode_input")
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                onCreateRoom(createPasscodeInput.ifBlank { null })
                            },
                            enabled = !isLoading,
                            colors = ButtonDefaults.buttonColors(containerColor = PlayerAtBlue),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("submit_create_room_button")
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                            } else {
                                Text(
                                    text = "CREATE & GET CODE",
                                    fontFamily = PermanentMarkerFamily,
                                    fontSize = 16.sp,
                                    color = Color.White
                                )
                            }
                        }
                    } else {
                        // Join Room content
                        Text(
                            text = "You will play as # (Player 2)",
                            fontFamily = PatrickHandFamily,
                            fontSize = 16.sp,
                            color = PlayerHashRed,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = roomCodeInput,
                            onValueChange = { roomCodeInput = it.uppercase().take(6) },
                            label = { Text("Enter 6-Letter Room Code") },
                            placeholder = { Text("e.g. HASH42") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("join_room_code_input")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = passcodeInput,
                            onValueChange = { passcodeInput = it.take(8) },
                            label = { Text("Passcode (if required)") },
                            singleLine = true,
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = GraphitePencilMuted)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("join_passcode_input")
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                onJoinRoom(roomCodeInput.trim(), passcodeInput.ifBlank { null })
                            },
                            enabled = !isLoading && roomCodeInput.length >= 4,
                            colors = ButtonDefaults.buttonColors(containerColor = PlayerHashRed),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("submit_join_room_button")
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                            } else {
                                Text(
                                    text = "JOIN MATCH",
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
