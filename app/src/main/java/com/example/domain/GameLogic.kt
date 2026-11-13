package com.example.domain

object GameLogic {
    // 3-men's morris / Tapatan board graph adjacency
    val ADJACENCY_MAP: Map<Int, List<Int>> = mapOf(
        0 to listOf(1, 3, 4),
        1 to listOf(0, 2, 4),
        2 to listOf(1, 5, 4),
        3 to listOf(0, 6, 4),
        4 to listOf(0, 1, 2, 3, 5, 6, 7, 8),
        5 to listOf(2, 8, 4),
        6 to listOf(3, 7, 4),
        7 to listOf(6, 8, 4),
        8 to listOf(5, 7, 4)
    )

    val WIN_PATTERNS: List<List<Int>> = listOf(
        // Rows
        listOf(0, 1, 2),
        listOf(3, 4, 5),
        listOf(6, 7, 8),
        // Columns
        listOf(0, 3, 6),
        listOf(1, 4, 7),
        listOf(2, 5, 8),
        // Diagonals
        listOf(0, 4, 8),
        listOf(2, 4, 6)
    )

    fun getValidDestinations(board: List<String>, fromIndex: Int): List<Int> {
        if (fromIndex !in 0..8) return emptyList()
        val neighbors = ADJACENCY_MAP[fromIndex] ?: return emptyList()
        return neighbors.filter { board[it].isEmpty() }
    }

    fun checkWin(board: List<String>): Pair<String, List<Int>> {
        for (pattern in WIN_PATTERNS) {
            val a = board[pattern[0]]
            val b = board[pattern[1]]
            val c = board[pattern[2]]
            if (a.isNotEmpty() && a == b && b == c) {
                return Pair(a, pattern)
            }
        }
        return Pair("", emptyList())
    }

    /**
     * Checks if moving a piece reverses the immediate prior move of that player (e.g. A->B then B->A).
     */
    fun calculateNewRepeatCount(
        currentRepeatCount: Long,
        lastMoveFrom: Long,
        lastMoveTo: Long,
        lastMovePiece: String,
        newFrom: Int,
        newTo: Int,
        newPiece: String
    ): Long {
        if (lastMovePiece == newPiece && lastMoveFrom != -1L) {
            if (lastMoveFrom == newTo.toLong() && lastMoveTo == newFrom.toLong()) {
                return currentRepeatCount + 1L
            }
        }
        return currentRepeatCount
    }

    /**
     * Simple smart AI for Solo Classroom Mode.
     * Looks for immediate win, immediate block, center preference, or random valid move.
     */
    fun findAiMove(
        board: List<String>,
        aiSymbol: String,
        phase: String
    ): AiAction? {
        val opponentSymbol = if (aiSymbol == "@") "#" else "@"

        if (phase == "drop") {
            val emptyIndices = board.indices.filter { board[it].isEmpty() }
            if (emptyIndices.isEmpty()) return null

            // 1. Check if AI can win immediately
            for (idx in emptyIndices) {
                val testBoard = board.toMutableList().also { it[idx] = aiSymbol }
                if (checkWin(testBoard).first == aiSymbol) {
                    return AiAction(fromIndex = -1, toIndex = idx)
                }
            }

            // 2. Block opponent immediate win
            for (idx in emptyIndices) {
                val testBoard = board.toMutableList().also { it[idx] = opponentSymbol }
                if (checkWin(testBoard).first == opponentSymbol) {
                    return AiAction(fromIndex = -1, toIndex = idx)
                }
            }

            // 3. Take center if open
            if (board[4].isEmpty()) {
                return AiAction(fromIndex = -1, toIndex = 4)
            }

            // 4. Prefer corners
            val corners = listOf(0, 2, 6, 8).filter { board[it].isEmpty() }
            if (corners.isNotEmpty()) {
                return AiAction(fromIndex = -1, toIndex = corners.random())
            }

            return AiAction(fromIndex = -1, toIndex = emptyIndices.random())
        } else {
            // Movement phase:
            val myPieceIndices = board.indices.filter { board[it] == aiSymbol }
            val allPossibleMoves = mutableListOf<Pair<Int, Int>>() // Pair(from, to)

            for (from in myPieceIndices) {
                val destinations = getValidDestinations(board, from)
                for (to in destinations) {
                    allPossibleMoves.add(Pair(from, to))
                }
            }

            if (allPossibleMoves.isEmpty()) return null

            // 1. Move to win immediately
            for (move in allPossibleMoves) {
                val testBoard = board.toMutableList().also {
                    it[move.first] = ""
                    it[move.second] = aiSymbol
                }
                if (checkWin(testBoard).first == aiSymbol) {
                    return AiAction(fromIndex = move.first, toIndex = move.second)
                }
            }

            // 2. Move to block opponent
            val oppPieceIndices = board.indices.filter { board[it] == opponentSymbol }
            // If opponent has a winning move next turn, try to occupy or disrupt
            for (move in allPossibleMoves) {
                val testBoard = board.toMutableList().also {
                    it[move.first] = ""
                    it[move.second] = aiSymbol
                }
                // Check if opponent can win next
                var opponentCanWin = false
                for (oppFrom in oppPieceIndices) {
                    val oppDests = getValidDestinations(testBoard, oppFrom)
                    for (oppTo in oppDests) {
                        val oppTestBoard = testBoard.toMutableList().also {
                            it[oppFrom] = ""
                            it[oppTo] = opponentSymbol
                        }
                        if (checkWin(oppTestBoard).first == opponentSymbol) {
                            opponentCanWin = true
                            break
                        }
                    }
                    if (opponentCanWin) break
                }
                if (!opponentCanWin) {
                    return AiAction(fromIndex = move.first, toIndex = move.second)
                }
            }

            // Prefer moving to center if open
            val centerMove = allPossibleMoves.find { it.second == 4 }
            if (centerMove != null) {
                return AiAction(fromIndex = centerMove.first, toIndex = centerMove.second)
            }

            return allPossibleMoves.random().let { AiAction(it.first, it.second) }
        }
    }
}

data class AiAction(
    val fromIndex: Int, // -1 for drop
    val toIndex: Int
)
