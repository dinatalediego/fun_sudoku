package com.dinatale.funsudoku

import kotlin.random.Random

object SudokuEngine {
    data class Puzzle(val puzzle: IntArray, val solution: IntArray)

    fun generate(difficulty: Difficulty, random: Random = Random.Default): Puzzle {
        val solved = IntArray(81)
        fill(solved, 0, random)
        val puzzle = solved.copyOf()
        val indices = (0 until 81).shuffled(random)
        var clues = 81
        for (index in indices) {
            if (clues <= difficulty.givens) break
            val previous = puzzle[index]
            puzzle[index] = 0
            if (countSolutions(puzzle.copyOf(), 2) != 1) {
                puzzle[index] = previous
            } else {
                clues--
            }
        }
        return Puzzle(puzzle, solved)
    }

    fun candidates(board: IntArray, index: Int): Set<Int> =
        if (board[index] != 0) emptySet() else (1..9).filterTo(mutableSetOf()) { isAllowed(board, index, it) }

    private fun fill(board: IntArray, index: Int, random: Random): Boolean {
        if (index == 81) return true
        if (board[index] != 0) return fill(board, index + 1, random)
        for (n in (1..9).shuffled(random)) {
            if (isAllowed(board, index, n)) {
                board[index] = n
                if (fill(board, index + 1, random)) return true
                board[index] = 0
            }
        }
        return false
    }

    private fun countSolutions(board: IntArray, limit: Int): Int {
        val index = board.indices
            .filter { board[it] == 0 }
            .minByOrNull { candidates(board, it).size } ?: return 1
        var count = 0
        for (n in 1..9) {
            if (isAllowed(board, index, n)) {
                board[index] = n
                count += countSolutions(board, limit - count)
                board[index] = 0
                if (count >= limit) break
            }
        }
        return count
    }

    private fun isAllowed(board: IntArray, index: Int, value: Int): Boolean {
        val row = index / 9
        val col = index % 9
        for (c in 0 until 9) if (board[row * 9 + c] == value) return false
        for (r in 0 until 9) if (board[r * 9 + col] == value) return false
        val br = (row / 3) * 3
        val bc = (col / 3) * 3
        for (r in br until br + 3) for (c in bc until bc + 3) {
            if (board[r * 9 + c] == value) return false
        }
        return true
    }
}
