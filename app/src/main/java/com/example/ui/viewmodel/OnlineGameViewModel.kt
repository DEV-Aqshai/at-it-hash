package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.GameRoom
import com.example.data.repository.RoomRepository
import com.example.domain.GameLogic
import com.example.ui.sound.SoundEffects
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OnlineUiState(
    val room: GameRoom? = null,
    val selectedPieceIndex: Int? = null,
    val validDestinations: List<Int> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val createdRoomCode: String? = null
)

class OnlineGameViewModel(
    private val repository: RoomRepository
) : ViewModel() {

    private val _state = MutableStateFlow(OnlineUiState())
    val state: StateFlow<OnlineUiState> = _state.asStateFlow()

    private var roomListenerJob: Job? = null
    private var lastObservedUpdatedAt: Any? = null

    val currentUserId: String
        get() = Firebase.auth.currentUser?.uid ?: ""

    val userDisplayName: String
        get() = Firebase.auth.currentUser?.displayName ?: "Student"

    val mySymbol: String
        get() {
            val room = _state.value.room ?: return ""
            return if (room.hostUid == currentUserId) "@" else "#"
        }

    val isMyTurn: Boolean
        get() {
            val room = _state.value.room ?: return false
            if (room.status != "playing") return false
            return room.turn == mySymbol
        }

    fun createRoom(passcode: String?) {
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val result = repository.createRoom(
                hostName = userDisplayName,
                passcode = passcode
            )
            result.onSuccess { roomId ->
                _state.update { it.copy(isLoading = false, createdRoomCode = roomId) }
                startObservingRoom(roomId)
            }.onFailure { err ->
                _state.update { it.copy(isLoading = false, errorMessage = err.localizedMessage ?: "Failed to create room") }
            }
        }
    }

    fun joinRoom(roomCode: String, passcode: String?) {
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val result = repository.joinRoom(
                roomIdInput = roomCode,
                guestName = userDisplayName,
                passcode = passcode
            )
            result.onSuccess { room ->
                _state.update { it.copy(isLoading = false, room = room) }
                startObservingRoom(room.roomId)
            }.onFailure { err ->
                _state.update { it.copy(isLoading = false, errorMessage = err.localizedMessage ?: "Failed to join room") }
            }
        }
    }

    fun startObservingRoom(roomId: String) {
        roomListenerJob?.cancel()
        roomListenerJob = viewModelScope.launch {
            repository.observeRoom(roomId).collect { updatedRoom ->
                if (updatedRoom != null) {
                    val prevRoom = _state.value.room
                    if (prevRoom != null && prevRoom.turn != updatedRoom.turn) {
                        // Play sound when opponent makes a move
                        if (updatedRoom.winner.isNotEmpty()) {
                            if (updatedRoom.winner == "draw") {
                                SoundEffects.playDrawSound()
                            } else {
                                SoundEffects.playWinFanfare()
                            }
                        } else {
                            SoundEffects.playPiecePlace(updatedRoom.turn != "@")
                        }
                    }

                    _state.update {
                        it.copy(
                            room = updatedRoom,
                            selectedPieceIndex = null,
                            validDestinations = emptyList()
                        )
                    }
                }
            }
        }
    }

    fun onSlotClicked(index: Int) {
        val s = _state.value
        val room = s.room ?: return
        if (room.status != "playing" || !isMyTurn) return

        val mySym = mySymbol
        val currentSlot = room.board.getOrElse(index) { "" }

        if (room.phase == "drop") {
            if (currentSlot.isEmpty()) {
                executeOnlineDrop(index)
            }
        } else {
            // Move phase
            if (currentSlot == mySym) {
                if (s.selectedPieceIndex == index) {
                    _state.update { it.copy(selectedPieceIndex = null, validDestinations = emptyList()) }
                } else {
                    val dests = GameLogic.getValidDestinations(room.board, index)
                    SoundEffects.playPencilScribble()
                    _state.update { it.copy(selectedPieceIndex = index, validDestinations = dests) }
                }
            } else if (s.selectedPieceIndex != null && s.validDestinations.contains(index)) {
                executeOnlineMove(from = s.selectedPieceIndex!!, to = index)
            }
        }
    }

    private fun executeOnlineDrop(index: Int) {
        val room = _state.value.room ?: return
        val mySym = mySymbol
        val newBoard = room.board.toMutableList().also { it[index] = mySym }
        val newAtPlaced = if (mySym == "@") room.atPiecesPlaced + 1 else room.atPiecesPlaced
        val newHashPlaced = if (mySym == "#") room.hashPiecesPlaced + 1 else room.hashPiecesPlaced

        val (winSymbol, pattern) = GameLogic.checkWin(newBoard)
        val allPlaced = newAtPlaced == 3L && newHashPlaced == 3L
        val nextPhase = if (allPlaced) "move" else "drop"
        val nextTurn = if (mySym == "@") "#" else "@"

        SoundEffects.playPiecePlace(mySym == "@")

        viewModelScope.launch {
            repository.makeMove(
                roomId = room.roomId,
                newBoard = newBoard,
                fromIndex = -1,
                toIndex = index,
                piece = mySym,
                nextTurn = if (winSymbol.isNotEmpty()) room.turn else nextTurn,
                nextPhase = nextPhase,
                atPlaced = newAtPlaced,
                hashPlaced = newHashPlaced,
                winner = winSymbol,
                winPattern = pattern.map { it.toLong() },
                repeatCount = 0L
            )
        }
    }

    private fun executeOnlineMove(from: Int, to: Int) {
        val room = _state.value.room ?: return
        val mySym = mySymbol
        val newBoard = room.board.toMutableList().also {
            it[from] = ""
            it[to] = mySym
        }

        val newRepeatCount = GameLogic.calculateNewRepeatCount(
            currentRepeatCount = room.repeatMoveCount,
            lastMoveFrom = room.lastMoveFrom,
            lastMoveTo = room.lastMoveTo,
            lastMovePiece = room.lastMovePiece,
            newFrom = from,
            newTo = to,
            newPiece = mySym
        )

        val (winSymbol, pattern) = GameLogic.checkWin(newBoard)
        val isDraw = newRepeatCount >= 5L
        val finalWinner = when {
            winSymbol.isNotEmpty() -> winSymbol
            isDraw -> "draw"
            else -> ""
        }
        val nextTurn = if (mySym == "@") "#" else "@"

        SoundEffects.playValidMove()

        viewModelScope.launch {
            repository.makeMove(
                roomId = room.roomId,
                newBoard = newBoard,
                fromIndex = from,
                toIndex = to,
                piece = mySym,
                nextTurn = if (finalWinner.isNotEmpty()) room.turn else nextTurn,
                nextPhase = "move",
                atPlaced = room.atPiecesPlaced,
                hashPlaced = room.hashPiecesPlaced,
                winner = finalWinner,
                winPattern = pattern.map { it.toLong() },
                repeatCount = if (isDraw) 5L else newRepeatCount
            )
        }
    }

    fun requestRematch() {
        val room = _state.value.room ?: return
        viewModelScope.launch {
            repository.requestRematch(room.roomId)
        }
    }

    fun leaveRoom() {
        val room = _state.value.room ?: return
        viewModelScope.launch {
            repository.leaveRoom(room.roomId)
            roomListenerJob?.cancel()
            _state.update { OnlineUiState() }
        }
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }
}
