package com.dinatale.funsudoku

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class GameViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = app.getSharedPreferences("fun_sudoku", 0)
    var state by mutableStateOf(newGame(Difficulty.MEDIUM))
        private set

    init {
        restore()?.let { state = it }
        viewModelScope.launch {
            while (true) {
                delay(1000)
                if (!state.paused && !state.completed) {
                    state = state.copy(elapsedSeconds = state.elapsedSeconds + 1)
                    if (state.elapsedSeconds % 10L == 0L) persist()
                }
            }
        }
    }

    fun start(difficulty: Difficulty) {
        state = newGame(difficulty)
        persist()
    }

    fun select(index: Int) { if (!state.paused) state = state.copy(selected = index) }
    fun toggleNotes() { state = state.copy(notesMode = !state.notesMode) }
    fun togglePause() { state = state.copy(paused = !state.paused) }

    fun input(value: Int) {
        val index = state.selected ?: return
        val cell = state.cells[index]
        if (cell.fixed || state.paused || state.completed) return
        if (state.notesMode) {
            val notes = cell.notes.toMutableSet().apply { if (!add(value)) remove(value) }
            replace(index, cell.copy(notes = notes, error = false))
            return
        }
        val correct = state.solution[index] == value
        replace(index, cell.copy(value = value, notes = emptySet(), error = !correct), mistakeDelta = if (correct) 0 else 1)
    }

    fun erase() {
        val index = state.selected ?: return
        val cell = state.cells[index]
        if (!cell.fixed && !state.paused) replace(index, cell.copy(value = 0, notes = emptySet(), error = false))
    }

    fun hint() {
        val index = state.selected?.takeIf { !state.cells[it].fixed && state.cells[it].value == 0 }
            ?: state.cells.indices.firstOrNull { !state.cells[it].fixed && state.cells[it].value == 0 }
            ?: return
        val cell = state.cells[index]
        replace(index, cell.copy(value = state.solution[index], notes = emptySet(), error = false), hintDelta = 1)
        state = state.copy(selected = index)
    }

    private fun replace(index: Int, cell: Cell, mistakeDelta: Int = 0, hintDelta: Int = 0) {
        val updated = state.cells.toMutableList().apply { this[index] = cell }
        val completed = updated.indices.all { updated[it].value == state.solution[it] }
        state = state.copy(
            cells = updated,
            mistakes = state.mistakes + mistakeDelta,
            hintsUsed = state.hintsUsed + hintDelta,
            completed = completed
        )
        persist()
        if (completed) recordWin()
    }

    private fun newGame(difficulty: Difficulty): GameState {
        val generated = SudokuEngine.generate(difficulty)
        val cells = generated.puzzle.mapIndexed { i, v ->
            Cell(i / 9, i % 9, value = v, fixed = v != 0)
        }
        return GameState(cells = cells, solution = generated.solution.toList(), difficulty = difficulty)
    }

    private fun persist() {
        val cells = state.cells.joinToString(";") { c -> "${c.value},${if (c.fixed) 1 else 0},${c.notes.sorted().joinToString("")},${if (c.error) 1 else 0}" }
        prefs.edit()
            .putString("cells", cells)
            .putString("solution", state.solution.joinToString(""))
            .putString("difficulty", state.difficulty.name)
            .putInt("mistakes", state.mistakes)
            .putInt("hints", state.hintsUsed)
            .putLong("elapsed", state.elapsedSeconds)
            .putBoolean("completed", state.completed)
            .apply()
    }

    private fun restore(): GameState? = runCatching {
        val raw = prefs.getString("cells", null) ?: return null
        val solution = prefs.getString("solution", null)?.map { it.digitToInt() } ?: return null
        val cells = raw.split(";").mapIndexed { i, token ->
            val p = token.split(",")
            Cell(
                row = i / 9,
                col = i % 9,
                value = p[0].toInt(),
                fixed = p[1] == "1",
                notes = p[2].mapNotNull { it.digitToIntOrNull() }.toSet(),
                error = p.getOrNull(3) == "1"
            )
        }
        GameState(
            cells = cells,
            solution = solution,
            difficulty = Difficulty.valueOf(prefs.getString("difficulty", Difficulty.MEDIUM.name)!!),
            mistakes = prefs.getInt("mistakes", 0),
            hintsUsed = prefs.getInt("hints", 0),
            elapsedSeconds = prefs.getLong("elapsed", 0),
            completed = prefs.getBoolean("completed", false)
        )
    }.getOrNull()

    private fun recordWin() {
        val wins = prefs.getInt("wins", 0) + 1
        val bestKey = "best_${state.difficulty.name}"
        val best = prefs.getLong(bestKey, Long.MAX_VALUE)
        prefs.edit().putInt("wins", wins).apply()
        if (state.elapsedSeconds < best) prefs.edit().putLong(bestKey, state.elapsedSeconds).apply()
    }

    fun stats(): Pair<Int, Long?> {
        val best = prefs.getLong("best_${state.difficulty.name}", Long.MAX_VALUE)
        return prefs.getInt("wins", 0) to best.takeIf { it != Long.MAX_VALUE }
    }
}
