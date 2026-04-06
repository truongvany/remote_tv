package com.example.remote_tv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.remote_tv.ui.theme.ButtonBackground
import com.example.remote_tv.ui.theme.OrangeAccent
import com.example.remote_tv.ui.theme.TextSecondary

@Composable
fun ControlButtons(onCommand: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            RemoteIconButton(Icons.AutoMirrored.Filled.Undo, "BACK") { onCommand("KEY_BACK") }
            RemoteIconButton(Icons.Filled.Home, "HOME") { onCommand("KEY_HOME") }
            RemoteIconButton(Icons.Filled.Menu, "MENU") { onCommand("KEY_MENU") }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            RemoteIconButton(Icons.Filled.Search, "SEARCH") { onCommand("KEY_SEARCH") }

            VoiceButton { onCommand("KEY_VOICE") }

            RemoteIconButton(Icons.AutoMirrored.Filled.VolumeOff, "MUTE") { onCommand("KEY_MUTE") }
        }
    }
}

@Composable
fun RemoteIconButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(84.dp)
                .background(ButtonBackground, RoundedCornerShape(24.dp))
                .border(1.dp, Color(0xFF262626), RoundedCornerShape(24.dp))
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = Color(0xFFA8A8A8),
                modifier = Modifier.size(34.dp)
            )
        }
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}

@Composable
fun VoiceButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(92.dp)
            .shadow(18.dp, CircleShape)
            .background(OrangeAccent, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Filled.Mic,
            contentDescription = "Voice",
            tint = Color.Black,
            modifier = Modifier.size(40.dp)
        )
    }
}
