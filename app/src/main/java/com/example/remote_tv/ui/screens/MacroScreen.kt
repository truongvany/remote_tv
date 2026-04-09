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
import com.example.remote_tv.ui.theme.OrangeAccent
import com.example.remote_tv.ui.theme.CardBackground
import com.example.remote_tv.ui.theme.TextSecondary
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
                    color = OrangeAccent,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "One-tap command sequences",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
            IconButton(
                onClick = { showCreateDialog = true },
                modifier = Modifier
                    .size(44.dp)
                    .background(OrangeAccent, RoundedCornerShape(12.dp))
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Macro", tint = Color.Black)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Built-in presets
        Text(
            "PRESETS",
            color = TextSecondary,
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
                color = TextSecondary,
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

// ----------------------------------------------------------------
// MacroCard
// ----------------------------------------------------------------

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
            .background(CardBackground)
            .border(1.dp, Color(0xFF2A2A2A), RoundedCornerShape(20.dp))
            .clickable(enabled = isEnabled) { onRun() }
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Icon circle
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(OrangeAccent.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayCircle,
                    contentDescription = null,
                    tint = if (isEnabled) OrangeAccent else Color.Gray,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = macro.name,
                    color = if (isEnabled) Color.White else Color.Gray,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = "${macro.commands.size} commands · ${macro.delayMs}ms delay",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
                // Preview lệnh
                Text(
                    text = macro.commands.take(3).joinToString(" → "),
                    color = Color(0xFF555555),
                    fontSize = 10.sp,
                    maxLines = 1
                )
            }
            if (!isBuiltIn) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete", tint = Color(0xFF555555))
                }
            }
        }
    }
}

// ----------------------------------------------------------------
// Create Macro Dialog
// ----------------------------------------------------------------

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
        containerColor = Color(0xFF121212),
        title = {
            Text("Create Macro", color = Color.White, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = macroName,
                    onValueChange = { macroName = it },
                    label = { Text("Macro name", color = TextSecondary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OrangeAccent,
                        unfocusedBorderColor = Color(0xFF333333),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "Commands (one per line):",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                OutlinedTextField(
                    value = commandsText,
                    onValueChange = { commandsText = it },
                    minLines = 4,
                    maxLines = 8,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OrangeAccent,
                        unfocusedBorderColor = Color(0xFF333333),
                        focusedTextColor = Color(0xFFE0E0E0),
                        unfocusedTextColor = Color(0xFFB0B0B0),
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "Examples: KEY_HOME, KEY_BACK, KEY_VOL_UP, TEXT:hello, KEY_ENTER",
                    color = Color(0xFF555555),
                    fontSize = 10.sp
                )
                OutlinedTextField(
                    value = delayMs,
                    onValueChange = { delayMs = it.filter { c -> c.isDigit() } },
                    label = { Text("Delay between commands (ms)", color = TextSecondary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OrangeAccent,
                        unfocusedBorderColor = Color(0xFF333333),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
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
                colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent)
            ) {
                Text("Create", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

// ----------------------------------------------------------------
// Empty State
// ----------------------------------------------------------------

@Composable
private fun EmptyMacrosHint() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF0C0C0C))
            .border(1.dp, Color(0xFF1F1F1F), RoundedCornerShape(20.dp))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.TouchApp, contentDescription = null, tint = Color(0xFF333333), modifier = Modifier.size(36.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "No macros yet",
                color = Color(0xFF555555),
                fontWeight = FontWeight.Bold
            )
            Text(
                "Tap + to create a command sequence",
                color = Color(0xFF333333),
                fontSize = 12.sp
            )
        }
    }
}

// ----------------------------------------------------------------
// Built-in Presets
// ----------------------------------------------------------------

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
