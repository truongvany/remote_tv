package com.example.remote_tv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.remote_tv.ui.theme.ButtonBackground
import com.example.remote_tv.ui.theme.OrangeAccent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeyboardInput(onSendText: (String) -> Unit = {}) {
    var text by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ButtonBackground, RoundedCornerShape(28.dp))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("Nhập nội dung tìm kiếm / gõ phím...", color = Color.Gray) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = OrangeAccent,
                unfocusedBorderColor = Color.DarkGray,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                if (text.isNotEmpty()) {
                    onSendText(text)
                    text = ""
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent),
            modifier = Modifier.fillMaxWidth(0.6f)
        ) {
            Icon(Icons.Filled.Send, contentDescription = "Send", modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Gửi lên TV", color = Color.White)
        }
    }
}

