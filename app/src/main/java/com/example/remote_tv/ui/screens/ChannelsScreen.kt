package com.example.remote_tv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.remote_tv.ui.theme.ButtonBackground
import com.example.remote_tv.ui.theme.CardBackground
import com.example.remote_tv.ui.theme.OrangeAccent
import com.example.remote_tv.ui.theme.TextSecondary

data class TrendingItem(val title: String, val category: String, val isNew: Boolean = false, val isLive: Boolean = false)
data class Channel(val name: String, val subtitle: String, val isSelected: Boolean = false)

private val trendingList = listOf(
    TrendingItem("Neon Protocol", "Action • Sci-Fi", isNew = true),
    TrendingItem("The Mandalorian", "Sci-Fi • Adventure", isLive = true),
    TrendingItem("Dark Matter", "Thriller", isNew = true)
)

private val channelList = listOf(
    Channel("HBO", "The Last of Us", isSelected = true),
    Channel("CNN", "Global Report"),
    Channel("DSY", "Deep Ocean"),
    Channel("GEO", "Wild Savanna"),
    Channel("MTV", "Music Live"),
    Channel("BBC", "World News"),
    Channel("SPN", "Game Day"),
    Channel("FOX", "Primetime")
)

@Composable
fun ChannelsScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        ChannelsHeader()
        
        Spacer(modifier = Modifier.height(24.dp))
        SearchBar()

        Spacer(modifier = Modifier.height(32.dp))
        SectionHeader(title = "Trending", hasViewAll = true)
        
        Spacer(modifier = Modifier.height(16.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 8.dp)
        ) {
            items(trendingList) { item ->
                TrendingCard(item)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Channels",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(ButtonBackground, RoundedCornerShape(14.dp))
                    .clickable { },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.FilterList, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // Channel Grid
        channelList.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                rowItems.forEach { channel ->
                    ChannelCard(channel, modifier = Modifier.weight(1f))
                }
                if (rowItems.size < 2) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
fun ChannelsHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Logo Icon
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(OrangeAccent, RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Search, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "COMMAND",
                color = OrangeAccent,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 22.sp,
                letterSpacing = 1.sp
            )
        }
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Search, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(20.dp))
            // Profile
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF252525))
            )
        }
    }
}

@Composable
fun SearchBar() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(Color(0xFF0F0F0F), RoundedCornerShape(20.dp))
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Search, contentDescription = null, tint = Color(0xFF333333), modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Search for channels, movies, or show",
                color = Color(0xFF555555),
                fontSize = 15.sp
            )
        }
    }
}

@Composable
fun SectionHeader(title: String, hasViewAll: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        if (hasViewAll) {
            Text(
                text = "VIEW ALL",
                color = OrangeAccent,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
    }
}

@Composable
fun TrendingCard(item: TrendingItem) {
    Box(
        modifier = Modifier
            .width(280.dp)
            .height(160.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(Color(0xFF1A1A1A))
    ) {
        // Mock background image with gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f)),
                        startY = 50f
                    )
                )
        )
        
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (item.isNew) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF3D1E16), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text("NEW", color = OrangeAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                }
                if (item.isLive) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF2D1010), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text("LIVE", color = Color.Red, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                }
                Text(item.category, color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                item.title, 
                color = Color.White, 
                fontSize = 22.sp, 
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
fun ChannelCard(channel: Channel, modifier: Modifier = Modifier) {
    val isSelected = channel.isSelected
    val backgroundColor = if (isSelected) Color(0xFF151515) else Color(0xFF151515)
    val borderColor = if (isSelected) OrangeAccent.copy(alpha = 0.5f) else Color.Transparent

    Box(
        modifier = modifier
            .height(150.dp)
            .clip(RoundedCornerShape(40.dp))
            .background(backgroundColor)
            .then(if (isSelected) Modifier.border(1.5.dp, borderColor, RoundedCornerShape(40.dp)) else Modifier)
            .clickable { }
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            // Orange Dot
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-4).dp, y = 4.dp)
                    .size(6.dp)
                    .background(OrangeAccent, CircleShape)
            )
        }
        
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = channel.name,
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
            
            if (isSelected) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "NOW PLAYING",
                    color = OrangeAccent,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = channel.subtitle,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = channel.subtitle,
                    color = Color.Gray,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
