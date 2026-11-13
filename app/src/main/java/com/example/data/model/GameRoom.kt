package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class GameRoom(
    @DocumentId val id: String = "",
    val roomId: String = "",
    val hostUid: String = "",
    val hostName: String = "",
    val guestUid: String? = null,
    val guestName: String? = null,
    val hasPasscode: Boolean = false,
    val passcode: String? = null,
    val status: String = "waiting", // "waiting", "playing", "finished"
    val turn: String = "@", // "@" or "#"
    val phase: String = "drop", // "drop" or "move"
    val board: List<String> = List(9) { "" },
    val atPiecesPlaced: Long = 0L,
    val hashPiecesPlaced: Long = 0L,
    val winner: String = "", // "", "@", "#", "draw"
    val winPattern: List<Long> = emptyList(),
    val repeatMoveCount: Long = 0L,
    val lastMoveFrom: Long = -1L,
    val lastMoveTo: Long = -1L,
    val lastMovePiece: String = "",
    val rematchRequestedBy: List<String> = emptyList(),
    val memberUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreationMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "roomId" to roomId,
            "hostUid" to hostUid,
            "hostName" to hostName,
            "hasPasscode" to hasPasscode,
            "status" to status,
            "turn" to turn,
            "phase" to phase,
            "board" to board,
            "atPiecesPlaced" to atPiecesPlaced,
            "hashPiecesPlaced" to hashPiecesPlaced,
            "winner" to winner,
            "winPattern" to winPattern,
            "repeatMoveCount" to repeatMoveCount,
            "lastMoveFrom" to lastMoveFrom,
            "lastMoveTo" to lastMoveTo,
            "lastMovePiece" to lastMovePiece,
            "rematchRequestedBy" to rematchRequestedBy,
            "memberUids" to memberUids,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (passcode != null) map["passcode"] = passcode
        if (guestUid != null) map["guestUid"] = guestUid
        if (guestName != null) map["guestName"] = guestName
        return map
    }
}
