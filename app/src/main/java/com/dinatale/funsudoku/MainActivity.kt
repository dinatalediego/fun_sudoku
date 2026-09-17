package com.dinatale.funsudoku

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import java.text.DateFormat
import java.util.Date

class MainActivity : ComponentActivity() {
    private val gameViewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CloudBackend.handleDeepLink(intent)
        gameViewModel.refreshCloudAccount()
        setContent { FunSudokuTheme { SudokuApp(gameViewModel) } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        CloudBackend.handleDeepLink(intent)
        gameViewModel.refreshCloudAccount()
    }
}

private enum class AppTab(val label: String) {
    PLAY("Jugar"),
    GAMES("Mis juegos"),
    PROFILE("Perfil")
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SudokuApp(vm: GameViewModel) {
    val game = vm.state
    var tab by remember { mutableStateOf(AppTab.PLAY) }
    var showDifficulty by remember { mutableStateOf(false) }
    val (wins, best) = vm.stats()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        when (tab) {
                            AppTab.PLAY -> "FUN SUDOKU"
                            AppTab.GAMES -> "MIS JUEGOS"
                            AppTab.PROFILE -> "PERFIL"
                        },
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                },
                actions = {
                    if (tab == AppTab.PLAY) {
                        IconButton(onClick = { showDifficulty = true }) {
                            Icon(Icons.Rounded.Add, contentDescription = "Nueva partida")
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == AppTab.PLAY,
                    onClick = { tab = AppTab.PLAY },
                    icon = { Icon(Icons.Rounded.SportsEsports, null) },
                    label = { Text(AppTab.PLAY.label) }
                )
                NavigationBarItem(
                    selected = tab == AppTab.GAMES,
                    onClick = { tab = AppTab.GAMES },
                    icon = { Icon(Icons.Rounded.QueryStats, null) },
                    label = { Text(AppTab.GAMES.label) }
                )
                NavigationBarItem(
                    selected = tab == AppTab.PROFILE,
                    onClick = { tab = AppTab.PROFILE },
                    icon = { Icon(Icons.Rounded.Person, null) },
                    label = { Text(AppTab.PROFILE.label) }
                )
            }
        }
    ) { padding ->
        when (tab) {
            AppTab.PLAY -> PlayScreen(
                modifier = Modifier.padding(padding),
                game = game,
                wins = wins,
                best = best,
                vm = vm
            )
            AppTab.GAMES -> MyGamesScreen(
                modifier = Modifier.padding(padding),
                vm = vm
            )
            AppTab.PROFILE -> ProfileScreen(
                modifier = Modifier.padding(padding),
                vm = vm
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
                            onClick = {
                                vm.start(difficulty)
                                showDifficulty = false
                                tab = AppTab.PLAY
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(difficulty.label) }
                    }
                }
            },
            confirmButton = {}
        )
    }

    if (game.completed && tab == AppTab.PLAY) {
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
                TextButton(onClick = { tab = AppTab.GAMES }) { Text("Ver estadísticas") }
            }
        )
    }

    if (game.paused && tab == AppTab.PLAY) {
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
private fun PlayScreen(
    modifier: Modifier,
    game: GameState,
    wins: Int,
    best: Long?,
    vm: GameViewModel
) {
    Column(
        modifier = modifier
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

@Composable
private fun MyGamesScreen(modifier: Modifier, vm: GameViewModel) {
    val stats = vm.playerStats()
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Tu progreso", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                if (vm.accountEmail != null) {
                    "Tus resultados se guardan localmente y se sincronizan con ${vm.accountEmail}."
                } else {
                    "Tus resultados se guardan en este teléfono incluso sin internet."
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("Partidas", stats.totalGames.toString(), Modifier.weight(1f))
                StatCard("Promedio", stats.averageSeconds?.let(::formatTime) ?: "—", Modifier.weight(1f))
                StatCard("Perfectas", stats.perfectGames.toString(), Modifier.weight(1f))
            }
        }

        item {
            Text("Récords", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Difficulty.entries.forEach { difficulty ->
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Row(
                            Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(difficulty.label, fontWeight = FontWeight.SemiBold)
                                val games = vm.history.count { it.difficulty == difficulty }
                                Text("$games partidas", style = MaterialTheme.typography.labelMedium)
                            }
                            Text(
                                stats.bestByDifficulty[difficulty]?.let(::formatTime) ?: "Sin récord",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(4.dp))
            Text("Historial reciente", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        if (vm.history.isEmpty()) {
            item {
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.fillMaxWidth().padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Rounded.SportsEsports, null, modifier = Modifier.size(42.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("Tu primera partida aparecerá aquí", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        } else {
            items(vm.history, key = { it.id }) { record ->
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(record.difficulty.label, fontWeight = FontWeight.Bold)
                            Text(
                                DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                                    .format(Date(record.completedAtMillis)),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(formatTime(record.elapsedSeconds), fontWeight = FontWeight.Bold)
                            Text(
                                "${record.mistakes} errores · ${record.hintsUsed} pistas",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    ElevatedCard(modifier) {
        Column(Modifier.padding(14.dp)) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ProfileScreen(modifier: Modifier, vm: GameViewModel) {
    Column(
        modifier = modifier.fillMaxSize().padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.AccountCircle, null, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            vm.accountEmail ?: "Perfil local",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${vm.history.size} partidas guardadas",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider()

                if (vm.accountEmail != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.CloudDone, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text(vm.cloudMessage, style = MaterialTheme.typography.bodyMedium)
                    }
                    Button(onClick = vm::syncCloud, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Rounded.Sync, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Sincronizar ahora")
                    }
                    TextButton(onClick = vm::signOut, modifier = Modifier.fillMaxWidth()) {
                        Text("Cerrar sesión")
                    }
                } else if (vm.cloudConfigured) {
                    Text(
                        "Inicia sesión para conservar tus partidas, estadísticas y récords al cambiar de teléfono.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(onClick = vm::signInGoogle, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Rounded.Login, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Continuar con Google")
                    }
                    Text(
                        vm.cloudMessage,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        "El juego ya conserva tus estadísticas sin conexión. La cuenta Google se activará al conectar el proyecto Supabase dedicado de Fun Sudoku.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    AssistChip(
                        onClick = {},
                        enabled = false,
                        label = { Text("Google Sync pendiente de configurar") },
                        leadingIcon = { Icon(Icons.Rounded.CloudSync, null) }
                    )
                }
            }
        }

        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Offline-first", fontWeight = FontWeight.Bold)
                Text(
                    "Puedes jugar sin señal. Cuando el login esté activo, la nube sincronizará el historial sin convertir Internet en requisito para jugar.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
                        val sr = s / 9
                        val sc = s % 9
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
private fun SmallAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    active: Boolean = false
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FilledTonalIconButton(
            onClick = onClick,
            colors = if (active) {
                IconButtonDefaults.filledTonalIconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            } else {
                IconButtonDefaults.filledTonalIconButtonColors()
            }
        ) {
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
