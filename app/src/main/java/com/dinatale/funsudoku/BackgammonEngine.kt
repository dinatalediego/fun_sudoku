package com.dinatale.funsudoku

import kotlin.random.Random

enum class BackgammonSide(val label: String, val sign: Int) {
    WHITE("Blancas", 1),
    BLACK("Negras", -1);

    val opponent: BackgammonSide
        get() = if (this == WHITE) BLACK else WHITE
}

/** A point is numbered 1–24 in the UI and stored as an index from 0–23. */
data class BackgammonState(
    val points: List<Int>,
    val whiteBar: Int = 0,
    val blackBar: Int = 0,
    val whiteOff: Int = 0,
    val blackOff: Int = 0,
    val player: BackgammonSide = BackgammonSide.WHITE,
    val dice: List<Int> = emptyList(),
    val selectedPoint: Int? = null,
    val winner: BackgammonSide? = null,
    val openingRoll: Boolean = true
) {
    fun bar(side: BackgammonSide): Int = if (side == BackgammonSide.WHITE) whiteBar else blackBar
    fun off(side: BackgammonSide): Int = if (side == BackgammonSide.WHITE) whiteOff else blackOff
}

data class BackgammonMove(val from: Int?, val to: Int?, val die: Int)

object BackgammonEngine {
    fun newGame(): BackgammonState {
        val points = MutableList(24) { 0 }
        points[23] = 2
        points[12] = 5
        points[7] = 3
        points[5] = 5
        points[0] = -2
        points[11] = -5
        points[16] = -3
        points[18] = -5
        return BackgammonState(points = points)
    }

    fun roll(state: BackgammonState, random: Random = Random.Default): BackgammonState {
        if (state.winner != null || state.dice.isNotEmpty()) return state
        if (state.openingRoll) {
            var first: Int
            var second: Int
            do {
                first = random.nextInt(1, 7)
                second = random.nextInt(1, 7)
            } while (first == second)
            return state.copy(
                player = if (first > second) BackgammonSide.WHITE else BackgammonSide.BLACK,
                dice = listOf(first, second),
                selectedPoint = null,
                openingRoll = false
            )
        }
        val first = random.nextInt(1, 7)
        val second = random.nextInt(1, 7)
        val dice = if (first == second) List(4) { first } else listOf(first, second)
        return state.copy(dice = dice, selectedPoint = null)
    }

    fun legalMoves(state: BackgammonState): List<BackgammonMove> {
        if (state.dice.isEmpty() || state.winner != null) return emptyList()
        val candidates = basicMoves(state, state.dice)
        if (candidates.isEmpty()) return emptyList()
        val maxMoves = candidates.maxOf { move ->
            1 + maxMovesAfter(applyBoardMove(state, move), state.dice - move.die)
        }
        var allowed = candidates.filter { move ->
            1 + maxMovesAfter(applyBoardMove(state, move), state.dice - move.die) == maxMoves
        }

        // When only one of two different dice can be used, the higher die is mandatory.
        if (maxMoves == 1 && state.dice.distinct().size == 2) {
            val highDie = state.dice.maxOrNull() ?: return allowed
            if (allowed.any { it.die == highDie }) allowed = allowed.filter { it.die == highDie }
        }
        return allowed.distinct()
    }

    fun play(state: BackgammonState, move: BackgammonMove): BackgammonState {
        if (move !in legalMoves(state)) return state
        val moved = applyBoardMove(state, move)
        val remainingDice = state.dice.toMutableList().also { it.remove(move.die) }
        val winner = when {
            moved.whiteOff == 15 -> BackgammonSide.WHITE
            moved.blackOff == 15 -> BackgammonSide.BLACK
            else -> null
        }
        if (winner != null) return moved.copy(dice = emptyList(), selectedPoint = null, winner = winner)
        return if (remainingDice.isEmpty()) {
            moved.copy(player = state.player.opponent, dice = emptyList(), selectedPoint = null)
        } else {
            moved.copy(dice = remainingDice, selectedPoint = null)
        }
    }

    fun pass(state: BackgammonState): BackgammonState {
        if (state.dice.isEmpty() || legalMoves(state).isNotEmpty() || state.winner != null) return state
        return state.copy(player = state.player.opponent, dice = emptyList(), selectedPoint = null)
    }

    private fun maxMovesAfter(state: BackgammonState, dice: List<Int>): Int {
        if (dice.isEmpty()) return 0
        val candidates = basicMoves(state.copy(dice = dice), dice)
        return candidates.maxOfOrNull { move ->
            1 + maxMovesAfter(applyBoardMove(state, move), dice - move.die)
        } ?: 0
    }

    private fun basicMoves(state: BackgammonState, dice: List<Int>): List<BackgammonMove> {
        val side = state.player
        val barCount = state.bar(side)
        val sources: List<Int?> = if (barCount > 0) listOf(null) else state.points.indices
            .filter { state.points[it] * side.sign > 0 }
            .map { it }
        return buildList {
            sources.forEach { from ->
                dice.distinct().forEach { die ->
                    val to = if (from == null) {
                        if (side == BackgammonSide.WHITE) 24 - die else die - 1
                    } else {
                        from + side.sign * -die
                    }
                    if (from == null || to in state.points.indices) {
                        if (to in state.points.indices) {
                            val target = state.points[to]
                            if (target * side.sign >= -1) add(BackgammonMove(from, to, die))
                        } else if (canBearOff(state, side) && from != null && canUseForBearOff(state, side, from, die)) {
                            add(BackgammonMove(from, null, die))
                        }
                    }
                }
            }
        }
    }

    private fun canBearOff(state: BackgammonState, side: BackgammonSide): Boolean {
        if (state.bar(side) > 0) return false
        val home = if (side == BackgammonSide.WHITE) 0..5 else 18..23
        return state.points.indices.all { index ->
            state.points[index] * side.sign <= 0 || index in home
        }
    }

    private fun canUseForBearOff(state: BackgammonState, side: BackgammonSide, from: Int, die: Int): Boolean {
        if (side == BackgammonSide.WHITE) {
            val distance = from + 1
            return die == distance || (die > distance && (from + 1..5).none { state.points[it] > 0 })
        }
        val distance = 24 - from
        return die == distance || (die > distance && (18 until from).none { state.points[it] < 0 })
    }

    private fun applyBoardMove(state: BackgammonState, move: BackgammonMove): BackgammonState {
        val side = state.player
        val points = state.points.toMutableList()
        var whiteBar = state.whiteBar
        var blackBar = state.blackBar
        var whiteOff = state.whiteOff
        var blackOff = state.blackOff

        if (move.from == null) {
            if (side == BackgammonSide.WHITE) whiteBar-- else blackBar--
        } else {
            points[move.from] -= side.sign
        }

        if (move.to == null) {
            if (side == BackgammonSide.WHITE) whiteOff++ else blackOff++
        } else {
            if (points[move.to] * side.sign == -1) {
                points[move.to] = 0
                if (side == BackgammonSide.WHITE) blackBar++ else whiteBar++
            }
            points[move.to] += side.sign
        }
        return state.copy(
            points = points,
            whiteBar = whiteBar,
            blackBar = blackBar,
            whiteOff = whiteOff,
            blackOff = blackOff
        )
    }
}
