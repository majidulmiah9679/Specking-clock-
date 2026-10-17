package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.SpeakingClockViewModel
import com.example.ui.components.ActionButtonsBar
import com.example.ui.components.DigitalClockCard
import com.example.ui.components.DownloadGuideDialog
import com.example.ui.components.IntervalPickerSection
import com.example.ui.components.MultiLanguagePreviewCard
import com.example.ui.components.PermissionBanner
import com.example.ui.components.SettingsDialog
import com.example.ui.components.StatusCountdownCard
import com.example.ui.components.VoiceAndScreenOffCard
import com.example.ui.theme.SpeakingClockTheme

class MainActivity : ComponentActivity() {

    private val viewModel: SpeakingClockViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SpeakingClockTheme {
                SpeakingClockScreen(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshSystemPermissions()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeakingClockScreen(viewModel: SpeakingClockViewModel) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showDownloadGuideDialog by remember { mutableStateOf(false) }

    // Launcher for POST_NOTIFICATIONS permission
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.setNotificationPermissionGranted(isGranted)
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val isGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            viewModel.setNotificationPermissionGranted(isGranted)
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("main_scaffold"),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Speaking Clock",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = uiState.settings.language.nativeName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.testVoice() },
                        modifier = Modifier.testTag("top_test_voice_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Test Voice",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = { showSettingsDialog = true },
                        modifier = Modifier.testTag("top_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val isWideScreen = maxWidth >= 600.dp

            if (isWideScreen) {
                // Wide Screen Tablet / Foldable Layout: Two Columns
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Left Column: Clock and Status
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        DigitalClockCard(
                            calendar = uiState.currentCalendar,
                            settings = uiState.settings
                        )
                        StatusCountdownCard(
                            settings = uiState.settings,
                            countdownSeconds = uiState.countdownSeconds
                        )
                        VoiceAndScreenOffCard(
                            currentGender = uiState.settings.voiceGender,
                            screenOffAlwaysActive = uiState.settings.screenOffAlwaysActive,
                            onSelectVoiceGender = { viewModel.updateVoiceGender(it) },
                            onToggleScreenOff = { viewModel.updateScreenOffAlwaysActive(it) },
                            onOpenDownloadGuide = { showDownloadGuideDialog = true }
                        )
                    }

                    // Right Column: Controls, Interval, and Multilingual Preview
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        ActionButtonsBar(
                            status = uiState.settings.status,
                            isSpeakingTest = uiState.isSpeakingTest,
                            onStart = { viewModel.startClock() },
                            onPause = { viewModel.pauseClock() },
                            onResume = { viewModel.resumeClock() },
                            onStop = { viewModel.stopClock() },
                            onTestVoice = { viewModel.testVoice() },
                            onOpenSettings = { showSettingsDialog = true }
                        )

                        IntervalPickerSection(
                            selectedPreset = uiState.settings.intervalPreset,
                            customMinutes = uiState.settings.customIntervalMinutes,
                            onSelectInterval = { preset, minutes ->
                                viewModel.updateInterval(preset, minutes)
                            }
                        )

                        PermissionBanner(
                            hasNotificationPermission = uiState.hasNotificationPermission,
                            canScheduleExactAlarms = uiState.canScheduleExactAlarms,
                            isIgnoringBatteryOptimizations = uiState.isIgnoringBatteryOptimizations,
                            onRequestNotificationPermission = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            }
                        )

                        MultiLanguagePreviewCard(
                            calendar = uiState.currentCalendar,
                            settings = uiState.settings,
                            onSelectLanguage = { viewModel.updateLanguage(it) }
                        )
                    }
                }
            } else {
                // Standard Mobile Layout
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .widthIn(max = 600.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 1. Large Digital Clock
                    DigitalClockCard(
                        calendar = uiState.currentCalendar,
                        settings = uiState.settings
                    )

                    // 2. Service Status and Next Announcement Countdown
                    StatusCountdownCard(
                        settings = uiState.settings,
                        countdownSeconds = uiState.countdownSeconds
                    )

                    // 3. Voice Gender (Female/Male) & Screen-Off Always Active Controls
                    VoiceAndScreenOffCard(
                        currentGender = uiState.settings.voiceGender,
                        screenOffAlwaysActive = uiState.settings.screenOffAlwaysActive,
                        onSelectVoiceGender = { viewModel.updateVoiceGender(it) },
                        onToggleScreenOff = { viewModel.updateScreenOffAlwaysActive(it) },
                        onOpenDownloadGuide = { showDownloadGuideDialog = true }
                    )

                    // 4. Primary Control Action Buttons
                    ActionButtonsBar(
                        status = uiState.settings.status,
                        isSpeakingTest = uiState.isSpeakingTest,
                        onStart = { viewModel.startClock() },
                        onPause = { viewModel.pauseClock() },
                        onResume = { viewModel.resumeClock() },
                        onStop = { viewModel.stopClock() },
                        onTestVoice = { viewModel.testVoice() },
                        onOpenSettings = { showSettingsDialog = true }
                    )

                    // 5. Interval Selector (15m, 30m, 1h, Custom)
                    IntervalPickerSection(
                        selectedPreset = uiState.settings.intervalPreset,
                        customMinutes = uiState.settings.customIntervalMinutes,
                        onSelectInterval = { preset, minutes ->
                            viewModel.updateInterval(preset, minutes)
                        }
                    )

                    // 6. System Permissions & Battery Notice Banner
                    PermissionBanner(
                        hasNotificationPermission = uiState.hasNotificationPermission,
                        canScheduleExactAlarms = uiState.canScheduleExactAlarms,
                        isIgnoringBatteryOptimizations = uiState.isIgnoringBatteryOptimizations,
                        onRequestNotificationPermission = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                    )

                    // 7. Multilingual Announcement Preview (English, Bengali, Hindi)
                    MultiLanguagePreviewCard(
                        calendar = uiState.currentCalendar,
                        settings = uiState.settings,
                        onSelectLanguage = { viewModel.updateLanguage(it) }
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    if (showSettingsDialog) {
        SettingsDialog(
            settings = uiState.settings,
            languageAvailability = uiState.languageAvailability,
            aiGenerating = uiState.aiGenerating,
            aiStatusMessage = uiState.aiStatusMessage,
            onDismiss = { showSettingsDialog = false },
            onSelectLanguage = { viewModel.updateLanguage(it) },
            onSelectVoiceGender = { viewModel.updateVoiceGender(it) },
            onToggleScreenOffAlways = { viewModel.updateScreenOffAlwaysActive(it) },
            onUpdateSpeechRate = { viewModel.updateSpeechRate(it) },
            onUpdateSpeechPitch = { viewModel.updateSpeechPitch(it) },
            onToggle24h = { viewModel.update24HourFormat(it) },
            onToggleAmPm = { viewModel.updateAnnounceAmPm(it) },
            onToggleChime = { viewModel.updatePlayChime(it) },
            onToggleVibrate = { viewModel.updateVibrate(it) },
            onToggleAutoRestart = { viewModel.updateAutoRestartBoot(it) },
            onTestAiSpeech = { viewModel.generateGeminiAiSpeechTest() },
            onTestLyriaChime = { viewModel.generateLyriaChimeTest() },
            onClearAiMessage = { viewModel.clearAiMessage() }
        )
    }

    if (showDownloadGuideDialog) {
        DownloadGuideDialog(
            onDismiss = { showDownloadGuideDialog = false }
        )
    }
}
