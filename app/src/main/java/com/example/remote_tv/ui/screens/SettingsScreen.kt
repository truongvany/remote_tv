package com.example.remote_tv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SettingsRemote
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.remote_tv.R
import com.example.remote_tv.data.model.AppThemeMode
import com.example.remote_tv.data.preferences.LocaleManager
import com.example.remote_tv.ui.viewmodel.SettingsUiState

private data class LanguageOption(val code: String, val label: String)

@Composable
fun SettingsScreen(
    settingsUiState: SettingsUiState,
    onThemeChanged: (Boolean) -> Unit,
    onLanguageChanged: (String) -> Unit,
    onAutoReconnectChanged: (Boolean) -> Unit,
    onAutoScanCastChanged: (Boolean) -> Unit,
    onKeepScreenOnChanged: (Boolean) -> Unit,
    onForgetLastDevice: () -> Unit,
    onClearDiagnostics: () -> Unit,
    onProfileSave: (String, String) -> Unit,
    onDismissProfileError: () -> Unit,
) {
    val profile = settingsUiState.userProfile
    val appSettings = settingsUiState.appSettings
    val isDarkMode = appSettings.themeMode == AppThemeMode.DARK

    val languageOptions = listOf(
        LanguageOption(code = "en", label = stringResource(R.string.language_english)),
        LanguageOption(code = "vi", label = stringResource(R.string.language_vietnamese)),
    )

    var languageMenuExpanded by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }

    val currentLanguage = languageOptions.firstOrNull { it.code == appSettings.languageCode } ?: languageOptions.first()
    val profileErrorText = profileErrorMessage(settingsUiState.profileError)

    if (showEditProfileDialog) {
        EditProfileDialog(
            initialName = profile.displayName,
            initialEmail = profile.email,
            isSaving = settingsUiState.isProfileSaving,
            onDismiss = {
                showEditProfileDialog = false
                onDismissProfileError()
            },
            onSave = { name, email -> onProfileSave(name, email) },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        SettingsHero(
            displayName = profile.displayName,
            email = profile.email,
        )

        Spacer(modifier = Modifier.height(18.dp))

        SettingsPanel(
            title = stringResource(R.string.section_account),
            subtitle = stringResource(R.string.settings_account_subtitle),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = profile.displayName.take(1).ifEmpty { "U" }.uppercase(),
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                        )
                    }
                    Spacer(modifier = Modifier.size(12.dp))
                    Column {
                        Text(
                            text = profile.displayName,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = if (profile.email.isBlank()) stringResource(R.string.profile_email_placeholder) else profile.email,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                OutlinedButton(onClick = { showEditProfileDialog = true }) {
                    Text(stringResource(R.string.profile_edit))
                }
            }
        }

        if (profileErrorText != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.errorContainer,
            ) {
                Text(
                    text = profileErrorText,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    fontSize = 13.sp,
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        SettingsPanel(
            title = stringResource(R.string.section_preferences),
            subtitle = stringResource(R.string.settings_preferences_subtitle),
        ) {
            ToggleSettingRow(
                icon = if (isDarkMode) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                title = stringResource(R.string.theme_mode),
                subtitle = if (isDarkMode) stringResource(R.string.theme_dark) else stringResource(R.string.theme_light),
                checked = isDarkMode,
                onCheckedChange = onThemeChanged,
            )

            SettingsDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SettingIcon(icon = Icons.Filled.Language)
                    Spacer(modifier = Modifier.size(12.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.language_title),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = currentLanguage.label,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                        )
                    }
                }

                Box {
                    OutlinedButton(onClick = { languageMenuExpanded = true }) {
                        Text(currentLanguage.label)
                    }
                    DropdownMenu(
                        expanded = languageMenuExpanded,
                        onDismissRequest = { languageMenuExpanded = false },
                    ) {
                        languageOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.label) },
                                onClick = {
                                    languageMenuExpanded = false
                                    LocaleManager.applyLanguage(option.code)
                                    onLanguageChanged(option.code)
                                },
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        SettingsPanel(
            title = stringResource(R.string.section_connection),
            subtitle = stringResource(R.string.settings_connection_subtitle),
        ) {
            ToggleSettingRow(
                icon = Icons.Filled.SettingsRemote,
                title = stringResource(R.string.setting_auto_reconnect_title),
                subtitle = stringResource(R.string.setting_auto_reconnect_subtitle),
                checked = appSettings.autoReconnectLastDevice,
                onCheckedChange = onAutoReconnectChanged,
            )

            SettingsDivider()

            ToggleSettingRow(
                icon = Icons.Filled.Cast,
                title = stringResource(R.string.setting_auto_scan_cast_title),
                subtitle = stringResource(R.string.setting_auto_scan_cast_subtitle),
                checked = appSettings.autoScanOnCastTab,
                onCheckedChange = onAutoScanCastChanged,
            )

            SettingsDivider()

            ToggleSettingRow(
                icon = Icons.Filled.LightMode,
                title = stringResource(R.string.setting_keep_screen_on_title),
                subtitle = stringResource(R.string.setting_keep_screen_on_subtitle),
                checked = appSettings.keepScreenOn,
                onCheckedChange = onKeepScreenOnChanged,
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        SettingsPanel(
            title = stringResource(R.string.section_maintenance),
            subtitle = stringResource(R.string.settings_maintenance_subtitle),
        ) {
            ActionSettingRow(
                icon = Icons.Filled.DeleteOutline,
                title = stringResource(R.string.action_forget_last_device),
                subtitle = stringResource(R.string.action_forget_last_device_subtitle),
                buttonLabel = stringResource(R.string.settings_action_clear),
                onAction = onForgetLastDevice,
                destructive = true,
            )

            SettingsDivider()

            ActionSettingRow(
                icon = Icons.Filled.Tv,
                title = stringResource(R.string.action_clear_diagnostics),
                subtitle = stringResource(R.string.action_clear_diagnostics_subtitle),
                buttonLabel = stringResource(R.string.settings_action_run),
                onAction = onClearDiagnostics,
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        SettingsPanel(
            title = stringResource(R.string.section_about),
            subtitle = stringResource(R.string.settings_about_subtitle),
        ) {
            InfoSettingRow(
                icon = Icons.Filled.Person,
                title = stringResource(R.string.account_type),
                value = stringResource(R.string.account_type_local),
            )

            SettingsDivider()

            InfoSettingRow(
                icon = Icons.Filled.LightMode,
                title = stringResource(R.string.app_version),
                value = stringResource(R.string.app_version_value),
            )

            SettingsDivider()

            InfoSettingRow(
                icon = Icons.Filled.Language,
                title = stringResource(R.string.language_title),
                value = currentLanguage.label,
            )
        }

        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
private fun SettingsHero(displayName: String, email: String) {
    val gradient = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
            MaterialTheme.colorScheme.secondary.copy(alpha = 0.28f),
            MaterialTheme.colorScheme.surface,
        ),
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        tonalElevation = 4.dp,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradient)
                .padding(horizontal = 18.dp, vertical = 20.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = stringResource(R.string.settings_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = stringResource(R.string.settings_subtitle),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = displayName,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = if (email.isBlank()) stringResource(R.string.profile_email_placeholder) else email,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun SettingsPanel(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                letterSpacing = 0.8.sp,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
            )
            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 12.dp),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
    )
}

@Composable
private fun ToggleSettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SettingIcon(icon = icon)
            Spacer(modifier = Modifier.size(12.dp))
            Column {
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = subtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

@Composable
private fun ActionSettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    buttonLabel: String,
    onAction: () -> Unit,
    destructive: Boolean = false,
) {
    val buttonColor = if (destructive) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.primary
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SettingIcon(icon = icon, iconTint = buttonColor)
            Spacer(modifier = Modifier.size(12.dp))
            Column {
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = subtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                )
            }
        }

        OutlinedButton(onClick = onAction) {
            Text(
                text = buttonLabel,
                color = buttonColor,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun InfoSettingRow(
    icon: ImageVector,
    title: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SettingIcon(icon = icon)
            Spacer(modifier = Modifier.size(12.dp))
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
        }

        Text(
            text = value,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SettingIcon(
    icon: ImageVector,
    iconTint: Color = MaterialTheme.colorScheme.primary,
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.55f), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
        )
    }
}

@Composable
private fun EditProfileDialog(
    initialName: String,
    initialEmail: String,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    var email by remember(initialEmail) { mutableStateOf(initialEmail) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.profile_edit)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.profile_name)) },
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.profile_email)) },
                )
            }
        },
        confirmButton = {
            Button(
                enabled = !isSaving,
                onClick = { onSave(name, email) },
            ) {
                Text(stringResource(R.string.save_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel_action))
            }
        },
    )
}

@Composable
private fun profileErrorMessage(code: String?): String? {
    return when (code) {
        "PROFILE_NAME_EMPTY" -> stringResource(R.string.profile_error_name_empty)
        "PROFILE_EMAIL_INVALID" -> stringResource(R.string.profile_error_email_invalid)
        else -> null
    }
}
