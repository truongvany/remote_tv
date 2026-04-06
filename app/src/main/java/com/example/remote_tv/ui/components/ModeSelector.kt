package com.example.remote_tv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.remote_tv.ui.theme.ButtonBackground
import com.example.remote_tv.ui.theme.OrangeAccent

@Composable
fun ModeSelector(selectedMode: Int, onModeSelected: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .background(ButtonBackground, RoundedCornerShape(14.dp))
            .padding(4.dp)
    ) {
        ModeButton(Icons.Filled.GridView,  isSelected = selectedMode == 0) { onModeSelected(0) }
        ModeButton(Icons.Filled.TouchApp,  isSelected = selectedMode == 1) { onModeSelected(1) }
        ModeButton(Icons.Filled.Keyboard,  isSelected = selectedMode == 2) { onModeSelected(2) }
    }
}

@Composable
fun ModeButton(icon: ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) OrangeAccent else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
    }
}

