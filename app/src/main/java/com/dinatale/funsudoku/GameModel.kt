package com.dinatale.funsudoku

enum class Difficulty(val givens: Int, val label: String) {
    EASY(42, "Fácil"),
    MEDIUM(34, "Medio"),
    HARD(29, "Difícil"),
    EXPERT(25, "Experto")
}

data class Cell(
    val row: Int,
    val col: Int,
    val value: Int = 0,
    val fixed: Boolean = false,
    val notes: Set<Int> = emptySet(),
    val error: Boolean = false
)

data class GameState(
    val cells: List<Cell>,
    val solution: List<Int>,
    val difficulty: Difficulty,
    val selected: Int? = null,
    val notesMode: Boolean = false,
    val mistakes: Int = 0,
    val hintsUsed: Int = 0,
    val elapsedSeconds: Long = 0,
    val paused: Boolean = false,
    val completed: Boolean = false
)
