package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.GeneratedSceneEntity
import com.example.data.model.TutorialPlanEntity
import com.example.ui.theme.*

@Composable
fun VideoPlayerAndEditorView(
    tutorial: TutorialPlanEntity,
    scenes: List<GeneratedSceneEntity>,
    currentSceneIndex: Int,
    playbackSecond: Float,
    isPlaying: Boolean,
    selectedScene: GeneratedSceneEntity?,
    presenterFaceUri: String? = null,
    presenterFraming: String = "circle",
    hasClonedVoice: Boolean = false,
    clonedVoiceName: String = "",
    onPlayPauseToggle: () -> Unit,
    onSeekScene: (Int) -> Unit,
    onSelectEditorScene: (GeneratedSceneEntity) -> Unit,
    onUpdateScene: (narration: String, subtitle: String, duration: Int, callout: String) -> Unit,
    onRegenerateScene: (GeneratedSceneEntity) -> Unit,
    onExportRequested: (format: String) -> Unit
) {
    val activeScene = scenes.getOrNull(currentSceneIndex) ?: scenes.firstOrNull()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // VIDEO PLAYER VIEWPORT
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Slate950),
            border = androidx.compose.foundation.BorderStroke(1.dp, Slate800),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("video_player_card")
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Top Player Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Slate900)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isPlaying) Emerald400 else Amber500)
                        )
                        Text(
                            text = if (isPlaying) "PLAYING (1080p 60fps)" else "PAUSED",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate300
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = Slate800,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "${tutorial.presentationType.replace('_', ' ')}",
                                fontSize = 9.sp,
                                color = Cyan400,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Surface(
                            color = Slate800,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "Subtitles: ${tutorial.subtitleLang}",
                                fontSize = 9.sp,
                                color = Slate300,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // SCREEN STAGE
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .background(Slate950),
                    contentAlignment = Alignment.Center
                ) {
                    val context = LocalContext.current
                    val drawableResId = remember(activeScene?.screenDrawableName) {
                        val name = activeScene?.screenDrawableName ?: "demo_screen_auth"
                        val id = context.resources.getIdentifier(name, "drawable", context.packageName)
                        if (id != 0) id else 0
                    }

                    // Background UI Screen
                    if (drawableResId != 0) {
                        Image(
                            painter = painterResource(id = drawableResId),
                            contentDescription = activeScene?.title ?: "Application Screen",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        listOf(Slate900, Slate850)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.LaptopMac,
                                    contentDescription = null,
                                    tint = Indigo400,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = activeScene?.title ?: "Screen View",
                                    color = Slate200,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // Overlay Vignette
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Black.copy(alpha = 0.35f),
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.75f)
                                    )
                                )
                            )
                    )

                    // PRESENTATION STYLE OVERLAYS:
                    // 1. Watermark
                    if (tutorial.watermarkOption != "None") {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.Black.copy(alpha = 0.6f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "CodeCast • PayFlex",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }

                    // 2. Cursor Click Indicator Animation
                    val infiniteTransition = rememberInfiniteTransition(label = "cursor")
                    val pulseScale by infiniteTransition.animateFloat(
                        initialValue = 0.8f,
                        targetValue = 1.3f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(800, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pulse"
                    )

                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset(x = 24.dp, y = (-12).dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .scale(if (isPlaying) pulseScale else 1f)
                                .clip(CircleShape)
                                .background(Cyan400.copy(alpha = 0.35f))
                        )
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .align(Alignment.Center)
                                .clip(CircleShape)
                                .background(Cyan400)
                                .border(2.dp, Color.White, CircleShape)
                        )
                    }

                    // 3. Action Callout Banner
                    if (activeScene?.calloutText?.isNotEmpty() == true) {
                        Surface(
                            color = Slate900.copy(alpha = 0.9f),
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Indigo500.copy(alpha = 0.6f)),
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TouchApp,
                                    contentDescription = null,
                                    tint = Indigo400,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = activeScene.calloutText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Slate50
                                )
                            }
                        }
                    }

                    // 4. Presenter Avatar PiP (For "Your Face + Voice")
                    if (tutorial.presentationType == "FACE_AND_VOICE") {
                        val avatarShape = if (presenterFraming == "window") RoundedCornerShape(8.dp) else CircleShape

                        // Speaking wave animation when video is playing
                        val infiniteTransition = rememberInfiniteTransition(label = "speech")
                        val borderAlpha by infiniteTransition.animateFloat(
                            initialValue = 0.5f,
                            targetValue = 1.0f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(600, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "borderGlow"
                        )

                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(12.dp),
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Card(
                                shape = avatarShape,
                                border = androidx.compose.foundation.BorderStroke(
                                    2.dp,
                                    if (isPlaying) Cyan400.copy(alpha = borderAlpha) else Cyan400
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                                modifier = Modifier.size(68.dp)
                            ) {
                                if (presenterFaceUri != null) {
                                    AsyncImage(
                                        model = presenterFaceUri,
                                        contentDescription = "Virtual Me Avatar",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    val presenterResId = remember {
                                        val id = context.resources.getIdentifier("demo_presenter", "drawable", context.packageName)
                                        if (id != 0) id else 0
                                    }
                                    if (presenterResId != 0) {
                                        Image(
                                            painter = painterResource(id = presenterResId),
                                            contentDescription = "Presenter Likeness",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Indigo600),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = null,
                                                tint = Color.White
                                            )
                                        }
                                    }
                                }
                            }

                            // Presenter Badge
                            Surface(
                                color = Slate950.copy(alpha = 0.85f),
                                shape = RoundedCornerShape(4.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (hasClonedVoice) Emerald400 else Cyan500)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(if (isPlaying) Emerald400 else Slate400)
                                    )
                                    Text(
                                        text = if (presenterFaceUri != null) "Virtual Me" else "AI Presenter",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (hasClonedVoice) Emerald300 else Cyan300
                                    )
                                }
                            }
                        }
                    }

                    // 5. Subtitles Bar (Overlay)
                    if (tutorial.hasSubtitles && activeScene?.subtitleText?.isNotEmpty() == true) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.85f),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .align(if (tutorial.subtitlePosition == "Top") Alignment.TopCenter else Alignment.BottomCenter)
                                .padding(
                                    bottom = if (tutorial.subtitlePosition == "Top") 0.dp else 12.dp,
                                    top = if (tutorial.subtitlePosition == "Top") 12.dp else 0.dp,
                                    start = 24.dp,
                                    end = 24.dp
                                )
                        ) {
                            Text(
                                text = activeScene.subtitleText,
                                fontSize = when (tutorial.subtitleStyle) {
                                    "Large / accessible" -> 14.sp
                                    "Social media style" -> 13.sp
                                    else -> 12.sp
                                },
                                fontWeight = FontWeight.SemiBold,
                                color = if (tutorial.subtitleStyle == "Social media style") Amber500 else Color.White,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // CONTROLS & TIMELINE
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Slate900)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val totalDuration = scenes.sumOf { it.durationSeconds }.coerceAtLeast(1)
                    val elapsedSoFar = scenes.take(currentSceneIndex).sumOf { it.durationSeconds } + playbackSecond
                    val progressFraction = (elapsedSoFar / totalDuration.toFloat()).coerceIn(0f, 1f)

                    // Scrubber Bar
                    Slider(
                        value = progressFraction,
                        onValueChange = {},
                        enabled = false,
                        colors = SliderDefaults.colors(
                            thumbColor = Indigo400,
                            activeTrackColor = Indigo500,
                            inactiveTrackColor = Slate700
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = onPlayPauseToggle,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Indigo500)
                                    .testTag("play_pause_button")
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    tint = Slate50,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Text(
                                text = "${elapsedSoFar.toInt()}s / ${totalDuration}s",
                                fontSize = 12.sp,
                                color = Slate300,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Voice and Narration Badge
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = "Voice Audio",
                                tint = Emerald400,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = tutorial.voiceName.split(" ").firstOrNull() ?: "AI Voice",
                                fontSize = 11.sp,
                                color = Slate300
                            )
                        }
                    }
                }
            }
        }

        // SCENE CAROUSEL / SELECTOR
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "TUTORIAL SCENES (${scenes.size})",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Slate400,
                letterSpacing = 0.5.sp
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                scenes.forEachIndexed { idx, scene ->
                    val isSelected = scene.id == (selectedScene?.id ?: activeScene?.id)
                    Surface(
                        onClick = {
                            onSeekScene(idx)
                            onSelectEditorScene(scene)
                        },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) Slate800 else Slate900,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) Indigo500 else Slate800
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("scene_pill_$idx")
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Scene ${idx + 1}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Indigo400 else Slate300
                            )
                            Text(
                                text = "${scene.durationSeconds}s",
                                fontSize = 10.sp,
                                color = Slate400
                            )
                        }
                    }
                }
            }
        }

        // SIMPLE VIDEO EDITOR PANEL
        selectedScene?.let { scene ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900),
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate800),
                modifier = Modifier.fillMaxWidth()
            ) {
                var editNarration by remember(scene.id) { mutableStateOf(scene.narrationScript) }
                var editSubtitle by remember(scene.id) { mutableStateOf(scene.subtitleText) }
                var editCallout by remember(scene.id) { mutableStateOf(scene.calloutText) }
                var editDuration by remember(scene.id) { mutableIntStateOf(scene.durationSeconds) }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.EditNote,
                                contentDescription = "Editor",
                                tint = Indigo400
                            )
                            Text(
                                text = "Scene #${scene.sceneOrder} Editor: ${scene.title}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate100
                            )
                        }

                        Button(
                            onClick = { onRegenerateScene(scene) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Indigo600.copy(alpha = 0.2f),
                                contentColor = Indigo400
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Indigo500.copy(alpha = 0.4f)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier
                                .defaultMinSize(minHeight = 32.dp)
                                .testTag("regenerate_single_scene_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Regenerate Scene",
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Regenerate Scene Only", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    // Narration Script input
                    OutlinedTextField(
                        value = editNarration,
                        onValueChange = {
                            editNarration = it
                            onUpdateScene(it, editSubtitle, editDuration, editCallout)
                        },
                        label = { Text("Narration Script (${tutorial.narrationLang})", fontSize = 11.sp) },
                        maxLines = 3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("editor_narration_field"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Indigo500,
                            unfocusedBorderColor = Slate700
                        )
                    )

                    // Subtitle Text input
                    OutlinedTextField(
                        value = editSubtitle,
                        onValueChange = {
                            editSubtitle = it
                            onUpdateScene(editNarration, it, editDuration, editCallout)
                        },
                        label = { Text("Subtitle Text (${tutorial.subtitleLang})", fontSize = 11.sp) },
                        maxLines = 2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("editor_subtitle_field"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Cyan500,
                            unfocusedBorderColor = Slate700
                        )
                    )

                    // Scene Duration and Callout Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = editCallout,
                            onValueChange = {
                                editCallout = it
                                onUpdateScene(editNarration, editSubtitle, editDuration, it)
                            },
                            label = { Text("Action Callout", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Indigo500,
                                unfocusedBorderColor = Slate700
                            )
                        )

                        Column(
                            modifier = Modifier
                                .width(120.dp)
                                .padding(top = 8.dp)
                        ) {
                            Text(
                                text = "Duration: ${editDuration}s",
                                fontSize = 11.sp,
                                color = Slate300
                            )
                            Slider(
                                value = editDuration.toFloat(),
                                onValueChange = {
                                    editDuration = it.toInt()
                                    onUpdateScene(editNarration, editSubtitle, it.toInt(), editCallout)
                                },
                                valueRange = 3f..25f,
                                steps = 22,
                                colors = SliderDefaults.colors(
                                    thumbColor = Indigo400,
                                    activeTrackColor = Indigo500
                                )
                            )
                        }
                    }
                }
            }
        }

        // EXPORT & SHARE BUTTONS
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { onExportRequested("MP4") },
                colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 44.dp)
                    .testTag("export_mp4_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Export MP4 (1080p)", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = { onExportRequested("SRT") },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate200),
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate700),
                modifier = Modifier
                    .defaultMinSize(minHeight = 44.dp)
                    .testTag("export_srt_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Subtitles,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Export SRT", fontSize = 12.sp)
            }

            OutlinedButton(
                onClick = { onExportRequested("SHARE") },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Cyan400),
                border = androidx.compose.foundation.BorderStroke(1.dp, Cyan500.copy(alpha = 0.5f)),
                modifier = Modifier
                    .defaultMinSize(minHeight = 44.dp)
                    .testTag("share_link_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Share", fontSize = 12.sp)
            }
        }
    }
}
