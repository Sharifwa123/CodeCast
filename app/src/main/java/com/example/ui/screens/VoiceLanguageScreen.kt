package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun VoiceLanguageScreen(
    voiceName: String,
    voiceGender: String,
    voiceAccent: String,
    speakingStyle: String,
    speakingSpeed: Float,
    narrationLang: String,
    subtitleLang: String,
    customTerminology: String,
    hasSubtitles: Boolean,
    subtitleStyle: String,
    subtitlePosition: String,
    onVoiceChange: (name: String, gender: String, accent: String, style: String, speed: Float) -> Unit,
    onNarrationLangChange: (String) -> Unit,
    onSubtitleLangChange: (String) -> Unit,
    onCustomTermsChange: (String) -> Unit,
    onSubtitleConfigChange: (enabled: Boolean, style: String, position: String) -> Unit,
    onContinueClick: () -> Unit
) {
    val scrollState = rememberScrollState()

    val voices = listOf(
        Triple("Marcus", "Male", "Professional Male"),
        Triple("Sarah", "Female", "Friendly Female"),
        Triple("David", "Male", "Calm Educational"),
        Triple("Alex", "Female", "Energetic & Direct")
    )

    val languages = listOf("English", "Twi", "French", "Arabic", "Spanish", "Portuguese", "German")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Guidance Box
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Slate900),
            border = androidx.compose.foundation.BorderStroke(1.dp, Slate800),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "VOICE, ACCENT & MULTI-LANGUAGE SUBTITLES",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Cyan400,
                    letterSpacing = 0.8.sp
                )
                Text(
                    text = "Narration language and subtitle language are configured independently. For example, narration in English with accurate Twi or French subtitles.",
                    fontSize = 12.sp,
                    color = Slate300
                )
            }
        }

        // Voice Selection Section
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Slate900),
            border = androidx.compose.foundation.BorderStroke(1.dp, Slate800),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "NARRATION VOICE ACTOR",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate400,
                    letterSpacing = 0.6.sp
                )

                // Voice Options
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    voices.forEach { (vName, vGender, vDesc) ->
                        val isSelected = voiceName.contains(vName)
                        Surface(
                            onClick = {
                                onVoiceChange(
                                    "$vName ($vDesc)",
                                    vGender,
                                    voiceAccent,
                                    speakingStyle,
                                    speakingSpeed
                                )
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) Slate800 else Slate950,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) Indigo500 else Slate800
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("voice_actor_${vName.lowercase()}")
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) Indigo600 else Slate800),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = null,
                                        tint = if (isSelected) Slate50 else Indigo400,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = vName,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Slate50 else Slate300
                                )
                                Text(
                                    text = vGender,
                                    fontSize = 10.sp,
                                    color = Slate500
                                )
                            }
                        }
                    }
                }

                // Speaking Speed Slider
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Speaking Speed", fontSize = 12.sp, color = Slate300)
                        Text(
                            text = String.format("%.1fx", speakingSpeed),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Cyan400
                        )
                    }
                    Slider(
                        value = speakingSpeed,
                        onValueChange = {
                            onVoiceChange(voiceName, voiceGender, voiceAccent, speakingStyle, it)
                        },
                        valueRange = 0.8f..1.4f,
                        steps = 5,
                        colors = SliderDefaults.colors(
                            thumbColor = Indigo400,
                            activeTrackColor = Indigo500
                        ),
                        modifier = Modifier.testTag("speaking_speed_slider")
                    )
                }
            }
        }

        // Separate Language Selectors
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Slate900),
            border = androidx.compose.foundation.BorderStroke(1.dp, Slate800),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "SEPARATE LANGUAGE CONTROLS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate400,
                    letterSpacing = 0.6.sp
                )

                // Narration Language Dropdown / Chips
                Column {
                    Text(text = "Narration Language", fontSize = 12.sp, color = Slate300, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("English", "French", "Spanish", "German").forEach { lang ->
                            val isSel = narrationLang == lang
                            Surface(
                                onClick = { onNarrationLangChange(lang) },
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSel) Indigo600 else Slate800,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = lang,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isSel) Slate50 else Slate300,
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // Subtitle Language Dropdown / Chips
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Subtitle Language", fontSize = 12.sp, color = Slate300, fontWeight = FontWeight.SemiBold)
                        Surface(
                            color = Cyan500.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "Localized Dual-Track",
                                fontSize = 9.sp,
                                color = Cyan400,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Twi", "English", "French", "Spanish").forEach { lang ->
                            val isSel = subtitleLang == lang
                            Surface(
                                onClick = { onSubtitleLangChange(lang) },
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSel) Cyan500 else Slate800,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("subtitle_lang_${lang.lowercase()}")
                            ) {
                                Text(
                                    text = lang,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isSel) Slate950 else Slate300,
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // Application Language (Auto-detected)
                Surface(
                    color = Slate950,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Application UI Language:", fontSize = 11.sp, color = Slate400)
                        Text(
                            text = "English (Detected automatically from DOM/i18n)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Emerald400
                        )
                    }
                }

                // Never Translate Custom Terminology
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Never Translate Custom Terminology",
                        fontSize = 12.sp,
                        color = Slate300,
                        fontWeight = FontWeight.SemiBold
                    )
                    OutlinedTextField(
                        value = customTerminology,
                        onValueChange = onCustomTermsChange,
                        placeholder = { Text("e.g. SHARIF AI, SHARIF TECHNOLOGIES, Paystack") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("never_translate_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Indigo500,
                            unfocusedBorderColor = Slate700
                        )
                    )
                    Text(
                        text = "Names and brand trademarks listed here will remain intact across all language translations.",
                        fontSize = 10.sp,
                        color = Slate400
                    )
                }
            }
        }

        // Subtitle Options & Styling
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Slate900),
            border = androidx.compose.foundation.BorderStroke(1.dp, Slate800),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Add subtitles?", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Slate100)
                    Switch(
                        checked = hasSubtitles,
                        onCheckedChange = { onSubtitleConfigChange(it, subtitleStyle, subtitlePosition) },
                        colors = SwitchDefaults.colors(checkedThumbColor = Indigo400, checkedTrackColor = Indigo600)
                    )
                }

                if (hasSubtitles) {
                    Text(text = "Subtitle Style", fontSize = 11.sp, color = Slate400)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Minimal", "Professional", "Large / accessible", "Social media style").forEach { style ->
                            val isSel = subtitleStyle == style
                            Surface(
                                onClick = { onSubtitleConfigChange(true, style, subtitlePosition) },
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSel) Slate800 else Slate950,
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) Indigo400 else Slate800),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = style.split(" ").first(),
                                    fontSize = 10.sp,
                                    color = if (isSel) Indigo300 else Slate300,
                                    modifier = Modifier.padding(vertical = 6.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }

        // Continue Button
        Button(
            onClick = onContinueClick,
            colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp)
                .testTag("continue_to_appearance_btn")
        ) {
            Text(
                text = "Continue to Appearance & Branding",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(imageVector = Icons.Default.ArrowForward, contentDescription = null)
        }
    }
}
