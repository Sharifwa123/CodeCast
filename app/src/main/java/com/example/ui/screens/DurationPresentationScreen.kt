package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Image
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.theme.*

@Composable
fun DurationPresentationScreen(
    selectedDuration: String,
    selectedPresentationType: String,
    selectedPresenterOption: String,
    presenterLikenessConsent: Boolean,
    presenterFaceUri: String? = null,
    presenterAvatarPreset: String = "custom_face",
    presenterFraming: String = "circle",
    presenterPosition: String = "bottom_right",
    hasClonedVoice: Boolean = false,
    clonedVoiceName: String = "Virtual Me (Sharif Voice Clone)",
    isRecordingVoice: Boolean = false,
    recordingDurationSec: Int = 0,
    clonedVoicePitch: Float = 1.0f,
    isTestingVoiceAudio: Boolean = false,
    onSelectDuration: (String) -> Unit,
    onSelectPresentationType: (String) -> Unit,
    onSelectPresenterOption: (String) -> Unit,
    onToggleConsent: (Boolean) -> Unit,
    onPickFacePhoto: () -> Unit = {},
    onSelectPresetAvatar: (String) -> Unit = {},
    onSetFraming: (String) -> Unit = {},
    onSetPosition: (String) -> Unit = {},
    onStartRecordVoice: () -> Unit = {},
    onStopRecordVoice: () -> Unit = {},
    onPickAudioFile: () -> Unit = {},
    onTuneVoicePitch: (Float) -> Unit = {},
    onTestVoicePreview: () -> Unit = {},
    onContinueClick: () -> Unit,
    extraContent: @Composable ColumnScope.() -> Unit = {}
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
                    text = "Pacing & visual presentation",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Cyan400,
                )
                Text(
                    text = "Select your target video length and presentation layout. The engine determines optimal scene timings automatically without forcing rigid limits.",
                    fontSize = 13.sp,
                    color = Slate300
                )
            }
        }

        // Duration Section
        Text(
            text = "Tutorial length",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Slate400,
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
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Slate50 else Slate200
                            )
                            Text(
                                text = desc,
                                fontSize = 12.sp,
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
            text = "Presentation style",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Slate400,
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
                                text = "Option 1 — your face + voice",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate50
                            )
                            Text(
                                text = "Presenter explains tutorial in picture-in-picture while app is shown.",
                                fontSize = 12.sp,
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

                    // 1. VIRTUAL ME / AI AVATAR LIKENESS
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "1. virtual me — face & avatar likeness",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Cyan400,
                        )
                        Surface(color = Cyan500.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                            Text(
                                text = "Deepfake avatar",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Cyan400,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Uploaded Face Preview Card
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Slate950,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (presenterFaceUri != null) Cyan400 else Slate700),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Avatar Visual Box
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(if (presenterFraming == "window") RoundedCornerShape(8.dp) else CircleShape)
                                    .border(2.dp, if (presenterFaceUri != null) Cyan400 else Indigo500, if (presenterFraming == "window") RoundedCornerShape(8.dp) else CircleShape)
                                    .background(Slate800),
                                contentAlignment = Alignment.Center
                            ) {
                                if (presenterFaceUri != null) {
                                    AsyncImage(
                                        model = presenterFaceUri,
                                        contentDescription = "My Uploaded Face Likeness",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    val context = LocalContext.current
                                    val fallbackRes = remember {
                                        val id = context.resources.getIdentifier("demo_presenter", "drawable", context.packageName)
                                        if (id != 0) id else 0
                                    }
                                    if (fallbackRes != 0) {
                                        Image(
                                            painter = painterResource(id = fallbackRes),
                                            contentDescription = "Presenter Likeness",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Icon(Icons.Default.Person, contentDescription = null, tint = Indigo300, modifier = Modifier.size(32.dp))
                                    }
                                }
                            }

                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = if (presenterFaceUri != null) "✓ My Uploaded Face Active" else "Upload Your Face / Headshot",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (presenterFaceUri != null) Emerald400 else Slate100
                                )
                                Text(
                                    text = if (presenterFaceUri != null)
                                        "Facial mesh & gaze calibrated for tutorial PiP presentation."
                                    else
                                        "Choose a photo to show as the presenter (a circle in the video).",
                                    fontSize = 12.sp,
                                    color = Slate400
                                )

                                Button(
                                    onClick = onPickFacePhoto,
                                    colors = ButtonDefaults.buttonColors(containerColor = if (presenterFaceUri != null) Slate800 else Indigo600),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier
                                        .defaultMinSize(minHeight = 32.dp)
                                        .padding(top = 2.dp)
                                        .testTag("upload_face_btn")
                                ) {
                                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (presenterFaceUri != null) "Change Photo" else "Upload Face Photo (Gallery)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    // Avatar Framing & Placement
                    Text(text = "Presenter Framing & Placement", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Slate300)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            Triple("circle", "Circle (PiP)", Icons.Default.Circle),
                            Triple("window", "Rounded Window", Icons.Default.CropPortrait),
                            Triple("split", "Side Split", Icons.Default.ViewSidebar)
                        ).forEach { (mode, label, icon) ->
                            val isSel = presenterFraming == mode
                            Surface(
                                onClick = { onSetFraming(mode) },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSel) Slate800 else Slate950,
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) Indigo500 else Slate800),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(icon, contentDescription = null, tint = if (isSel) Indigo400 else Slate400, modifier = Modifier.size(16.dp))
                                    Text(label, fontSize = 11.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal, color = if (isSel) Slate50 else Slate400)
                                }
                            }
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
                                text = "I confirm I own, or have consent to use, the photo I add as presenter.",
                                fontSize = 11.sp,
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
                                text = "Option 2 — voice only",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate50
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Emerald500.copy(alpha = 0.2f))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text("Recommended", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Emerald400)
                            }
                        }
                        Text(
                            text = "Direct screen recording with cursor movement, zoom highlights, action callouts, and clean voice narration.",
                            fontSize = 12.sp,
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
                            text = "Option 3 — illustrated",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate50
                        )
                        Text(
                            text = "Instructional diagrams, workflow arrows, UI component callouts, and animated schematics.",
                            fontSize = 12.sp,
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

        extraContent()

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
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(imageVector = Icons.Default.ArrowForward, contentDescription = null)
        }
    }
}
