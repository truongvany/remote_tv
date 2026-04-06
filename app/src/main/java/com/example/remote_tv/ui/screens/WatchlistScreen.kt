package com.example.remote_tv.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.remote_tv.ui.theme.CardBackground
import com.example.remote_tv.ui.theme.OrangeAccent
import com.example.remote_tv.ui.theme.TextPrimary
import com.example.remote_tv.ui.theme.TextSecondary

@Composable
fun WatchlistScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 16.dp)
    ) {
        HeaderSection()
        ProfileSection()
        StatsGrid()
        WatchlistSection()
        ContinueWatchingSection()
        MyDevicesSection()
        AccountManagementSection()
    }
}

@Composable
private fun HeaderSection() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.FlashOn,
                contentDescription = null,
                tint = OrangeAccent,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "ONYX COMMAND",
                color = OrangeAccent,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Box(
            modifier = Modifier
                .size(40.dp)
                .border(1.dp, Color(0xFF333333), CircleShape)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Lightbulb,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun ProfileSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .border(2.dp, OrangeAccent, CircleShape)
                    .padding(4.dp)
                    .clip(CircleShape)
                    .background(Color.DarkGray)
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    tint = Color.Gray
                )
            }
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(OrangeAccent, CircleShape)
                    .border(2.dp, Color.Black, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("6", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Alex Johnson",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "PREMIUM MEMBER",
                color = OrangeAccent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                " • Since 2022",
                color = Color.Gray,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun StatsGrid() {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("HOURS WATCHED", "1,284 h", Modifier.weight(1f), true)
            StatCard("FAVORITE GENRE", "Sci-Fi", Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("DEVICES", "03", Modifier.weight(1f))
            StatCard("WATCHLIST", "42", Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatCard(title: String, value: String, modifier: Modifier = Modifier, hasGlow: Boolean = false) {
    Box(
        modifier = modifier
            .height(100.dp)
            .background(CardBackground, RoundedCornerShape(24.dp))
            .then(
                if (hasGlow) Modifier.border(
                    1.dp,
                    Brush.verticalGradient(listOf(OrangeAccent, Color.Transparent)),
                    RoundedCornerShape(24.dp)
                ) else Modifier
            )
            .padding(16.dp)
    ) {
        Column {
            Text(title, color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun WatchlistSection() {
    Column(modifier = Modifier.padding(vertical = 24.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column {
                Text(
                    "WATCHLIST",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Your curated cinema queue",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
            Text(
                "VIEW ALL >",
                color = OrangeAccent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { }
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(3) {
                WatchlistCard()
            }
        }
    }
}

@Composable
private fun WatchlistCard() {
    Box(
        modifier = Modifier
            .width(240.dp)
            .height(320.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.DarkGray)
    ) {
        // Placeholder for image
        Box(modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)))
        ))
        
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            Text("Interstellar", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = Color.DarkGray,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        "SCI-FI",
                        color = OrangeAccent,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    Icons.Default.PlayCircle,
                    contentDescription = null,
                    tint = OrangeAccent,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun ContinueWatchingSection() {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            "CONTINUE WATCHING",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 16.dp)
        )
        ContinueWatchingItem("Succession", "S4 • E8 | 42:10 remaining")
        Spacer(modifier = Modifier.height(12.dp))
        ContinueWatchingItem("The Bear", "S2 • E1 | 15:00 remaining")
    }
}

@Composable
private fun ContinueWatchingItem(title: String, subtitle: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBackground, RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(width = 80.dp, height = 45.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.DarkGray)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = Color.Gray, fontSize = 12.sp)
        }
        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
    }
}

@Composable
private fun MyDevicesSection() {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp)) {
        Text(
            "MY DEVICES",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        DeviceItem("Living Room OLED", "CONNECTED", true)
        Spacer(modifier = Modifier.height(12.dp))
        DeviceItem("Master Bedroom TV", "OFFLINE", false)
    }
}

@Composable
private fun DeviceItem(name: String, status: String, isConnected: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBackground, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Color(0xFF222222), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Tv, contentDescription = null, tint = if (isConnected) OrangeAccent else Color.Gray)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isConnected) {
                    Box(modifier = Modifier.size(4.dp).background(OrangeAccent, CircleShape))
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(status, color = if (isConnected) OrangeAccent else Color.Gray, fontSize = 10.sp)
            }
        }
        Icon(
            if (isConnected) Icons.Default.Settings else Icons.Default.PowerSettingsNew,
            contentDescription = null,
            tint = Color.Gray,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun AccountManagementSection() {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            "ACCOUNT MANAGEMENT",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 16.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardBackground, RoundedCornerShape(24.dp))
                .padding(vertical = 8.dp)
        ) {
            Column {
                AccountActionItem(Icons.Default.PersonOutline, "Edit Profile")
                AccountActionItem(Icons.Default.Tune, "Preferences")
                AccountActionItem(Icons.Default.Security, "Account Security")
            }
        }
    }
}

@Composable
private fun AccountActionItem(icon: ImageVector, title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = OrangeAccent, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(title, color = Color.White, fontSize = 16.sp, modifier = Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
    }
}
