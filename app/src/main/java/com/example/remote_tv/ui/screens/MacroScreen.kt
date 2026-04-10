package com.example.remote_tv.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
    var showCreateDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Header
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
            IconButton(
                onClick = { showCreateDialog = true },
                modifier = Modifier
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Macro", tint = MaterialTheme.colorScheme.onPrimary)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Built-in presets
        Text(
            "PRESETS",
            color = MaterialTheme.colorScheme.onTertiary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(12.dp))

        val presets = remember { builtInPresets() }
        presets.forEach { macro ->
            MacroCard(
                macro = macro,
                isEnabled = currentDevice != null,
                isBuiltIn = true,
                onRun = { viewModel.executeMacro(macro) },
                onDelete = {}
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))

        // User macros
        if (macros.isNotEmpty()) {
            Text(
                "MY MACROS",
                color = MaterialTheme.colorScheme.onTertiary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            macros.forEach { macro ->
                MacroCard(
                    macro = macro,
                    isEnabled = currentDevice != null,
                    isBuiltIn = false,
                    onRun = { viewModel.executeMacro(macro) },
                    onDelete = { viewModel.removeMacro(macro.id) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        } else {
            EmptyMacrosHint()
        }

        Spacer(modifier = Modifier.height(120.dp))
    }

    if (showCreateDialog) {
        CreateMacroDialog(
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
            }
        )
    }
}

@Composable
private fun MacroCard(
    macro: Macro,
    isEnabled: Boolean,
    isBuiltIn: Boolean,
    onRun: () -> Unit,
    onDelete: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.tertiary)
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), RoundedCornerShape(20.dp))
            .clickable(enabled = isEnabled) { onRun() }
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Icon circle
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayCircle,
                    contentDescription = null,
                    tint = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = macro.name,
                    color = if (isEnabled) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onTertiary.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = "${macro.commands.size} commands · ${macro.delayMs}ms delay",
                    color = MaterialTheme.colorScheme.onTertiary.copy(alpha = 0.6f),
                    fontSize = 11.sp
                )
                Text(
                    text = macro.commands.take(3).joinToString(" → "),
                    color = MaterialTheme.colorScheme.onTertiary.copy(alpha = 0.3f),
                    fontSize = 10.sp,
                    maxLines = 1
                )
            }
            if (!isBuiltIn) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onTertiary.copy(alpha = 0.3f))
                }
            }
        }
    }
}

@Composable
private fun CreateMacroDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, commands: List<String>, delayMs: Long) -> Unit,
) {
    var macroName by remember { mutableStateOf("") }
    var commandsText by remember { mutableStateOf("KEY_HOME\nKEY_SEARCH\nTEXT:Netflix\nKEY_ENTER") }
    var delayMs by remember { mutableStateOf("350") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text("Create Macro", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = macroName,
                    onValueChange = { macroName = it },
                    label = { Text("Macro name") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "Commands (one per line):",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
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
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "Examples: KEY_HOME, KEY_BACK, KEY_VOL_UP, TEXT:hello, KEY_ENTER",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    fontSize = 10.sp
                )
                OutlinedTextField(
                    value = delayMs,
                    onValueChange = { delayMs = it.filter { c -> c.isDigit() } },
                    label = { Text("Delay between commands (ms)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val commands = commandsText.lines()
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                    val delay = delayMs.toLongOrNull() ?: 350L
                    if (macroName.isNotBlank() && commands.isNotEmpty()) {
                        onConfirm(macroName.trim(), commands, delay)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Create", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.primary)
            }
        }
    )
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
            Icon(Icons.Filled.TouchApp, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), modifier = Modifier.size(36.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "No macros yet",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                fontWeight = FontWeight.Bold
            )
            Text(
                "Tap + to create a command sequence",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
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
        commands = listOf(
            "KEY_VOL_UP", "KEY_VOL_UP", "KEY_VOL_UP", "KEY_VOL_UP", "KEY_VOL_UP"
        ),
        delayMs = 200,
    ),
)
