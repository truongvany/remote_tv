package com.example.remote_tv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.remote_tv.ui.theme.ButtonBackground
import com.example.remote_tv.ui.theme.OrangeAccent

@Composable
fun KeyboardInput() {
    // TODO: Implement real keyboard input forwarding
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ButtonBackground, RoundedCornerShape(28.dp))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Filled.Keyboard, contentDescription = null, tint = OrangeAccent, modifier = Modifier.size(48.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text("Use phone keyboard to type", color = Color.White)
    }
}

