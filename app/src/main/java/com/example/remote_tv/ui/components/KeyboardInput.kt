package com.example.remote_tv.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.text.Normalizer

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
            .background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(28.dp))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { updated ->
                text = if (updated.length <= maxChars) updated else updated.take(maxChars)
            },
            label = { Text("Nhập nội dung tìm kiếm / gõ phím...", color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.5f)) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                focusedTextColor = MaterialTheme.colorScheme.onSecondary,
                unfocusedTextColor = MaterialTheme.colorScheme.onSecondary,
                cursorColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.fillMaxWidth(),
            supportingText = {
                Text(
                    text = "${text.length}/$maxChars | Hỗ trợ tiếng Việt, dấu cách và câu dài",
                    color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.4f)
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
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.7f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
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
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.weight(1.4f)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Gửi lên TV", color = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}
