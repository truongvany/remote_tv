package com.example.remote_tv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.remote_tv.ui.theme.ButtonBackground
import kotlin.math.abs

@Composable
fun Touchpad(
    onSwipeUp: () -> Unit = {},
    onSwipeDown: () -> Unit = {},
    onSwipeLeft: () -> Unit = {},
    onSwipeRight: () -> Unit = {},
    onTap: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.2f)
            .background(ButtonBackground, RoundedCornerShape(28.dp))
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onTap() })
            }
            .pointerInput(Unit) {
                var totalDrag = androidx.compose.ui.geometry.Offset.Zero
                detectDragGestures(
                    onDragStart = { totalDrag = androidx.compose.ui.geometry.Offset.Zero },
                    onDragEnd = {
                        val x = totalDrag.x
                        val y = totalDrag.y
                        if (abs(x) > abs(y)) {
                            if (x > 50) onSwipeRight() else if (x < -50) onSwipeLeft()
                        } else {
                            if (y > 50) onSwipeDown() else if (y < -50) onSwipeUp()
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        totalDrag += dragAmount
                    }
                )
            }
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "Vuốt để điều hướng (Up/Down/Left/Right)\nChạm 1 lần để Chọn (OK)",
            color = Color.Gray,
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )
    }
}

