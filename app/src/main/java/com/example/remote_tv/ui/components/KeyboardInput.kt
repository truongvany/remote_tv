package com.example.remote_tv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.remote_tv.ui.theme.ButtonBackground
import com.example.remote_tv.ui.theme.OrangeAccent
import java.text.Normalizer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeyboardInput(onSendText: (String) -> Unit = {}) {
    var text by remember { mutableStateOf("") }
    val maxChars = 320
    val sanitized = remember(text) {
        Normalizer.normalize(text, Normalizer.Form.NFC)
            .replace("\r", "")
            .replace("\n", " ")
    }
    val canSend = sanitized.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ButtonBackground, RoundedCornerShape(28.dp))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { updated ->
                text = if (updated.length <= maxChars) updated else updated.take(maxChars)
            },
            label = { Text("Nhập nội dung tìm kiếm / gõ phím...", color = Color.Gray) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = OrangeAccent,
                unfocusedBorderColor = Color.DarkGray,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth(),
            supportingText = {
                Text(
                    text = "${text.length}/$maxChars | Hỗ trợ tiếng Việt, dấu cách và câu dài",
                    color = Color(0xFF8E8E8E)
                )
            },
            minLines = 3,
            maxLines = 5,
            singleLine = false
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = { text = "" },
                modifier = Modifier.weight(1f),
                enabled = text.isNotEmpty(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFCFCFCF))
            ) {
                Icon(Icons.Filled.Clear, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Xóa")
            }

            Button(
                onClick = {
                    if (canSend) {
                        onSendText(sanitized)
                    }
                },
                enabled = canSend,
                colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent),
                modifier = Modifier.weight(1.4f)
            ) {
                Icon(Icons.Filled.Send, contentDescription = "Send", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Gửi lên TV", color = Color.White)
            }
        }
    }
}

