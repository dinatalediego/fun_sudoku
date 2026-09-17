package com.dinatale.funsudoku

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { FunSudokuTheme { SudokuApp() } }
    }
}

@Composable
private fun FunSudokuTheme(content: @Composable () -> Unit) {
    val scheme = lightColorScheme(
        primary = Color(0xFF415F91),
        secondary = Color(0xFF565F71),
        tertiary = Color(0xFF705575),
        background = Color(0xFFF9F9FF),
        surface = Color(0xFFF9F9FF)
    )
    MaterialTheme(colorScheme = scheme, typography = Typography(), content = content)
}

@Composable
private fun SudokuApp(vm: GameViewModel = viewModel()) {
    val game = vm.state
    var showDifficulty by remember { mutableStateOf(false) }
    val (wins, best) = vm.stats()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("FUN SUDOKU", fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp) },
                actions = {
                    IconButton(onClick = { showDifficulty = true }) {
                        Icon(Icons.Rounded.Add, contentDescription = "Nueva partida")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            GameHeader(game, wins, best)
            Spacer(Modifier.height(12.dp))
            SudokuBoard(game, vm::select)
            Spacer(Modifier.height(18.dp))
            ActionRow(game, vm)
            Spacer(Modifier.height(16.dp))
            NumberPad(game, vm::input)
            Spacer(Modifier.weight(1f))
            Text(
                "Sudoku limpio, rápido y sin anuncios",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 14.dp)
            )
        }
    }

    if (showDifficulty) {
        AlertDialog(
            onDismissRequest = { showDifficulty = false },
            title = { Text("Nueva partida") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Difficulty.entries.forEach { difficulty ->
                        OutlinedButton(
                            onClick = { vm.start(difficulty); showDifficulty = false },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(difficulty.label) }
                    }
                }
            },
            confirmButton = {}
        )
    }

    if (game.completed) {
        AlertDialog(
            onDismissRequest = {},
            icon = { Icon(Icons.Rounded.EmojiEvents, contentDescription = null) },
            title = { Text("¡Sudoku completado!") },
            text = {
                Text("Tiempo ${formatTime(game.elapsedSeconds)} · ${game.mistakes} errores · ${game.hintsUsed} pistas")
            },
            confirmButton = {
                Button(onClick = { vm.start(game.difficulty) }) { Text("Jugar otro") }
            },
            dismissButton = {
                TextButton(onClick = { showDifficulty = true }) { Text("Cambiar nivel") }
            }
        )
    }

    if (game.paused) {
        Box(
            Modifier.fillMaxSize().background(Color(0xE6F9F9FF)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Rounded.PauseCircle, null, modifier = Modifier.size(64.dp))
                Spacer(Modifier.height(12.dp))
                Text("Partida en pausa", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                Button(onClick = vm::togglePause) {
                    Icon(Icons.Rounded.PlayArrow, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Continuar")
                }
            }
        }
    }
}

@Composable
private fun GameHeader(game: GameState, wins: Int, best: Long?) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(game.difficulty.label, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
            Text("$wins partidas ganadas", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(formatTime(game.elapsedSeconds), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                best?.let { "Récord ${formatTime(it)}" } ?: "Sin récord todavía",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SudokuBoard(game: GameState, onSelect: (Int) -> Unit) {
    val selectedValue = game.selected?.let { game.cells[it].value } ?: 0
    Column(
        Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .border(2.dp, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(12.dp))
    ) {
        repeat(9) { row ->
            Row(Modifier.weight(1f)) {
                repeat(9) { col ->
                    val index = row * 9 + col
                    val cell = game.cells[index]
                    val selected = game.selected == index
                    val related = game.selected?.let { s ->
                        val sr = s / 9; val sc = s % 9
                        sr == row || sc == col || (sr / 3 == row / 3 && sc / 3 == col / 3)
                    } ?: false
                    val sameValue = selectedValue != 0 && cell.value == selectedValue
                    val background = when {
                        selected -> MaterialTheme.colorScheme.primaryContainer
                        sameValue -> MaterialTheme.colorScheme.secondaryContainer
                        related -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                        else -> Color.Transparent
                    }
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(background)
                            .border(
                                width = if (col % 3 == 2 && col != 8) 1.5.dp else 0.35.dp,
                                color = MaterialTheme.colorScheme.outline
                            )
                            .clickable { onSelect(index) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (cell.value != 0) {
                            Text(
                                cell.value.toString(),
                                fontSize = 24.sp,
                                fontWeight = if (cell.fixed) FontWeight.Bold else FontWeight.Medium,
                                color = when {
                                    cell.error -> MaterialTheme.colorScheme.error
                                    cell.fixed -> MaterialTheme.colorScheme.onSurface
                                    else -> MaterialTheme.colorScheme.primary
                                }
                            )
                        } else if (cell.notes.isNotEmpty()) {
                            Text(
                                (1..9).joinToString("") { if (it in cell.notes) it.toString() else "·" },
                                fontSize = 10.sp,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            if (row % 3 == 2 && row != 8) {
                HorizontalDivider(thickness = 1.5.dp, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun ActionRow(game: GameState, vm: GameViewModel) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        SmallAction(Icons.Rounded.Undo, "Borrar", vm::erase)
        SmallAction(
            if (game.notesMode) Icons.Rounded.EditNote else Icons.Rounded.Edit,
            if (game.notesMode) "Notas ON" else "Notas",
            vm::toggleNotes,
            active = game.notesMode
        )
        SmallAction(Icons.Rounded.Lightbulb, "Pista", vm::hint)
        SmallAction(Icons.Rounded.Pause, "Pausa", vm::togglePause)
    }
}

@Composable
private fun SmallAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit, active: Boolean = false) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FilledTonalIconButton(onClick = onClick, colors = if (active) IconButtonDefaults.filledTonalIconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer) else IconButtonDefaults.filledTonalIconButtonColors()) {
            Icon(icon, contentDescription = label)
        }
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun NumberPad(game: GameState, onNumber: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        (1..9).forEach { n ->
            val completedCount = game.cells.count { it.value == n }
            OutlinedButton(
                onClick = { onNumber(n) },
                enabled = completedCount < 9,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(0.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Text(n.toString(), fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

private fun formatTime(totalSeconds: Long): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
