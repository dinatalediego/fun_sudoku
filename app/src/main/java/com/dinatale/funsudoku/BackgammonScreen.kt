package com.dinatale.funsudoku

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val BoardWood = Color(0xFF684536)
private val BoardLight = Color(0xFFE8CBA5)
private val BoardDark = Color(0xFFB95E47)
private val CheckerWhite = Color(0xFFFFF5DF)
private val CheckerBlack = Color(0xFF273449)

@Composable
fun BackgammonScreen(
    modifier: Modifier = Modifier,
    state: BackgammonState,
    onRoll: () -> Unit,
    onNewGame: () -> Unit,
    onSelectSource: (Int) -> Unit,
    onMove: (BackgammonMove) -> Unit,
    onPass: () -> Unit
) {
    val moves = BackgammonEngine.legalMoves(state)
    val selectedSource = state.selectedPoint?.let { if (it == -1) null else it }
    val barSelected = state.selectedPoint == -1
    val selectedMoves = moves.filter { it.from == selectedSource && (barSelected || selectedSource != null) }
    val selectableDestinations = selectedMoves.mapNotNull { it.to }.toSet()

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Backgammon", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                Text("Dos jugadores · mismo teléfono", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            OutlinedButton(onClick = onNewGame) {
                Icon(Icons.Rounded.Refresh, contentDescription = null)
                Spacer(Modifier.width(5.dp))
                Text("Nueva")
            }
        }

        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (state.winner != null) {
                    Text("¡${state.winner.label} ganan!", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                } else {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("Turno de ${state.player.label}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text(if (state.dice.isEmpty()) "Lanza los dados para empezar" else "Usa los dados en cualquier orden", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        PlayerPill(state.player)
                    }
                }

                BackgammonBoard(
                    state = state,
                    selectableSources = moves.mapNotNull { it.from }.toSet(),
                    selectableDestinations = selectableDestinations,
                    onPointClick = { point ->
                        val candidate = selectedMoves.firstOrNull { it.to == point }
                        if (candidate != null) {
                            onMove(candidate)
                        } else if (point in moves.mapNotNull { it.from }) {
                            onSelectSource(point)
                        } else {
                            onSelectSource(-2)
                        }
                    },
                    onBarClick = { onSelectSource(-1) }
                )

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Blancas fuera: ${state.whiteOff}/15", style = MaterialTheme.typography.labelMedium)
                    DiceTray(state.dice)
                    Text("Negras fuera: ${state.blackOff}/15", style = MaterialTheme.typography.labelMedium)
                }

                if (state.winner == null) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (state.dice.isEmpty()) {
                            Button(onClick = onRoll, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Rounded.Casino, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Lanzar dados")
                            }
                        } else if (moves.isEmpty()) {
                            Button(onClick = onPass, modifier = Modifier.weight(1f)) { Text("Sin movimientos · pasar turno") }
                        } else {
                            OutlinedButton(onClick = { onSelectSource(-2) }, modifier = Modifier.weight(1f)) { Text("Cancelar selección") }
                        }
                        if (selectedMoves.any { it.to == null }) {
                            Button(onClick = { onMove(selectedMoves.first { it.to == null }) }, modifier = Modifier.weight(1f)) {
                                Text("Sacar ficha")
                            }
                        }
                    }
                } else {
                    Button(onClick = onNewGame, modifier = Modifier.fillMaxWidth()) { Text("Jugar otra partida") }
                }
            }
        }

        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Cómo se juega", fontWeight = FontWeight.Bold)
                Text("Toca una ficha disponible y luego una punta marcada. Si tienes fichas en la barra, debes reingresarlas primero. Una ficha sola puede ser capturada; gana quien saque sus 15 fichas.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun BackgammonBoard(
    state: BackgammonState,
    selectableSources: Set<Int>,
    selectableDestinations: Set<Int>,
    onPointClick: (Int) -> Unit,
    onBarClick: () -> Unit
) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).border(2.dp, BoardWood, RoundedCornerShape(12.dp)).background(BoardWood).padding(5.dp)
    ) {
        Row(Modifier.fillMaxWidth()) {
            (23 downTo 12).forEach { point ->
                PointColumn(
                    point = point,
                    count = state.points[point],
                    upper = true,
                    selected = state.selectedPoint == point,
                    sourceAvailable = point in selectableSources,
                    destinationAvailable = point in selectableDestinations,
                    onClick = { onPointClick(point) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Row(
            Modifier.fillMaxWidth().height(36.dp).background(Color(0xFF4D352C)),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BarMarker(
                count = state.whiteBar,
                side = BackgammonSide.WHITE,
                active = state.player == BackgammonSide.WHITE && moves.any { it.from == null },
                onClick = onBarClick
            )
            Text("⟵  TABLERO  ⟶", color = Color(0xFFF0DFC9), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            BarMarker(
                count = state.blackBar,
                side = BackgammonSide.BLACK,
                active = state.player == BackgammonSide.BLACK && moves.any { it.from == null },
                onClick = onBarClick
            )
        }
        Row(Modifier.fillMaxWidth()) {
            (0..11).forEach { point ->
                PointColumn(
                    point = point,
                    count = state.points[point],
                    upper = false,
                    selected = state.selectedPoint == point,
                    sourceAvailable = point in selectableSources,
                    destinationAvailable = point in selectableDestinations,
                    onClick = { onPointClick(point) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun PointColumn(
    point: Int,
    count: Int,
    upper: Boolean,
    selected: Boolean,
    sourceAvailable: Boolean,
    destinationAvailable: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val checkerColor = if (count >= 0) CheckerWhite else CheckerBlack
    val shown = minOf(kotlin.math.abs(count), 5)
    Box(
        modifier.height(132.dp)
            .background(if (destinationAvailable) Color(0xFFB5D9B5) else if (selected) Color(0xFFEDC85C) else Color.Transparent)
            .clickable(onClick = onClick)
            .semantics { contentDescription = "Punta ${point + 1}, ${kotlin.math.abs(count)} fichas" },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize().padding(horizontal = 2.dp)) { drawPointTriangle(upper, if (point % 2 == 0) BoardLight else BoardDark) }
        if (upper) {
            Column(Modifier.align(Alignment.TopCenter).padding(top = 5.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(0.dp)) {
                repeat(shown) { Checker(color = checkerColor) }
                if (kotlin.math.abs(count) > 5) Text(kotlin.math.abs(count).toString(), color = if (count >= 0) CheckerBlack else CheckerWhite, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
            Text((point + 1).toString(), Modifier.align(Alignment.BottomCenter).padding(bottom = 2.dp), fontSize = 8.sp, fontWeight = FontWeight.Bold, color = BoardWood)
        } else {
            Column(Modifier.align(Alignment.BottomCenter).padding(bottom = 5.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(0.dp)) {
                if (kotlin.math.abs(count) > 5) Text(kotlin.math.abs(count).toString(), color = if (count >= 0) CheckerBlack else CheckerWhite, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                repeat(shown) { Checker(color = checkerColor) }
            }
            Text((point + 1).toString(), Modifier.align(Alignment.TopCenter).padding(top = 2.dp), fontSize = 8.sp, fontWeight = FontWeight.Bold, color = BoardWood)
        }
        if (sourceAvailable) {
            Box(Modifier.align(if (upper) Alignment.BottomCenter else Alignment.TopCenter).padding(2.dp).size(6.dp).clip(CircleShape).background(Color(0xFF216B45)))
        }
    }
}

@Composable
private fun Checker(color: Color) {
    Box(
        Modifier.size(17.dp).clip(CircleShape).background(color).border(1.dp, if (color == CheckerWhite) Color(0xFFAC9678) else Color(0xFF101A28), CircleShape)
    )
}

@Composable
private fun BarMarker(count: Int, side: BackgammonSide, active: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(8.dp)).background(if (active && count > 0) Color(0xFF94735B) else Color.Transparent).clickable(enabled = active && count > 0, onClick = onClick).padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (count > 0) Checker(if (side == BackgammonSide.WHITE) CheckerWhite else CheckerBlack)
        Text("${if (side == BackgammonSide.WHITE) "B" else "N"} $count", color = Color.White, fontSize = 9.sp)
    }
}

@Composable
private fun PlayerPill(side: BackgammonSide) {
    val color = if (side == BackgammonSide.WHITE) Color(0xFFF1E3C8) else CheckerBlack
    Surface(shape = CircleShape, color = color) {
        Text(side.label, Modifier.padding(horizontal = 12.dp, vertical = 6.dp), color = if (side == BackgammonSide.WHITE) CheckerBlack else Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun DiceTray(dice: List<Int>) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        dice.take(4).forEach { die ->
            Surface(shape = RoundedCornerShape(5.dp), color = Color.White, shadowElevation = 2.dp) {
                Text(die.toString(), Modifier.size(27.dp), textAlign = TextAlign.Center, fontWeight = FontWeight.ExtraBold, color = CheckerBlack, fontSize = 17.sp)
            }
        }
        if (dice.isEmpty()) Text("—", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun DrawScope.drawPointTriangle(upper: Boolean, color: Color) {
    val triangle = Path().apply {
        if (upper) {
            moveTo(0f, 0f)
            lineTo(size.width, 0f)
            lineTo(size.width / 2f, size.height * 0.78f)
        } else {
            moveTo(0f, size.height)
            lineTo(size.width, size.height)
            lineTo(size.width / 2f, size.height * 0.22f)
        }
        close()
    }
    drawPath(triangle, color)
}
