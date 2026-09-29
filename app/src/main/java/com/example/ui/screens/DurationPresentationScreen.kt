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

                    // 1. VIRTUAL ME / AI AVATAR LIKENESS
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "1. VIRTUAL ME — FACE & AVATAR LIKENESS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Cyan400,
                            letterSpacing = 0.5.sp
                        )
                        Surface(color = Cyan500.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                            Text(
                                text = "DEEPFAKE AVATAR",
                                fontSize = 9.sp,
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
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (presenterFaceUri != null) Emerald400 else Slate100
                                )
                                Text(
                                    text = if (presenterFaceUri != null)
                                        "Facial mesh & gaze calibrated for tutorial PiP presentation."
                                    else
                                        "Select your selfie or photo to generate your talking head presenter clone.",
                                    fontSize = 11.sp,
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
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    // Avatar Framing & Placement
                    Text(text = "Presenter Framing & Placement", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate300)
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
                                    Text(label, fontSize = 10.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal, color = if (isSel) Slate50 else Slate400)
                                }
                            }
                        }
                    }

                    // 2. EXACT VOICE CLONING (DEEPFAKE VOICE)
                    HorizontalDivider(color = Slate700)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "2. EXACT VOICE CLONE — VIRTUAL ME",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Indigo300,
                            letterSpacing = 0.5.sp
                        )
                        Surface(color = Indigo500.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                            Text(
                                text = "VOCAL TIMBRE CLONE",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Indigo300,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Slate950,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (hasClonedVoice) Emerald400 else Slate700),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (hasClonedVoice) Emerald400.copy(alpha = 0.2f) else Indigo500.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (hasClonedVoice) Icons.Default.GraphicEq else Icons.Default.Mic,
                                        contentDescription = null,
                                        tint = if (hasClonedVoice) Emerald400 else Indigo400,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = if (hasClonedVoice) "✓ Voice Clone Active ($clonedVoiceName)" else "Record or Upload Your Voice Sample",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (hasClonedVoice) Emerald400 else Slate100
                                    )
                                    Text(
                                        text = if (hasClonedVoice)
                                            "Neural voice synthesizer matches your exact timbre and inflection."
                                        else
                                            "Read a short 5-second sentence to capture your authentic voice profile.",
                                        fontSize = 11.sp,
                                        color = Slate400
                                    )
                                }
                            }

                            // Calibration Script Prompt
                            Surface(color = Slate900, shape = RoundedCornerShape(6.dp), modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(text = "CALIBRATION READING SENTENCE:", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Cyan400)
                                    Text(
                                        text = "\"Hi, welcome to this video tutorial! I'll guide you step by step through our app features today.\"",
                                        fontSize = 11.sp,
                                        color = Slate200,
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                    )
                                }
                            }

                            // Recording Controls
                            if (isRecordingVoice) {
                                // Active Recording State
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Rose500.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clip(CircleShape)
                                                .background(Rose500)
                                        )
                                        Text(text = "RECORDING MIC: 00:0${recordingDurationSec}s", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Rose400)
                                        Text(text = "|||||||||||", fontSize = 12.sp, color = Cyan400)
                                    }

                                    Button(
                                        onClick = onStopRecordVoice,
                                        colors = ButtonDefaults.buttonColors(containerColor = Rose600),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        modifier = Modifier.defaultMinSize(minHeight = 32.dp)
                                    ) {
                                        Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Stop & Clone", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            } else {
                                // Standby Buttons
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = onStartRecordVoice,
                                        colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
                                        modifier = Modifier.weight(1f).defaultMinSize(minHeight = 36.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Record 5s Voice Sample", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    }

                                    OutlinedButton(
                                        onClick = onPickAudioFile,
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate200),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Slate700),
                                        modifier = Modifier.weight(1f).defaultMinSize(minHeight = 36.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Upload Audio (.mp3/.wav)", fontSize = 11.sp)
                                    }
                                }
                            }

                            // If voice is cloned, show test preview button and pitch slider
                            if (hasClonedVoice) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = "Cloned Pitch Tuning: ${String.format("%.2f", clonedVoicePitch)}x", fontSize = 11.sp, color = Slate300)
                                        OutlinedButton(
                                            onClick = onTestVoicePreview,
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Cyan400),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, Cyan500.copy(alpha = 0.5f)),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier.defaultMinSize(minHeight = 28.dp)
                                        ) {
                                            Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(if (isTestingVoiceAudio) "Speaking..." else "Test Preview", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Slider(
                                        value = clonedVoicePitch,
                                        onValueChange = onTuneVoicePitch,
                                        valueRange = 0.8f..1.2f,
                                        colors = SliderDefaults.colors(thumbColor = Cyan400, activeTrackColor = Cyan400)
                                    )
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
                                text = "I confirm ownership or proper consent for the uploaded face likeness and cloned voice profile for tutorial synthesis.",
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
