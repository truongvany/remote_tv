package com.example.remote_tv.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ──────────────── DATA MODELS ────────────────

data class TrendingItem(
    val title: String,
    val category: String,
    val icon: ImageVector,
    val isNew: Boolean = false,
    val isLive: Boolean = false
)

data class AppItem(
    val name: String,
    val subtitle: String,
    val packageName: String,
    val isSelected: Boolean = false,
    val themeColor: Color = Color(0xFF888888),
    val logoIcon: ImageVector? = null,
    val logoLabel: String? = null
)

// ──────────────── DATA ────────────────

private val trendingList = listOf(
    TrendingItem("Quick Launch", "Trải nghiệm mượt mà", Icons.Filled.Bolt, isNew = true),
    TrendingItem("Cast Screen",  "Chia sẻ màn hình",   Icons.Filled.Cast, isLive = true),
    TrendingItem("Cài đặt",     "Hệ thống",            Icons.Filled.Settings)
)

private val appList = listOf(
    AppItem(
        name        = "YouTube",
        subtitle    = "Video & Shorts",
        packageName = "com.google.android.youtube.tv",
        isSelected  = true,
        themeColor  = Color(0xFFFF0000),
        logoIcon    = Icons.Filled.PlayArrow
    ),
    AppItem(
        name        = "Netflix",
        subtitle    = "Movies & Series",
        packageName = "com.netflix.ninja",
        themeColor  = Color(0xFFE50914),
        logoLabel   = "N"
    ),
    AppItem(
        name        = "FPT Play",
        subtitle    = "Truyền hình Việt",
        packageName = "com.fptplay.nettv",
        themeColor  = Color(0xFFFF6A2A),
        logoIcon    = Icons.Filled.Tv
    ),
    AppItem(
        name        = "YT Music",
        subtitle    = "Âm nhạc trực tuyến",
        packageName = "com.google.android.youtube.tvmusic",
        themeColor  = Color(0xFFFF0044),
        logoIcon    = Icons.Filled.MusicNote
    ),
    AppItem(
        name        = "Prime Video",
        subtitle    = "Amazon Originals",
        packageName = "com.amazon.amazonvideo.livingroom",
        themeColor  = Color(0xFF00A0D6),
        logoIcon    = Icons.Filled.PlayArrow
    ),
    AppItem(
        name        = "Cài đặt",
        subtitle    = "Hệ thống TV",
        packageName = "com.android.tv.settings",
        themeColor  = Color(0xFF8E8E93),
        logoIcon    = Icons.Filled.Settings
    )
)

// ──────────────── MAIN SCREEN ────────────────

@Composable
fun ChannelsScreen(
    onLaunchApp: (String) -> Unit = {}
) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val bgColor = MaterialTheme.colorScheme.background

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        // ── Page title ──
        Text(
            text = "Channels",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ── Search bar ──
        SimpleSearchBar()

        Spacer(modifier = Modifier.height(28.dp))

        // ── Features ──
        Label("Tính năng")
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(end = 2.dp)
        ) {
            items(trendingList) { FeatureChip(it) }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // ── Quick Launch ──
        Label("Quick Launch")
        Spacer(modifier = Modifier.height(12.dp))

        appList.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowItems.forEach { app ->
                    CleanAppCard(
                        appItem  = app,
                        modifier = Modifier.weight(1f),
                        onClick  = { onLaunchApp(app.packageName) }
                    )
                }
                if (rowItems.size < 2) Spacer(modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        Spacer(modifier = Modifier.height(100.dp))
    }
}

// ──────────────── SEARCH BAR ────────────────

@Composable
fun SimpleSearchBar() {
    val cardColor = MaterialTheme.colorScheme.secondary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(cardColor)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Filled.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "Tìm ứng dụng...",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
    }
}

// ──────────────── LABEL ────────────────

@Composable
fun Label(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.8.sp,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f)
    )
}

// ──────────────── FEATURE CHIP ────────────────

@Composable
fun FeatureChip(item: TrendingItem) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.7f),
        label = "scale"
    )

    Row(
        modifier = Modifier
            .scale(scale)
            .height(42.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.secondary)
            .border(
                1.dp,
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f),
                RoundedCornerShape(12.dp)
            )
            .clickable(interactionSource = interactionSource, indication = null) {}
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            item.icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.75f),
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = item.title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSecondary
        )
        if (item.isLive) {
            Box(
                modifier = Modifier
                    .background(Color(0xFFFF3B30), RoundedCornerShape(4.dp))
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text("LIVE", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
        }
        if (item.isNew) {
            Box(
                modifier = Modifier
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text(
                    "NEW",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ──────────────── CLEAN APP CARD ────────────────

@Composable
fun CleanAppCard(appItem: AppItem, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.7f),
        label = "scale"
    )

    val shape = RoundedCornerShape(20.dp)
    val borderColor = if (appItem.isSelected)
        appItem.themeColor.copy(alpha = 0.5f)
    else
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)

    Column(
        modifier = modifier
            .scale(scale)
            .clip(shape)
            .background(MaterialTheme.colorScheme.secondary)
            .border(1.dp, borderColor, shape)
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .padding(vertical = 20.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Icon container
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(appItem.themeColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            if (appItem.logoLabel != null) {
                Text(
                    text = appItem.logoLabel,
                    color = appItem.themeColor,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            } else if (appItem.logoIcon != null) {
                Icon(
                    imageVector = appItem.logoIcon,
                    contentDescription = appItem.name,
                    tint = appItem.themeColor,
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = appItem.name,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSecondary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(3.dp))

        // "Playing" indicator dành riêng cho selected
        if (appItem.isSelected) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .background(appItem.themeColor, CircleShape)
                )
                Text(
                    text = "Đang phát",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = appItem.themeColor
                )
            }
        } else {
            Text(
                text = appItem.subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.45f),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
