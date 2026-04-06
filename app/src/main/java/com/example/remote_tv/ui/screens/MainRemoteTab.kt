package com.example.remote_tv.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.remote_tv.ui.components.*
import com.example.remote_tv.ui.viewmodel.TVViewModel
import java.util.Locale

@Composable
fun MainRemoteTab(viewModel: TVViewModel) {
    val context = LocalContext.current
    val currentDevice by viewModel.currentDevice.collectAsState()
    val deviceName = currentDevice?.name ?: "Living Room TV"
    var isVoiceListening by remember { mutableStateOf(false) }

    val speechRecognizer = remember(context) {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            SpeechRecognizer.createSpeechRecognizer(context)
        } else {
            null
        }
    }

    val recognizerIntent = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
    }

    val startVoiceRecognition = {
        if (speechRecognizer == null) {
            // Fallback: ask TV to open native voice assistant if speech recognizer is unavailable.
            viewModel.sendCommand("KEY_VOICE")
        } else {
            runCatching {
                isVoiceListening = true
                speechRecognizer.startListening(recognizerIntent)
            }.onFailure {
                isVoiceListening = false
                viewModel.sendCommand("KEY_VOICE")
            }
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (granted) {
                startVoiceRecognition()
            }
        }
    )

    DisposableEffect(speechRecognizer) {
        val listener = object : RecognitionListener {
            override fun onReadyForSpeech(params: android.os.Bundle?) {
                isVoiceListening = true
            }

            override fun onBeginningOfSpeech() {
                isVoiceListening = true
            }

            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit

            override fun onEndOfSpeech() = Unit

            override fun onError(error: Int) {
                isVoiceListening = false
                if (error != SpeechRecognizer.ERROR_NO_MATCH && error != SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                    // Fallback for TVs that support native voice trigger.
                    viewModel.sendCommand("KEY_VOICE")
                }
            }

            override fun onResults(results: android.os.Bundle?) {
                isVoiceListening = false
                val spokenText = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    ?.trim()
                    .orEmpty()

                if (spokenText.isNotEmpty()) {
                    // Open search first and then inject recognized text when protocol supports it.
                    viewModel.sendCommand("KEY_SEARCH")
                    viewModel.sendCommand("TEXT:$spokenText")
                    viewModel.sendCommand("KEY_ENTER")
                }
            }

            override fun onPartialResults(partialResults: android.os.Bundle?) = Unit
            override fun onEvent(eventType: Int, params: android.os.Bundle?) = Unit
        }

        speechRecognizer?.setRecognitionListener(listener)

        onDispose {
            runCatching {
                speechRecognizer?.cancel()
                speechRecognizer?.destroy()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TopBar(
            deviceName = deviceName,
            onPower = { viewModel.powerToggle() },
            onCastClick = { viewModel.selectTab(2) }
        )

        NowPlayingCard()

        val uiState by viewModel.uiState.collectAsState()

        ModeSelector(
            selectedMode = uiState.selectedMode,
            onModeSelected = { viewModel.selectMode(it) }
        )

        Spacer(modifier = Modifier.height(28.dp))

        when (uiState.selectedMode) {
            0 -> DPad(
                onDirection = { viewModel.sendDirection(it) },
                onOk = { viewModel.sendOk() }
            )
            1 -> Touchpad(
                onSwipeUp = { viewModel.sendCommand("UP") },
                onSwipeDown = { viewModel.sendCommand("DOWN") },
                onSwipeLeft = { viewModel.sendCommand("LEFT") },
                onSwipeRight = { viewModel.sendCommand("RIGHT") },
                onTap = { viewModel.sendOk() }
            )
            2 -> KeyboardInput(
                onSendText = { text -> viewModel.sendCommand("TEXT:$text") }
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        ControlButtons(
            onCommand = { viewModel.sendCommand(it) },
            isVoiceListening = isVoiceListening,
            onVoiceClick = {
                if (isVoiceListening) {
                    runCatching { speechRecognizer?.stopListening() }
                    isVoiceListening = false
                } else {
                    val hasAudioPermission = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.RECORD_AUDIO,
                    ) == PackageManager.PERMISSION_GRANTED

                    if (hasAudioPermission) {
                        startVoiceRecognition()
                    } else {
                        audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(28.dp))

        QuickLaunch(onLaunchApp = { viewModel.launchApp(it) })

        Spacer(modifier = Modifier.height(24.dp))
    }
}
