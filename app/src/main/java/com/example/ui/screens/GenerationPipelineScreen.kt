package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.GenerationProgressState

@Composable
fun GenerationPipelineScreen(
    progressState: GenerationProgressState,
    onViewResultClick: () -> Unit,
    showRecorder: Boolean = false,
    recorderAspect: Float = 0.5f,
    onWebViewReady: (android.webkit.WebView) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(androidx.compose.foundation.rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Slate900),
            border = androidx.compose.foundation.BorderStroke(1.dp, Slate800),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("generation_pipeline_card")
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Indigo600.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (progressState.isCompleted) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Emerald400,
                            modifier = Modifier.size(32.dp)
                        )
                    } else {
                        CircularProgressIndicator(
                            progress = { progressState.progressPercent },
                            color = Cyan400,
                            trackColor = Slate800,
                            strokeWidth = 4.dp,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }

                if (showRecorder) {
                    Text("Recording the live app. Keep CodeCast open until it finishes.", fontSize = 12.sp, color = Cyan300)
                    androidx.compose.foundation.layout.BoxWithConstraints(
                        modifier = Modifier.fillMaxWidth().height(330.dp).clip(RoundedCornerShape(8.dp)).background(Color.White).testTag("recorder_host")
                    ) {
                        val fullW = maxWidth
                        val fullH = maxWidth * recorderAspect
                        val scale = minOf(1f, 330.dp.value / fullH.value)
                        androidx.compose.ui.viewinterop.AndroidView(
                            factory = { ctx -> android.webkit.WebView(ctx).also(onWebViewReady) },
                            onRelease = { it.destroy() },
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .requiredSize(fullW, fullH)
                                .graphicsLayer(scaleX = scale, scaleY = scale, transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0f))
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (progressState.isCompleted) "Video Generation Complete!" else "Rendering Video Tutorial",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate50
                    )
                    Text(
                        text = progressState.phaseName,
                        fontSize = 13.sp,
                        color = if (progressState.isCompleted) Emerald400 else Cyan300,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                LinearProgressIndicator(
                    progress = { progressState.progressPercent },
                    color = if (progressState.isCompleted) Emerald500 else Indigo500,
                    trackColor = Slate800,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                )

                // Pipeline Stages Checklist
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Slate950, RoundedCornerShape(10.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val running = if (!progressState.isCompleted && progressState.isRunning) listOf(progressState.phaseName) else emptyList()
                    (progressState.completedStages + running).forEach { stage ->
                        val isDone = stage in progressState.completedStages
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (isDone) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Done",
                                        tint = Emerald400,
                                        modifier = Modifier.size(16.dp)
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Slate700)
                                    )
                                }
                                Text(
                                    text = stage,
                                    fontSize = 13.sp,
                                    color = if (isDone) Slate200 else Slate500,
                                    fontWeight = if (isDone) FontWeight.Medium else FontWeight.Normal
                                )
                            }

                            if (isDone) {
                                Text(
                                    text = "✓",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Emerald400
                                )
                            }
                        }
                    }
                }

                if (progressState.isCompleted) {
                    Button(
                        onClick = onViewResultClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 44.dp)
                            .testTag("preview_final_video_btn")
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Open Video Player & Editor", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
