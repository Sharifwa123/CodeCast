package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun DurationPresentationScreen(
    selectedDuration: String,
    selectedPresentationType: String,
    selectedPresenterOption: String,
    presenterLikenessConsent: Boolean,
    onSelectDuration: (String) -> Unit,
    onSelectPresentationType: (String) -> Unit,
    onSelectPresenterOption: (String) -> Unit,
    onToggleConsent: (Boolean) -> Unit,
    onContinueClick: () -> Unit
) {
    val scrollState = rememberScrollState()

    val durations = listOf(
        "Quick — 30–60 seconds" to "Fast, punchy summary of key actions.",
        "Standard — 1–3 minutes" to "Balanced walkthrough with visual cursor guidance.",
        "Detailed — 3–7 minutes" to "Deep dive with comprehensive explanations and context.",
        "Full walkthrough — 7+ minutes" to "End-to-end exploration including edge cases."
    )

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
                    text = "PACING & VISUAL PRESENTATION",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Cyan400,
                    letterSpacing = 0.8.sp
                )
                Text(
                    text = "Select your target video length and presentation layout. The engine determines optimal scene timings automatically without forcing rigid limits.",
                    fontSize = 12.sp,
                    color = Slate300
                )
            }
        }

        // Duration Section
        Text(
            text = "TUTORIAL LENGTH",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Slate400,
            letterSpacing = 0.6.sp
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            durations.forEach { (dur, desc) ->
                val isSelected = selectedDuration == dur
                Surface(
                    onClick = { onSelectDuration(dur) },
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) Slate800 else Slate900,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSelected) Indigo500 else Slate800
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("duration_option_${dur.take(5).lowercase().trim()}")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = dur,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Slate50 else Slate200
                            )
                            Text(
                                text = desc,
                                fontSize = 11.sp,
                                color = Slate400,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        RadioButton(
                            selected = isSelected,
                            onClick = { onSelectDuration(dur) },
                            colors = RadioButtonDefaults.colors(selectedColor = Indigo500)
                        )
                    }
                }
            }
        }

        // 3 Large Presentation Choices
        Text(
            text = "PRESENTATION STYLE",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Slate400,
            letterSpacing = 0.6.sp
        )

        // Option 1: Your Face + Voice
        val isFaceSelected = selectedPresentationType == "FACE_AND_VOICE"
        Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = if (isFaceSelected) Slate800 else Slate900),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isFaceSelected) Indigo500 else Slate800
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("presentation_option_face_voice")
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Indigo600.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.Face, contentDescription = null, tint = Indigo400)
                        }
                        Column {
                            Text(
                                text = "OPTION 1 — YOUR FACE + VOICE",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate50
                            )
                            Text(
                                text = "Presenter explains tutorial in picture-in-picture while app is shown.",
                                fontSize = 11.sp,
                                color = Slate400
                            )
                        }
                    }

                    RadioButton(
                        selected = isFaceSelected,
                        onClick = { onSelectPresentationType("FACE_AND_VOICE") },
                        colors = RadioButtonDefaults.colors(selectedColor = Indigo500)
                    )
                }

                if (isFaceSelected) {
                    HorizontalDivider(color = Slate700)

                    Text(
                        text = "Presenter Options",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate300
                    )

                    val presenterModes = listOf(
                        "Talking head circle (PiP)",
                        "Presenter beside application",
                        "Full-screen presenter introduction",
                        "Presenter introduction + walkthrough"
                    )

                    presenterModes.forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = mode, fontSize = 12.sp, color = Slate200)
                            RadioButton(
                                selected = selectedPresenterOption == mode,
                                onClick = { onSelectPresenterOption(mode) },
                                colors = RadioButtonDefaults.colors(selectedColor = Indigo400)
                            )
                        }
                    }

                    // Likeness Consent
                    Surface(
                        color = Slate950,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Checkbox(
                                checked = presenterLikenessConsent,
                                onCheckedChange = onToggleConsent,
                                colors = CheckboxDefaults.colors(checkedColor = Indigo500)
                            )
                            Text(
                                text = "I confirm ownership or proper consent for the uploaded likeness and voice sample.",
                                fontSize = 10.sp,
                                color = Slate300
                            )
                        }
                    }
                }
            }
        }

        // Option 2: Voice Only
        val isVoiceOnlySelected = selectedPresentationType == "VOICE_ONLY"
        Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = if (isVoiceOnlySelected) Slate800 else Slate900),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isVoiceOnlySelected) Cyan400 else Slate800
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("presentation_option_voice_only")
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Cyan500.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.LaptopMac, contentDescription = null, tint = Cyan400)
                    }
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "OPTION 2 — VOICE ONLY",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate50
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Emerald500.copy(alpha = 0.2f))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text("RECOMMENDED", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Emerald400)
                            }
                        }
                        Text(
                            text = "Direct screen recording with cursor movement, zoom highlights, action callouts, and clean voice narration.",
                            fontSize = 11.sp,
                            color = Slate400,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                RadioButton(
                    selected = isVoiceOnlySelected,
                    onClick = { onSelectPresentationType("VOICE_ONLY") },
                    colors = RadioButtonDefaults.colors(selectedColor = Cyan400)
                )
            }
        }

        // Option 3: Illustrated
        val isIllustratedSelected = selectedPresentationType == "ILLUSTRATED"
        Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = if (isIllustratedSelected) Slate800 else Slate900),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isIllustratedSelected) Purple500 else Slate800
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("presentation_option_illustrated")
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Purple500.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Palette, contentDescription = null, tint = Purple500)
                    }
                    Column {
                        Text(
                            text = "OPTION 3 — ILLUSTRATED",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate50
                        )
                        Text(
                            text = "Instructional diagrams, workflow arrows, UI component callouts, and animated schematics.",
                            fontSize = 11.sp,
                            color = Slate400,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                RadioButton(
                    selected = isIllustratedSelected,
                    onClick = { onSelectPresentationType("ILLUSTRATED") },
                    colors = RadioButtonDefaults.colors(selectedColor = Purple500)
                )
            }
        }

        // Continue Button
        Button(
            onClick = onContinueClick,
            colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp)
                .testTag("continue_to_voice_btn")
        ) {
            Text(
                text = "Continue to Voice Selection",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(imageVector = Icons.Default.ArrowForward, contentDescription = null)
        }
    }
}
