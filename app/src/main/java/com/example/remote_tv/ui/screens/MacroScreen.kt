package com.example.remote_tv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.remote_tv.data.model.Macro
import com.example.remote_tv.ui.viewmodel.TVViewModel
import java.util.UUID

@Composable
fun MacroScreen(viewModel: TVViewModel) {
    val macros by viewModel.macros.collectAsState()
    val currentDevice by viewModel.currentDevice.collectAsState()
    val macroRunState by viewModel.macroRunState.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var editingMacro by remember { mutableStateOf<Macro?>(null) }
    var pendingDeleteMacro by remember { mutableStateOf<Macro?>(null) }

    val isMacroRunning = macroRunState.runningMacroId != null
    val presets = remember { builtInPresets() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "MACRO KEYS",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "One-tap command sequences",
                    color = MaterialTheme.colorScheme.onTertiary,
                    fontSize = 12.sp
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (isMacroRunning) {
                    OutlinedButton(
                        onClick = viewModel::stopMacroExecution,
                        enabled = !macroRunState.isCancelling,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                    ) {
                        Icon(Icons.Filled.StopCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(
                            text = if (macroRunState.isCancelling) "Stopping" else "Stop",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                        )
                    }
                }

                IconButton(
                    onClick = { showCreateDialog = true },
                    modifier = Modifier
                        .size(44.dp)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add Macro", tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }

        if (currentDevice == null) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Connect a TV to run macros",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                fontSize = 11.sp,
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "PRESETS",
            color = MaterialTheme.colorScheme.onTertiary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(12.dp))

        presets.forEach { macro ->
            MacroCard(
                macro = macro,
                isEnabled = currentDevice != null && !isMacroRunning,
                isBuiltIn = true,
                isRunning = macroRunState.runningMacroId == macro.id,
                runningStep = macroRunState.currentStep,
                runningTotal = macroRunState.totalSteps,
                runningCommand = macroRunState.currentCommand,
                onRun = { viewModel.executeMacro(macro) },
                onDuplicate = { viewModel.duplicateMacro(macro) },
                onEdit = null,
                onDelete = null,
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "MY MACROS",
            color = MaterialTheme.colorScheme.onTertiary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(12.dp))

        if (macros.isNotEmpty()) {
            macros.forEach { macro ->
                MacroCard(
                    macro = macro,
                    isEnabled = currentDevice != null && !isMacroRunning,
                    isBuiltIn = false,
                    isRunning = macroRunState.runningMacroId == macro.id,
                    runningStep = macroRunState.currentStep,
                    runningTotal = macroRunState.totalSteps,
                    runningCommand = macroRunState.currentCommand,
                    onRun = { viewModel.executeMacro(macro) },
                    onDuplicate = { viewModel.duplicateMacro(macro) },
                    onEdit = {
                        if (!isMacroRunning) {
                            editingMacro = macro
                        }
                    },
                    onDelete = {
                        if (!isMacroRunning) {
                            pendingDeleteMacro = macro
                        }
                    },
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        } else {
            EmptyMacrosHint()
        }

        Spacer(modifier = Modifier.height(120.dp))
    }

    if (showCreateDialog) {
        MacroEditorDialog(
            title = "Create Macro",
            confirmLabel = "Create",
            initialName = "",
            initialCommands = "KEY_HOME\nKEY_SEARCH\nTEXT:Netflix\nKEY_ENTER",
            initialDelayMs = 350L,
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, commands, delay ->
                viewModel.addMacro(
                    Macro(
                        id = UUID.randomUUID().toString(),
                        name = name,
                        commands = commands,
                        delayMs = delay,
                    )
                )
                showCreateDialog = false
            },
        )
    }

    val editing = editingMacro
    if (editing != null) {
        MacroEditorDialog(
            title = "Edit Macro",
            confirmLabel = "Save",
            initialName = editing.name,
            initialCommands = editing.commands.joinToString("\n"),
            initialDelayMs = editing.delayMs,
            onDismiss = { editingMacro = null },
            onConfirm = { name, commands, delay ->
                viewModel.updateMacro(
                    editing.copy(
                        name = name,
                        commands = commands,
                        delayMs = delay,
                    )
                )
                editingMacro = null
            },
        )
    }

    val deleting = pendingDeleteMacro
    if (deleting != null) {
        AlertDialog(
            onDismissRequest = { pendingDeleteMacro = null },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    text = "Delete Macro",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Text(
                    text = "Delete '${deleting.name}'? This action cannot be undone.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.removeMacro(deleting.id)
                        pendingDeleteMacro = null
                    },
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteMacro = null }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.primary)
                }
            },
        )
    }
}

