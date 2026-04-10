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
    var voiceStatusText by remember { mutableStateOf("Tap mic to start speaking") }
    var voiceTranscript by remember { mutableStateOf("") }
    var voiceRmsLevel by remember { mutableFloatStateOf(0f) }
    var busyRetryAttempted by remember { mutableStateOf(false) }

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
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 900L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 600L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1200L)
        }
    }

    val startVoiceRecognition = {
        voiceTranscript = ""
        voiceStatusText = "Starting microphone..."
        voiceRmsLevel = 0f
        busyRetryAttempted = false
        isVoiceListening = true

        if (speechRecognizer == null) {
            voiceStatusText = "Voice recognizer is unavailable on this device"
            isVoiceListening = false
            viewModel.sendCommand("KEY_VOICE")
        } else {
            runCatching {
                speechRecognizer.cancel()
                speechRecognizer.startListening(recognizerIntent)
            }.onFailure {
                voiceStatusText = "Cannot start listening right now"
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
            } else {
                voiceStatusText = "Microphone permission is required"
            }
        }
    )

    DisposableEffect(speechRecognizer) {
        val listener = object : RecognitionListener {
            override fun onReadyForSpeech(params: android.os.Bundle?) {
                isVoiceListening = true
                voiceStatusText = "Listening..."
            }

            override fun onBeginningOfSpeech() {
                isVoiceListening = true
                voiceStatusText = "Speak naturally"
            }

            override fun onRmsChanged(rmsdB: Float) {
                voiceRmsLevel = rmsdB.coerceAtLeast(0f)
            }
            override fun onBufferReceived(buffer: ByteArray?) = Unit

            override fun onEndOfSpeech() {
                voiceStatusText = "Processing your voice..."
            }

            override fun onError(error: Int) {
                if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY && !busyRetryAttempted) {
                    busyRetryAttempted = true
                    runCatching {
                        speechRecognizer?.cancel()
                        speechRecognizer?.startListening(recognizerIntent)
                    }.onFailure {
                        isVoiceListening = false
                        voiceStatusText = "Voice recognizer is busy"
                    }
                    return
                }

                isVoiceListening = false
                voiceRmsLevel = 0f
                voiceStatusText = speechErrorMessage(error)
                if (error != SpeechRecognizer.ERROR_NO_MATCH && error != SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                    viewModel.sendCommand("KEY_VOICE")
                }
            }

            override fun onResults(results: android.os.Bundle?) {
                val spokenText = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    ?.trim()
                    .orEmpty()

                if (spokenText.isNotEmpty()) {
                    voiceTranscript = spokenText
                    voiceStatusText = "Sending to TV..."
                    viewModel.sendVoiceQuery(spokenText)
                } else {
                    voiceStatusText = "No speech detected"
                }

                isVoiceListening = false
                voiceRmsLevel = 0f
            }

            override fun onPartialResults(partialResults: android.os.Bundle?) {
                val partialText = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    ?.trim()
                    .orEmpty()

                if (partialText.isNotEmpty()) {
                    voiceTranscript = partialText
                    voiceStatusText = "Listening..."
                }
            }

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

    val uiState by viewModel.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TopBar(
                deviceName = deviceName,
                onPower = {
                    if (currentDevice != null) {
                        viewModel.sendCommand("KEY_POWER")
                    } else {
                        viewModel.wakeLastDevice()
                    }
                },
                onCastClick = { viewModel.selectTab(2) }
            )

            if (uiState.showNowPlaying) {
                NowPlayingCard()
            }

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
                    onSendText = { text -> viewModel.sendText(text) }
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
                        voiceRmsLevel = 0f
                        voiceStatusText = "Voice listening stopped"
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

            val installedApps by viewModel.installedApps.collectAsState()
            QuickLaunch(
                apps = installedApps,
                isEnabled = currentDevice != null,
                onLaunchApp = { viewModel.launchApp(it) }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        if (isVoiceListening) {
            VoiceListeningOverlay(
                statusText = voiceStatusText,
                transcript = voiceTranscript,
                rmsLevel = voiceRmsLevel,
                onStop = {
                    runCatching { speechRecognizer?.stopListening() }
                    isVoiceListening = false
                    voiceRmsLevel = 0f
                    voiceStatusText = "Voice listening stopped"
                }
            )
        }
    }
}

private fun speechErrorMessage(errorCode: Int): String {
    return when (errorCode) {
        SpeechRecognizer.ERROR_AUDIO -> "Audio input error"
        SpeechRecognizer.ERROR_CLIENT -> "Voice recognition interrupted"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission missing"
        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network issue while recognizing"
        SpeechRecognizer.ERROR_NO_MATCH -> "Could not understand that, try again"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Voice recognizer is busy"
        SpeechRecognizer.ERROR_SERVER -> "Speech service is unavailable"
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected"
        else -> "Voice recognition failed"
    }
}
