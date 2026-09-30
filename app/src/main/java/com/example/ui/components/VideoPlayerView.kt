package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.text.font.FontFamily
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
    onExploreCodeFile: ((String) -> Unit)? = null,
    onPlayPauseToggle: () -> Unit,
    onSeekScene: (Int) -> Unit,
    onSelectEditorScene: (GeneratedSceneEntity) -> Unit,
    onUpdateScene: (narration: String, subtitle: String, duration: Int, callout: String) -> Unit,
    onRegenerateScene: (GeneratedSceneEntity) -> Unit,
    exportProgress: Float? = null,
    exportedVideoPath: String? = null,
    exportedAspect: Float = 16f / 9f,
    onExportRequested: (format: String) -> Unit
) {
    val activeScene = scenes.getOrNull(currentSceneIndex) ?: scenes.firstOrNull()
    var stageMode by remember { mutableStateOf("CODE") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        exportedVideoPath?.let { path ->
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                androidx.compose.ui.viewinterop.AndroidView(
                    factory = { ctx ->
                        android.widget.VideoView(ctx).apply {
                            setMediaController(android.widget.MediaController(ctx).also { it.setAnchorView(this) })
                        }
                    },
                    update = { v -> if (v.tag != path) { v.tag = path; v.setVideoPath(path); v.seekTo(1) } },
                    modifier = Modifier
                        .fillMaxWidth(if (exportedAspect < 1f) 0.62f else 1f)
                        .aspectRatio(exportedAspect)
                        .testTag("exported_video_view")
                )
            }
        }

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

                    // Stage Mode Switcher (Code | Split | UI)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(
                            Triple("CODE", "Code", Icons.Default.Code)
                        ).forEach { (mode, label, icon) ->
                            val isSel = stageMode == mode
                            Surface(
                                onClick = { stageMode = mode },
                                shape = RoundedCornerShape(4.dp),
                                color = if (isSel) Indigo600 else Slate800,
                                modifier = Modifier.testTag("stage_mode_${mode.lowercase()}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(icon, contentDescription = null, tint = if (isSel) Color.White else Slate400, modifier = Modifier.size(11.dp))
                                    Text(
                                        text = label,
                                        fontSize = 9.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSel) Color.White else Slate300
                                    )
                                }
                            }
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

                    when (stageMode) {
                        "CODE" -> {
                            VideoSceneCodeStage(scene = activeScene, isPlaying = isPlaying)
                        }
                        "SPLIT" -> {
                            Row(modifier = Modifier.fillMaxSize()) {
                                Box(modifier = Modifier.weight(0.53f).fillMaxHeight()) {
                                    VideoSceneCodeStage(scene = activeScene, isPlaying = isPlaying)
                                }
                                Box(modifier = Modifier.weight(0.47f).fillMaxHeight()) {
                                    if (drawableResId != 0) {
                                        Image(
                                            painter = painterResource(id = drawableResId),
                                            contentDescription = activeScene?.title ?: "Application Screen",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Slate900, Slate850))),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(text = activeScene?.title ?: "Live UI", color = Slate200, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                        else -> {
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
                        }
                    }
                }

                if (tutorial.hasSubtitles && activeScene?.subtitleText?.isNotEmpty() == true) {
                    Surface(color = Slate900, modifier = Modifier.fillMaxWidth().testTag("subtitle_row")) {
                        Text(
                            text = activeScene.subtitleText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }

                // Source Code Bar & Inspector Trigger
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Slate900.copy(alpha = 0.95f))
                        .border(androidx.compose.foundation.BorderStroke(1.dp, Slate800))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.IntegrationInstructions, contentDescription = null, tint = Cyan400, modifier = Modifier.size(13.dp))
                        Text(
                            text = "Scene Code: ${activeScene?.codeFilePath?.ifEmpty { "src/app/page.tsx" }}",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Slate200,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (onExploreCodeFile != null) {
                        Surface(
                            onClick = { onExploreCodeFile(activeScene?.codeFilePath ?: "src/app/page.tsx") },
                            shape = RoundedCornerShape(4.dp),
                            color = Indigo600,
                            modifier = Modifier.testTag("inspect_scene_code_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Code, contentDescription = null, tint = Color.White, modifier = Modifier.size(11.dp))
                                Text("Inspect Full File", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
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
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = { if (exportProgress == null) onExportRequested("MP4") },
                colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 44.dp)
                    .testTag("export_mp4_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (exportProgress != null) "Rendering ${(exportProgress * 100).toInt()}%" else "Export MP4 (720p)", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { onExportRequested("SRT") },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate200),
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate700),
                modifier = Modifier
                    .weight(1f)
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
                    .weight(1f)
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
}

@Composable
fun VideoSceneCodeStage(
    scene: GeneratedSceneEntity?,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val codePath = scene?.codeFilePath?.ifEmpty { "src/app/api/auth/register/route.ts" } ?: "src/app/page.tsx"
    val snippet = scene?.codeSnippet?.ifEmpty {
        "export async function handleAction() {\n  // Implementation code\n  const res = await api.execute();\n  return res;\n}"
    } ?: ""
    val lines = remember(snippet) { snippet.lines() }
    val highlightedSet = remember(scene?.highlightedLines) {
        scene?.highlightedLines?.split(",")?.mapNotNull { it.trim().toIntOrNull() }?.toSet() ?: setOf(1, 2)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0B132B))
    ) {
        // Tab Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1C2541))
                .padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Code, contentDescription = null, tint = Cyan400, modifier = Modifier.size(12.dp))
                Text(
                    text = codePath,
                    fontSize = 9.5.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Slate200,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Surface(color = Color(0xFF0B132B), shape = RoundedCornerShape(3.dp)) {
                Text(
                    text = "${lines.size} lines",
                    fontSize = 8.sp,
                    color = Slate400,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
        }

        // Code Editor Lines Viewport
        Box(modifier = Modifier.weight(1f)) {
            val vScroll = rememberScrollState()
            Row(modifier = Modifier.fillMaxSize().verticalScroll(vScroll)) {
                // Line numbers gutter
                Column(
                    modifier = Modifier
                        .background(Color(0xFF141D35))
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    lines.indices.forEach { idx ->
                        val lineNum = idx + 1
                        val isHl = highlightedSet.contains(lineNum)
                        Text(
                            text = "$lineNum",
                            fontSize = 8.5.sp,
                            fontFamily = FontFamily.Monospace,
                            color = if (isHl) Cyan400 else Slate600,
                            fontWeight = if (isHl) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }

                // Code lines with syntax highlighting
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    lines.forEachIndexed { idx, line ->
                        val lineNum = idx + 1
                        val isHl = highlightedSet.contains(lineNum)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (isHl) Color(0xFF38BDF8).copy(alpha = 0.16f) else Color.Transparent)
                                .padding(vertical = 0.5.dp)
                        ) {
                            Text(
                                text = highlightSyntax(line),
                                fontSize = 8.5.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // Terminal Drawer at bottom
        if (scene?.terminalOutput?.isNotEmpty() == true) {
            Surface(
                color = Color(0xFF030712),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF1F2937))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.Terminal, contentDescription = null, tint = Emerald400, modifier = Modifier.size(10.dp))
                    Text(
                        text = scene.terminalOutput,
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Emerald300,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
