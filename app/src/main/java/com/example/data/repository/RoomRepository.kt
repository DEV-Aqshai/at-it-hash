package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.GameRoom
import com.example.data.util.OperationType
import com.example.data.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Locale
import kotlin.random.Random

class RoomRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun generateRoomCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..6).map { chars[Random.nextInt(chars.length)] }.joinToString("")
    }

    suspend fun createRoom(hostName: String, passcode: String?): Result<String> {
        val uid = requireUserId()
        val roomId = generateRoomCode()
        val cleanPass = passcode?.trim()?.takeIf { it.isNotEmpty() }
        val docRef = db.collection("rooms").document(roomId)

        val room = GameRoom(
            roomId = roomId,
            hostUid = uid,
            hostName = hostName.ifBlank { "Player @" },
            hasPasscode = cleanPass != null,
            passcode = cleanPass,
            status = "waiting",
            turn = "@",
            phase = "drop",
            board = List(9) { "" },
            atPiecesPlaced = 0L,
            hashPiecesPlaced = 0L,
            winner = "",
            winPattern = emptyList(),
            repeatMoveCount = 0L,
            lastMoveFrom = -1L,
            lastMoveTo = -1L,
            lastMovePiece = "",
            rematchRequestedBy = emptyList(),
            memberUids = listOf(uid)
        )

        return try {
            docRef.set(room.toCreationMap()).await()
            Result.success(roomId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun joinRoom(roomIdInput: String, guestName: String, passcode: String?): Result<GameRoom> {
        val uid = requireUserId()
        val roomId = roomIdInput.trim().uppercase(Locale.ROOT)
        val docRef = db.collection("rooms").document(roomId)

        return try {
            val snapshot = docRef.get().await()
            if (!snapshot.exists()) {
                return Result.failure(IllegalArgumentException("Room $roomId not found."))
            }
            val room = snapshot.toObject(GameRoom::class.java)
                ?: return Result.failure(IllegalStateException("Invalid room format"))

            if (room.hostUid == uid) {
                // Host rejoining their own room
                return Result.success(room)
            }

            if (room.status != "waiting") {
                if (room.guestUid == uid) {
                    return Result.success(room)
                }
                return Result.failure(IllegalStateException("Room is already full or in progress."))
            }

            if (room.hasPasscode && !room.passcode.isNullOrBlank()) {
                val inputPass = passcode?.trim() ?: ""
                if (inputPass != room.passcode) {
                    return Result.failure(IllegalArgumentException("Incorrect passcode for room."))
                }
            }

            val updates = mapOf<String, Any>(
                "guestUid" to uid,
                "guestName" to guestName.ifBlank { "Player #" },
                "status" to "playing",
                "memberUids" to listOf(room.hostUid, uid),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            docRef.update(updates).await()
            val updatedSnap = docRef.get().await()
            val updatedRoom = updatedSnap.toObject(GameRoom::class.java) ?: room
            Result.success(updatedRoom)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeRoom(roomId: String): Flow<GameRoom?> {
        val docRef = db.collection("rooms").document(roomId)
        return docRef.snapshots()
            .map { snap ->
                if (snap.exists()) snap.toObject(GameRoom::class.java) else null
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser != null) {
                    handleFirestoreError(exceptionToLog, OperationType.GET, docRef.path)
                }
                throw error
            }
    }

    suspend fun makeMove(
        roomId: String,
        newBoard: List<String>,
        fromIndex: Int,
        toIndex: Int,
        piece: String,
        nextTurn: String,
        nextPhase: String,
        atPlaced: Long,
        hashPlaced: Long,
        winner: String,
        winPattern: List<Long>,
        repeatCount: Long
    ): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("rooms").document(roomId)

        val updates = mutableMapOf<String, Any>(
            "board" to newBoard,
            "turn" to nextTurn,
            "phase" to nextPhase,
            "atPiecesPlaced" to atPlaced,
            "hashPiecesPlaced" to hashPlaced,
            "winner" to winner,
            "winPattern" to winPattern,
            "repeatMoveCount" to repeatCount,
            "lastMoveFrom" to fromIndex.toLong(),
            "lastMoveTo" to toIndex.toLong(),
            "lastMovePiece" to piece,
            "status" to if (winner.isNotEmpty()) "finished" else "playing",
            "updatedAt" to FieldValue.serverTimestamp()
        )

        return try {
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun requestRematch(roomId: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("rooms").document(roomId)

        return try {
            val snapshot = docRef.get().await()
            val room = snapshot.toObject(GameRoom::class.java)
                ?: return Result.failure(IllegalStateException("Room not found"))

            val currentRematchList = room.rematchRequestedBy.toMutableList()
            if (!currentRematchList.contains(uid)) {
                currentRematchList.add(uid)
            }

            if (currentRematchList.size >= 2) {
                // Both agreed to rematch -> reset board for new round!
                val updates = mapOf<String, Any>(
                    "board" to List(9) { "" },
                    "turn" to "@",
                    "phase" to "drop",
                    "atPiecesPlaced" to 0L,
                    "hashPiecesPlaced" to 0L,
                    "winner" to "",
                    "winPattern" to emptyList<Long>(),
                    "repeatMoveCount" to 0L,
                    "lastMoveFrom" to -1L,
                    "lastMoveTo" to -1L,
                    "lastMovePiece" to "",
                    "status" to "playing",
                    "rematchRequestedBy" to emptyList<String>(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updates).await()
            } else {
                docRef.update(
                    mapOf(
                        "rematchRequestedBy" to currentRematchList,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun leaveRoom(roomId: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("rooms").document(roomId)

        return try {
            val snapshot = docRef.get().await()
            val room = snapshot.toObject(GameRoom::class.java)
            if (room != null && room.status != "finished") {
                val opponentSymbol = if (uid == room.hostUid) "#" else "@"
                docRef.update(
                    mapOf(
                        "status" to "finished",
                        "winner" to opponentSymbol,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }
}
