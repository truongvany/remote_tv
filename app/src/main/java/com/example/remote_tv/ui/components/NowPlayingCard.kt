package com.example.remote_tv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.remote_tv.ui.theme.OrangeAccent
import com.example.remote_tv.ui.theme.TextSecondary

@Composable
fun NowPlayingCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(30.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF151515), Color(0xFF121212))
                )
            )
            .border(1.dp, Color(0xFF262626), RoundedCornerShape(30.dp))
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "NOW PLAYING",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "The Mandalorian",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "S2 : E5 • Chapter 13: The Jedi",
                    color = OrangeAccent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF1F1F1F))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "4K",
                    color = OrangeAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.VolumeDown,
                contentDescription = null,
                tint = Color(0xFFADAAAA),
                modifier = Modifier.size(16.dp)
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(10.dp)
                    .clip(CircleShape)
                    .background(Color.Black)
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .weight(0.68f)
                            .fillMaxHeight()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(OrangeAccent, Color(0xFFFF5D1F))
                                )
                            )
                    )
                    Box(
                        modifier = Modifier
                            .weight(0.09f)
                            .fillMaxHeight()
                            .background(Color(0xFFE4E2E1))
                    )
                    Box(
                        modifier = Modifier
                            .weight(0.23f)
                            .fillMaxHeight()
                            .background(Color.Black)
                    )
                }
            }

            Icon(
                Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = null,
                tint = Color(0xFFADAAAA),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
