package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.GameLogic
import com.example.ui.sound.SoundEffects
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LocalGameState(
    val board: List<String> = List(9) { "" },
    val turn: String = "@", // "@" starts
    val phase: String = "drop", // "drop" or "move"
    val atPiecesPlaced: Int = 0,
    val hashPiecesPlaced: Int = 0,
    val winner: String = "", // "", "@", "#", "draw"
    val winPattern: List<Long> = emptyList(),
    val selectedPieceIndex: Int? = null,
    val validDestinations: List<Int> = emptyList(),
    val repeatMoveCount: Long = 0L,
    val lastMoveFrom: Long = -1L,
    val lastMoveTo: Long = -1L,
    val lastMovePiece: String = "",
    val isVsAi: Boolean = true,
    val player1Name: String = "You (@)",
    val player2Name: String = "Doodle Bot (#)",
    val atWins: Int = 0,
    val hashWins: Int = 0,
    val draws: Int = 0
)

class LocalGameViewModel : ViewModel() {
    private val _state = MutableStateFlow(LocalGameState())
    val state: StateFlow<LocalGameState> = _state.asStateFlow()

    fun setGameMode(vsAi: Boolean) {
        _state.update {
            it.copy(
                isVsAi = vsAi,
                player1Name = if (vsAi) "You (@)" else "Player 1 (@)",
                player2Name = if (vsAi) "Doodle Bot (#)" else "Player 2 (#)"
            )
        }
        resetGame()
    }

    fun onSlotClicked(index: Int) {
        val s = _state.value
        if (s.winner.isNotEmpty()) return

        // If it's vs AI and it's AI's turn, ignore clicks
        if (s.isVsAi && s.turn == "#") return

        handleSlotSelection(index)
    }

    private fun handleSlotSelection(index: Int) {
        val s = _state.value
        val currentPiece = s.board[index]

        if (s.phase == "drop") {
            // Drop phase: click empty slot to place piece
            if (currentPiece.isEmpty()) {
                executeDrop(index, s.turn)
            }
        } else {
            // Movement phase:
            val currentTurn = s.turn
            if (currentPiece == currentTurn) {
                // Select or switch selection to this piece
                if (s.selectedPieceIndex == index) {
                    // Deselect
                    _state.update { it.copy(selectedPieceIndex = null, validDestinations = emptyList()) }
                } else {
                    val dests = GameLogic.getValidDestinations(s.board, index)
                    SoundEffects.playPencilScribble()
                    _state.update { it.copy(selectedPieceIndex = index, validDestinations = dests) }
                }
            } else if (s.selectedPieceIndex != null && s.validDestinations.contains(index)) {
                // Move selected piece to destination
                executeMove(from = s.selectedPieceIndex!!, to = index, piece = currentTurn)
            }
        }
    }

    private fun executeDrop(index: Int, piece: String) {
        val s = _state.value
        val newBoard = s.board.toMutableList().also { it[index] = piece }
        val newAtPlaced = if (piece == "@") s.atPiecesPlaced + 1 else s.atPiecesPlaced
        val newHashPlaced = if (piece == "#") s.hashPiecesPlaced + 1 else s.hashPiecesPlaced

        SoundEffects.playPiecePlace(piece == "@")

        val (winSymbol, pattern) = GameLogic.checkWin(newBoard)
        if (winSymbol.isNotEmpty()) {
            SoundEffects.playWinFanfare()
            _state.update {
                it.copy(
                    board = newBoard,
                    atPiecesPlaced = newAtPlaced,
                    hashPiecesPlaced = newHashPlaced,
                    winner = winSymbol,
                    winPattern = pattern.map { p -> p.toLong() },
                    atWins = if (winSymbol == "@") it.atWins + 1 else it.atWins,
                    hashWins = if (winSymbol == "#") it.hashWins + 1 else it.hashWins
                )
            }
            return
        }

        // Check if placement phase is complete (3 each = 6 total)
        val allPlaced = newAtPlaced == 3 && newHashPlaced == 3
        val nextPhase = if (allPlaced) "move" else "drop"
        val nextTurn = if (piece == "@") "#" else "@"

        _state.update {
            it.copy(
                board = newBoard,
                turn = nextTurn,
                phase = nextPhase,
                atPiecesPlaced = newAtPlaced,
                hashPiecesPlaced = newHashPlaced,
                selectedPieceIndex = null,
                validDestinations = emptyList()
            )
        }

        checkTriggerAi()
    }

    private fun executeMove(from: Int, to: Int, piece: String) {
        val s = _state.value
        val newBoard = s.board.toMutableList().also {
            it[from] = ""
            it[to] = piece
        }

        SoundEffects.playValidMove()

        val newRepeatCount = GameLogic.calculateNewRepeatCount(
            currentRepeatCount = s.repeatMoveCount,
            lastMoveFrom = s.lastMoveFrom,
            lastMoveTo = s.lastMoveTo,
            lastMovePiece = s.lastMovePiece,
            newFrom = from,
            newTo = to,
            newPiece = piece
        )

        val (winSymbol, pattern) = GameLogic.checkWin(newBoard)
        if (winSymbol.isNotEmpty()) {
            SoundEffects.playWinFanfare()
            _state.update {
                it.copy(
                    board = newBoard,
                    winner = winSymbol,
                    winPattern = pattern.map { p -> p.toLong() },
                    selectedPieceIndex = null,
                    validDestinations = emptyList(),
                    atWins = if (winSymbol == "@") it.atWins + 1 else it.atWins,
                    hashWins = if (winSymbol == "#") it.hashWins + 1 else it.hashWins
                )
            }
            return
        }

        // Draw condition: 5 consecutive repeated moves
        if (newRepeatCount >= 5L) {
            SoundEffects.playDrawSound()
            _state.update {
                it.copy(
                    board = newBoard,
                    winner = "draw",
                    repeatMoveCount = 5L,
                    selectedPieceIndex = null,
                    validDestinations = emptyList(),
                    draws = it.draws + 1
                )
            }
            return
        }

        val nextTurn = if (piece == "@") "#" else "@"
        _state.update {
            it.copy(
                board = newBoard,
                turn = nextTurn,
                selectedPieceIndex = null,
                validDestinations = emptyList(),
                repeatMoveCount = newRepeatCount,
                lastMoveFrom = from.toLong(),
                lastMoveTo = to.toLong(),
                lastMovePiece = piece
            )
        }

        checkTriggerAi()
    }

    private fun checkTriggerAi() {
        val s = _state.value
        if (s.isVsAi && s.turn == "#" && s.winner.isEmpty()) {
            viewModelScope.launch {
                delay(450) // Realistic thoughtful hesitation
                val action = GameLogic.findAiMove(s.board, "#", s.phase)
                if (action != null) {
                    if (s.phase == "drop") {
                        executeDrop(action.toIndex, "#")
                    } else {
                        executeMove(action.fromIndex, action.toIndex, "#")
                    }
                }
            }
        }
    }

    fun resetGame() {
        SoundEffects.playEraserSwipe()
        _state.update {
            it.copy(
                board = List(9) { "" },
                turn = "@",
                phase = "drop",
                atPiecesPlaced = 0,
                hashPiecesPlaced = 0,
                winner = "",
                winPattern = emptyList(),
                selectedPieceIndex = null,
                validDestinations = emptyList(),
                repeatMoveCount = 0L,
                lastMoveFrom = -1L,
                lastMoveTo = -1L,
                lastMovePiece = ""
            )
        }
    }
}
