package com.example.remote_tv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.remote_tv.ui.theme.ButtonBackground

@Composable
fun Touchpad(
    onSwipeUp: () -> Unit = {},
    onSwipeDown: () -> Unit = {},
    onSwipeLeft: () -> Unit = {},
    onSwipeRight: () -> Unit = {},
    onTap: () -> Unit = {}
) {
    // TODO: Implement real gesture detection with pointerInput
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.2f)
            .background(ButtonBackground, RoundedCornerShape(28.dp))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "Swipe to navigate\nTap to Select",
            color = Color.Gray,
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )
        Row(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("RETURN", color = Color.Gray, fontSize = 12.sp)
            Box(modifier = Modifier.width(1.dp).height(16.dp).background(Color.DarkGray))
            Text("EXIT", color = Color.Gray, fontSize = 12.sp)
        }
    }
}

