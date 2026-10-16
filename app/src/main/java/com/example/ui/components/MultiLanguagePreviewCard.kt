package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.ClockLanguage
import com.example.model.ClockSettings
import com.example.tts.NaturalTimeFormatter
import java.util.Calendar

@Composable
fun MultiLanguagePreviewCard(
    calendar: Calendar,
    settings: ClockSettings,
    onSelectLanguage: (ClockLanguage) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("multi_language_preview_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Translate,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Multilingual Spoken Announcements",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = "Tap any language to set it as active, or hear how the current time sounds:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // 1. English
            LanguagePhraseRow(
                language = ClockLanguage.ENGLISH,
                isSelected = settings.language == ClockLanguage.ENGLISH,
                phrase = NaturalTimeFormatter.formatSpokenTime(
                    calendar = calendar,
                    language = ClockLanguage.ENGLISH,
                    is24Hour = settings.is24HourFormat,
                    announceAmPm = settings.announceAmPm
                ),
                onClick = { onSelectLanguage(ClockLanguage.ENGLISH) }
            )

            // 2. Bengali (বাংলা)
            LanguagePhraseRow(
                language = ClockLanguage.BENGALI,
                isSelected = settings.language == ClockLanguage.BENGALI,
                phrase = NaturalTimeFormatter.formatSpokenTime(
                    calendar = calendar,
                    language = ClockLanguage.BENGALI,
                    is24Hour = settings.is24HourFormat,
                    announceAmPm = settings.announceAmPm
                ),
                onClick = { onSelectLanguage(ClockLanguage.BENGALI) }
            )

            // 3. Hindi (हिन्दी)
            LanguagePhraseRow(
                language = ClockLanguage.HINDI,
                isSelected = settings.language == ClockLanguage.HINDI,
                phrase = NaturalTimeFormatter.formatSpokenTime(
                    calendar = calendar,
                    language = ClockLanguage.HINDI,
                    is24Hour = settings.is24HourFormat,
                    announceAmPm = settings.announceAmPm
                ),
                onClick = { onSelectLanguage(ClockLanguage.HINDI) }
            )
        }
    }
}

@Composable
private fun LanguagePhraseRow(
    language: ClockLanguage,
    isSelected: Boolean,
    phrase: String,
    onClick: () -> Unit
) {
    val bgColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    }

    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = language.nativeName,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                if (isSelected) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = "ACTIVE",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "\"$phrase\"",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
