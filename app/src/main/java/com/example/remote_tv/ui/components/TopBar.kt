package com.example.remote_tv.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.remote_tv.ui.theme.OrangeAccent

@Composable
fun TopBar(
    deviceName: String,
    onPower: () -> Unit,
    onCastClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPower) {
            Icon(
                Icons.Filled.PowerSettingsNew,
                contentDescription = "Power",
                tint = OrangeAccent,
                modifier = Modifier.size(32.dp)
            )
        }

        Text(
            text = deviceName.uppercase(),
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.5.sp
            ),
            color = Color.White,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            fontSize = 18.sp
        )

        IconButton(onClick = onCastClick) {
            Icon(
                Icons.Filled.Cast,
                contentDescription = "Connect",
                tint = Color(0xFFADAAAA),
                modifier = Modifier.size(32.dp)
            )
        }
    }
}
