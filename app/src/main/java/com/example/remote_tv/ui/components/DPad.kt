package com.example.remote_tv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DPad(onDirection: (String) -> Unit, onOk: () -> Unit) {
    Box(
        modifier = Modifier
            .size(280.dp)
            .background(MaterialTheme.colorScheme.tertiary, CircleShape)
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .shadow(10.dp, CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        )
                    ),
                    shape = CircleShape
                )
        ) {
            DirectionIcon(Icons.Filled.KeyboardArrowUp, Alignment.TopCenter, Modifier.padding(top = 18.dp))
            DirectionIcon(Icons.Filled.KeyboardArrowDown, Alignment.BottomCenter, Modifier.padding(bottom = 18.dp))
            DirectionIcon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, Alignment.CenterStart, Modifier.padding(start = 18.dp))
            DirectionIcon(Icons.AutoMirrored.Filled.KeyboardArrowRight, Alignment.CenterEnd, Modifier.padding(end = 18.dp))

            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.38f)
                        .align(Alignment.TopCenter)
                        .clickable { onDirection("UP") }
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.38f)
                        .align(Alignment.BottomCenter)
                        .clickable { onDirection("DOWN") }
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.38f)
                        .fillMaxHeight()
                        .align(Alignment.CenterStart)
                        .clickable { onDirection("LEFT") }
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.38f)
                        .fillMaxHeight()
                        .align(Alignment.CenterEnd)
                        .clickable { onDirection("RIGHT") }
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(96.dp)
                    .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f), CircleShape)
                    .clickable { onOk() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "OK",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp
                )
            }
        }
    }
}

@Composable
private fun BoxScope.DirectionIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    alignment: Alignment,
    modifier: Modifier = Modifier
) {
    Icon(
        icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
        modifier = modifier.align(alignment).size(36.dp)
    )
}