@Composable
private fun MacroCard(
    macro: Macro,
    isEnabled: Boolean,
    isBuiltIn: Boolean,
    isRunning: Boolean,
    runningStep: Int,
    runningTotal: Int,
    runningCommand: String?,
    onRun: () -> Unit,
    onDuplicate: (() -> Unit)?,
    onEdit: (() -> Unit)?,
    onDelete: (() -> Unit)?,
) {
    val progress = if (isRunning && runningTotal > 0) runningStep.toFloat() / runningTotal.toFloat() else 0f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.tertiary)
            .border(
                1.dp,
                if (isRunning) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                RoundedCornerShape(20.dp)
            )
            .clickable(enabled = isEnabled) { onRun() }
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isRunning) Icons.Filled.StopCircle else Icons.Filled.PlayCircle,
                        contentDescription = null,
                        tint = if (isEnabled || isRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = macro.name,
                        color = if (isEnabled || isRunning) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onTertiary.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "${macro.commands.size} commands · ${macro.delayMs}ms delay",
                        color = MaterialTheme.colorScheme.onTertiary.copy(alpha = 0.62f),
                        fontSize = 11.sp
                    )
                    Text(
                        text = macro.commands.take(4).joinToString(" -> "),
                        color = MaterialTheme.colorScheme.onTertiary.copy(alpha = 0.38f),
                        fontSize = 10.sp,
                        maxLines = 1
                    )
                }

                if (onDuplicate != null) {
                    IconButton(onClick = onDuplicate) {
                        Icon(
                            Icons.Filled.ContentCopy,
                            contentDescription = if (isBuiltIn) "Save preset" else "Duplicate",
                            tint = MaterialTheme.colorScheme.onTertiary.copy(alpha = 0.5f)
                        )
                    }
                }

                if (onEdit != null) {
                    IconButton(onClick = onEdit) {
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.onTertiary.copy(alpha = 0.5f)
                        )
                    }
                }

                if (onDelete != null) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Filled.DeleteOutline,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.onTertiary.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            if (isRunning) {
                LinearProgressIndicator(
                    progress = { progress },
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(6.dp))
                )
                Text(
                    text = "Step $runningStep/$runningTotal${runningCommand?.let { " · $it" } ?: ""}",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun MacroEditorDialog(
    title: String,
    confirmLabel: String,
    initialName: String,
    initialCommands: String,
    initialDelayMs: Long,
    onDismiss: () -> Unit,
    onConfirm: (name: String, commands: List<String>, delayMs: Long) -> Unit,
) {
    var macroName by remember(initialName) { mutableStateOf(initialName) }
    var commandsText by remember(initialCommands) { mutableStateOf(initialCommands) }
    var delayMsText by remember(initialDelayMs) { mutableStateOf(initialDelayMs.toString()) }

    val parsedCommands = parseMacroCommands(commandsText)
    val parsedDelay = delayMsText.toLongOrNull()?.coerceIn(50L, 5_000L)
    val isValid = macroName.trim().isNotBlank() && parsedCommands.isNotEmpty() && parsedDelay != null

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = macroName,
                    onValueChange = { macroName = it },
                    label = { Text("Macro name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Commands (one per line)",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                )

                OutlinedTextField(
                    value = commandsText,
                    onValueChange = { commandsText = it },
                    minLines = 4,
                    maxLines = 8,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Examples: KEY_HOME, KEY_BACK, KEY_VOL_UP, TEXT:hello, SEARCH_QUERY:Netflix",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                    fontSize = 10.sp,
                )

                OutlinedTextField(
                    value = delayMsText,
                    onValueChange = { delayMsText = it.filter { c -> c.isDigit() } },
                    label = { Text("Delay between commands (50-5000 ms)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "${parsedCommands.size} command(s) ready",
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val delayValue = parsedDelay ?: 350L
                    onConfirm(macroName.trim(), parsedCommands, delayValue)
                },
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(confirmLabel, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.primary)
            }
        }
    )
}

private fun parseMacroCommands(rawCommands: String): List<String> {
    return rawCommands
        .lines()
        .map { it.trim() }
        .filter { it.isNotBlank() }
}

@Composable
private fun EmptyMacrosHint() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.secondary)
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), RoundedCornerShape(20.dp))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Filled.TouchApp,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "No macros yet",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Tap + to create a command sequence",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                fontSize = 12.sp
            )
        }
    }
}

private fun builtInPresets(): List<Macro> = listOf(
    Macro(
        id = "preset_netflix",
        name = "Open Netflix",
        commands = listOf("KEY_HOME", "KEY_SEARCH", "TEXT:Netflix", "KEY_ENTER"),
        delayMs = 400,
    ),
    Macro(
        id = "preset_youtube",
        name = "Open YouTube",
        commands = listOf("KEY_HOME", "KEY_SEARCH", "TEXT:YouTube", "KEY_ENTER"),
        delayMs = 400,
    ),
    Macro(
        id = "preset_sleep",
        name = "Sleep Timer (5 min)",
        commands = listOf("KEY_MENU", "KEY_RIGHT", "KEY_RIGHT", "KEY_RIGHT", "OK"),
        delayMs = 500,
    ),
    Macro(
        id = "preset_vol_up5",
        name = "Volume +5",
        commands = listOf("KEY_VOL_UP", "KEY_VOL_UP", "KEY_VOL_UP", "KEY_VOL_UP", "KEY_VOL_UP"),
        delayMs = 200,
    ),
)
