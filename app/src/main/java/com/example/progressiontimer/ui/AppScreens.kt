@file:OptIn(
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class,
)

package com.example.progressiontimer.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.progressiontimer.data.EventFolder
import com.example.progressiontimer.data.TimeRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

@Composable
fun MainAppScreen(viewModel: TimerViewModel) {
    val navController = rememberNavController()
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val allRecords by viewModel.allRecords.collectAsStateWithLifecycle()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    LaunchedEffect(folders) {
        viewModel.ensureFolderSelected(folders)
    }

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                NavigationBarItem(
                    selected = currentRoute == Routes.Timer,
                    onClick = { navController.navigateTopLevel(Routes.Timer) },
                    icon = { Text("T") },
                    label = { Text("Timer") },
                )
                NavigationBarItem(
                    selected = currentRoute == Routes.Folders || currentRoute == Routes.Stats,
                    onClick = { navController.navigateTopLevel(Routes.Folders) },
                    icon = { Text("P") },
                    label = { Text("Progress") },
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.Timer,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Routes.Timer) {
                TimerScreen(
                    folders = folders,
                    allRecords = allRecords,
                    viewModel = viewModel,
                )
            }
            composable(Routes.Folders) {
                FoldersScreen(
                    folders = folders,
                    allRecords = allRecords,
                    onOpenStats = { folderId -> navController.navigate("stats/$folderId") },
                    onCreateFolder = viewModel::createFolder,
                    onDeleteFolder = viewModel::deleteFolder,
                    onMoveFolderUp = viewModel::moveFolderUp,
                    onMoveFolderDown = viewModel::moveFolderDown,
                )
            }
            composable(
                route = Routes.Stats,
                arguments = listOf(navArgument("folderId") { type = NavType.IntType }),
            ) { entry ->
                val folderId = entry.arguments?.getInt("folderId") ?: return@composable
                StatsRoute(
                    folderId = folderId,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}

@Composable
private fun TimerScreen(
    folders: List<EventFolder>,
    allRecords: List<TimeRecord>,
    viewModel: TimerViewModel,
) {
    val timeMillis by viewModel.timeMillis.collectAsStateWithLifecycle()
    val isRunning by viewModel.isRunning.collectAsStateWithLifecycle()
    val selectedFolderId by viewModel.selectedFolderId.collectAsStateWithLifecycle()
    val selectedFolder = folders.firstOrNull { it.id == selectedFolderId }
    val selectedRecords = selectedFolder?.let { folder ->
        allRecords.filter { it.folderId == folder.id }
    }.orEmpty()
    val bestRecord = selectedFolder?.let { selectedRecords.bestFor(it) }
    val haptics = LocalHapticFeedback.current
    var showNewEventDialog by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Timer",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = selectedFolder?.name ?: "Choose an activity below",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = formatTime(timeMillis),
            style = MaterialTheme.typography.displayLarge,
            fontSize = 72.sp,
            lineHeight = 76.sp,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = bestRecord?.let { "Best: ${formatTime(it.timeInMillis)}" } ?: "No attempts saved yet.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        selectedFolder?.let { folder ->
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (folder.isHigherBetter) "Longer is better" else "Lower is better",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                if (isRunning) viewModel.stopTimer() else viewModel.startTimer()
            },
            enabled = selectedFolder != null,
            modifier = Modifier.size(122.dp),
            shape = CircleShape,
        ) {
            Text(
                text = if (isRunning) "STOP" else "START",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FilledTonalButton(
                onClick = viewModel::saveCurrentTime,
                enabled = !isRunning && timeMillis > 0L && selectedFolder != null,
                modifier = Modifier.weight(1f),
            ) {
                Text("Save Time")
            }
            OutlinedButton(
                onClick = viewModel::resetTimer,
                enabled = !isRunning && timeMillis > 0L,
                modifier = Modifier.weight(1f),
            ) {
                Text("Reset")
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        HorizontalDivider()

        Spacer(modifier = Modifier.height(18.dp))

        EventPicker(
            folders = folders,
            selectedFolder = selectedFolder,
            onSelectFolder = viewModel::selectFolder,
            onCreateFolder = { showNewEventDialog = true },
        )
    }

    if (showNewEventDialog) {
        NewEventDialog(
            onDismiss = { showNewEventDialog = false },
            onCreate = { name, isHigherBetter ->
                viewModel.createFolder(name, isHigherBetter)
                showNewEventDialog = false
            },
        )
    }
}

@Composable
private fun EventPicker(
    folders: List<EventFolder>,
    selectedFolder: EventFolder?,
    onSelectFolder: (Int) -> Unit,
    onCreateFolder: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Current activity",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.weight(1f)) {
                OutlinedButton(
                    onClick = { expanded = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = selectedFolder?.name ?: "Choose activity",
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Start,
                    )
                    Text("Open")
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    folders.forEach { folder ->
                        DropdownMenuItem(
                            text = { Text(folder.name) },
                            onClick = {
                                onSelectFolder(folder.id)
                                expanded = false
                            },
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("New activity") },
                        onClick = {
                            expanded = false
                            onCreateFolder()
                        },
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            TextButton(onClick = onCreateFolder) {
                Text("New")
            }
        }
    }
}

@Composable
private fun FoldersScreen(
    folders: List<EventFolder>,
    allRecords: List<TimeRecord>,
    onOpenStats: (Int) -> Unit,
    onCreateFolder: (String, Boolean) -> Unit,
    onDeleteFolder: (Int) -> Unit,
    onMoveFolderUp: (Int) -> Unit,
    onMoveFolderDown: (Int) -> Unit,
) {
    var showNewEventDialog by rememberSaveable { mutableStateOf(false) }
    var pendingDeleteFolderId by rememberSaveable { mutableStateOf<Int?>(null) }
    val pendingDeleteFolder = folders.firstOrNull { it.id == pendingDeleteFolderId }
    val pendingDeleteCount = allRecords.count { it.folderId == pendingDeleteFolderId }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(MaterialTheme.colorScheme.background)
            .padding(22.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Activities",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "Open stats, reorder folders, or remove activities you no longer want.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(onClick = { showNewEventDialog = true }) {
                Text("New")
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        if (folders.isEmpty()) {
            EmptyState(
                title = "No activities yet",
                body = "Create one activity, then save timer attempts into it.",
            )
        } else {
            folders.forEachIndexed { index, folder ->
                val records = allRecords.filter { it.folderId == folder.id }
                val bestRecord = records.bestFor(folder)

                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp),
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = folder.name,
                                    style = MaterialTheme.typography.titleLarge,
                                )
                                Text(
                                    text = if (folder.isHigherBetter) "Higher is better" else "Lower is better",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                text = "${records.size}x",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            AssistChip(
                                onClick = { onOpenStats(folder.id) },
                                label = { Text(bestRecord?.let { "Best ${formatTime(it.timeInMillis)}" } ?: "No best yet") },
                            )
                            AssistChip(
                                onClick = { onOpenStats(folder.id) },
                                label = { Text(records.lastOrNull()?.let { "Latest ${formatTime(it.timeInMillis)}" } ?: "No attempts") },
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            FilledTonalButton(onClick = { onOpenStats(folder.id) }) {
                                Text("Stats")
                            }
                            OutlinedButton(
                                onClick = { onMoveFolderUp(folder.id) },
                                enabled = index > 0,
                            ) {
                                Text("Up")
                            }
                            OutlinedButton(
                                onClick = { onMoveFolderDown(folder.id) },
                                enabled = index < folders.lastIndex,
                            ) {
                                Text("Down")
                            }
                            TextButton(onClick = { pendingDeleteFolderId = folder.id }) {
                                Text("Delete")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showNewEventDialog) {
        NewEventDialog(
            onDismiss = { showNewEventDialog = false },
            onCreate = { name, isHigherBetter ->
                onCreateFolder(name, isHigherBetter)
                showNewEventDialog = false
            },
        )
    }

    if (pendingDeleteFolder != null) {
        AlertDialog(
            onDismissRequest = { pendingDeleteFolderId = null },
            title = { Text("Delete activity") },
            text = {
                Text(
                    text = if (pendingDeleteCount > 0) {
                        "Delete ${pendingDeleteFolder.name}? This will also remove $pendingDeleteCount saved attempts."
                    } else {
                        "Delete ${pendingDeleteFolder.name}?"
                    },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteFolder(pendingDeleteFolder.id)
                        pendingDeleteFolderId = null
                    },
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteFolderId = null }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun StatsRoute(
    folderId: Int,
    viewModel: TimerViewModel,
    onBack: () -> Unit,
) {
    val folderFlow = remember(folderId) { viewModel.observeFolder(folderId) }
    val recordsFlow = remember(folderId) { viewModel.observeRecords(folderId) }
    val folder by folderFlow.collectAsStateWithLifecycle(initialValue = null)
    val records by recordsFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    StatsScreen(
        folder = folder,
        records = records,
        onBack = onBack,
        onDeleteRecord = viewModel::deleteRecord,
    )
}

@Composable
private fun StatsScreen(
    folder: EventFolder?,
    records: List<TimeRecord>,
    onBack: () -> Unit,
    onDeleteRecord: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(MaterialTheme.colorScheme.background)
            .padding(22.dp),
    ) {
        TextButton(onClick = onBack) {
            Text("< Back")
        }

        if (folder == null) {
            EmptyState(
                title = "Activity not found",
                body = "This activity may have been removed.",
            )
            return@Column
        }

        Text(
            text = folder.name,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = if (folder.isHigherBetter) "Progress means holding longer." else "Progress means finishing faster.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(20.dp))

        StatsSummary(folder = folder, records = records)

        Spacer(modifier = Modifier.height(20.dp))

        ProgressChart(records = records)

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Attempts",
            style = MaterialTheme.typography.titleLarge,
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (records.isEmpty()) {
            EmptyState(
                title = "No attempts saved",
                body = "Run the timer, save a time, then come back here to see progression.",
            )
        } else {
            records.asReversed().forEachIndexed { index, record ->
                AttemptRow(
                    attemptNumber = records.size - index,
                    record = record,
                    onDelete = { onDeleteRecord(record.id) },
                )
                if (index != records.lastIndex) {
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun StatsSummary(folder: EventFolder, records: List<TimeRecord>) {
    val best = records.bestFor(folder)
    val latest = records.lastOrNull()
    val average = records.map { it.timeInMillis }.takeIf { it.isNotEmpty() }?.average()?.toLong()

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        StatTile(label = "Attempts", value = records.size.toString())
        StatTile(label = "Best", value = best?.let { formatTime(it.timeInMillis) } ?: "--")
        StatTile(label = "Average", value = average?.let(::formatTime) ?: "--")
        StatTile(label = "Latest", value = latest?.let { formatTime(it.timeInMillis) } ?: "--")
    }
}

@Composable
private fun StatTile(label: String, value: String) {
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.width(156.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelLarge,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun ProgressChart(records: List<TimeRecord>) {
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "Progression chart",
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (records.size < 2) {
                EmptyState(
                    title = "Need two attempts",
                    body = "The line chart appears after you save at least two records.",
                )
                return@Column
            }

            val lineColor = MaterialTheme.colorScheme.primary
            val pointColor = MaterialTheme.colorScheme.secondary
            val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(20.dp),
            ) {
                val values = records.map { it.timeInMillis.toFloat() }
                val minValue = values.minOrNull() ?: 0f
                val maxValue = values.maxOrNull() ?: minValue
                val range = max(1f, maxValue - minValue)
                val xStep = size.width / (values.lastIndex.coerceAtLeast(1))
                val points = values.mapIndexed { index, value ->
                    val normalized = (value - minValue) / range
                    Offset(
                        x = index * xStep,
                        y = size.height - (normalized * size.height),
                    )
                }

                repeat(4) { line ->
                    val y = size.height * line / 3f
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.dp.toPx(),
                    )
                }

                val path = Path().apply {
                    points.forEachIndexed { index, point ->
                        if (index == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
                    }
                }

                drawPath(
                    path = path,
                    color = lineColor,
                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round),
                )

                points.forEach { point ->
                    drawCircle(color = pointColor, radius = 5.dp.toPx(), center = point)
                }
            }
        }
    }
}

@Composable
private fun AttemptRow(
    attemptNumber: Int,
    record: TimeRecord,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Attempt $attemptNumber",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = formatDate(record.dateRecorded),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = formatTime(record.timeInMillis),
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.width(10.dp))
        TextButton(onClick = onDelete) {
            Text("Delete")
        }
    }
}

@Composable
private fun EmptyState(title: String, body: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = body,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun NewEventDialog(
    onDismiss: () -> Unit,
    onCreate: (String, Boolean) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var isHigherBetter by rememberSaveable { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New activity") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text("Activity name") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "What counts as progress?",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    FilterChip(
                        selected = isHigherBetter,
                        onClick = { isHigherBetter = true },
                        label = { Text("Higher time") },
                    )
                    FilterChip(
                        selected = !isHigherBetter,
                        onClick = { isHigherBetter = false },
                        label = { Text("Lower time") },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = { onCreate(name, isHigherBetter) },
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

private fun NavHostController.navigateTopLevel(route: String) {
    navigate(route) {
        launchSingleTop = true
        restoreState = true
        popUpTo(graph.startDestinationId) {
            saveState = true
        }
    }
}

private fun List<TimeRecord>.bestFor(folder: EventFolder): TimeRecord? {
    return if (folder.isHigherBetter) {
        maxByOrNull { it.timeInMillis }
    } else {
        minByOrNull { it.timeInMillis }
    }
}

fun formatTime(millis: Long): String {
    val totalSeconds = millis / 1_000L
    val hours = totalSeconds / 3_600L
    val minutes = (totalSeconds / 60L) % 60L
    val seconds = totalSeconds % 60L
    val centiseconds = (millis % 1_000L) / 10L

    return if (hours > 0L) {
        String.format(Locale.getDefault(), "%d:%02d:%02d.%02d", hours, minutes, seconds, centiseconds)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d.%02d", minutes, seconds, centiseconds)
    }
}

private fun formatDate(epochMillis: Long): String {
    return SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault()).format(Date(epochMillis))
}

private object Routes {
    const val Timer = "timer"
    const val Folders = "folders"
    const val Stats = "stats/{folderId}"
}
