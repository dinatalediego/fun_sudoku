package com.dinatale.funsudoku

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class SudokuEngineTest {
    @Test
    fun generatedPuzzleMatchesItsSolution() {
        val generated = SudokuEngine.generate(Difficulty.EASY, Random(42))
        assertEquals(81, generated.puzzle.size)
        assertEquals(81, generated.solution.size)
        generated.puzzle.indices.forEach { i ->
            if (generated.puzzle[i] != 0) assertEquals(generated.solution[i], generated.puzzle[i])
        }
    }

    @Test
    fun solutionContainsDigitsOneToNineInEveryRow() {
        val solution = SudokuEngine.generate(Difficulty.MEDIUM, Random(7)).solution
        repeat(9) { row ->
            assertEquals((1..9).toSet(), solution.slice(row * 9 until row * 9 + 9).toSet())
        }
    }

    @Test
    fun generatedPuzzleHasExpectedMinimumClues() {
        val puzzle = SudokuEngine.generate(Difficulty.HARD, Random(9)).puzzle
        assertTrue(puzzle.count { it != 0 } >= Difficulty.HARD.givens)
    }
}
