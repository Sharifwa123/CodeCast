package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun AppearanceBrandingScreen(
    cursorStyle: String,
    calloutStyle: String,
    zoomStyle: String,
    transitionStyle: String,
    backgroundMusic: String,
    musicVolume: Float,
    watermarkOption: String,
    onVisualChange: (cursor: String, callout: String, zoom: String, transition: String) -> Unit,
    onMusicChange: (music: String, volume: Float) -> Unit,
    onWatermarkChange: (String) -> Unit,
    onContinueClick: () -> Unit
) {
    val scrollState = rememberScrollState()

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
                    text = "Visual polish & branding",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Cyan400,
                )
                Text(
                    text = "Fine-tune cursor indicators, zoom focus, audio bed, and watermarks to match your company brand guidelines.",
                    fontSize = 13.sp,
                    color = Slate300
                )
            }
        }

        // Visual Style Card
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
                    text = "Screen visual styling",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate400,
                )

                // Cursor Style
                Text(text = "Cursor Effect", fontSize = 12.sp, color = Slate300)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Normal", "Highlighted", "Circle click indicator").forEach { cur ->
                        val isSel = cursorStyle == cur
                        Surface(
                            onClick = { onVisualChange(cur, calloutStyle, zoomStyle, transitionStyle) },
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSel) Slate800 else Slate950,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) Indigo400 else Slate800),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = cur,
                                fontSize = 11.sp,
                                color = if (isSel) Indigo300 else Slate300,
                                modifier = Modifier.padding(vertical = 8.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                // Callouts & Zoom
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Callout Details", fontSize = 12.sp, color = Slate300)
                        Spacer(modifier = Modifier.height(4.dp))
                        listOf("Minimal", "Detailed").forEach { c ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                RadioButton(
                                    selected = calloutStyle == c,
                                    onClick = { onVisualChange(cursorStyle, c, zoomStyle, transitionStyle) },
                                    colors = RadioButtonDefaults.colors(selectedColor = Indigo400)
                                )
                                Text(text = c, fontSize = 12.sp, color = Slate200)
                            }
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Camera Zoom", fontSize = 12.sp, color = Slate300)
                        Spacer(modifier = Modifier.height(4.dp))
                        listOf("Automatic", "Manual").forEach { z ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                RadioButton(
                                    selected = zoomStyle == z,
                                    onClick = { onVisualChange(cursorStyle, calloutStyle, z, transitionStyle) },
                                    colors = RadioButtonDefaults.colors(selectedColor = Cyan400)
                                )
                                Text(text = z, fontSize = 12.sp, color = Slate200)
                            }
                        }
                    }
                }
            }
        }

        // Background Music Card
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
                Text(
                    text = "Background music bed",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate400,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("None", "Subtle", "Corporate", "Educational").forEach { m ->
                        val isSel = backgroundMusic == m
                        Surface(
                            onClick = { onMusicChange(m, musicVolume) },
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSel) Slate800 else Slate950,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) Indigo400 else Slate800),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = m,
                                fontSize = 12.sp,
                                color = if (isSel) Indigo300 else Slate300,
                                modifier = Modifier.padding(vertical = 8.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                if (backgroundMusic != "None") {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Music Bed Volume (Voice remains dominant)", fontSize = 12.sp, color = Slate400)
                            Text(text = "${(musicVolume * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Slate200)
                        }
                        Slider(
                            value = musicVolume,
                            onValueChange = { onMusicChange(backgroundMusic, it) },
                            valueRange = 0.05f..0.5f,
                            colors = SliderDefaults.colors(
                                thumbColor = Indigo400,
                                activeTrackColor = Indigo500
                            )
                        )
                    }
                }
            }
        }

        // Branding & Watermark
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
                Text(
                    text = "Branding & watermark",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate400,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("None", "Logo", "Company name").forEach { wm ->
                        val isSel = watermarkOption == wm
                        Surface(
                            onClick = { onWatermarkChange(wm) },
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSel) Slate800 else Slate950,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) Cyan400 else Slate800),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = wm,
                                fontSize = 12.sp,
                                color = if (isSel) Cyan300 else Slate300,
                                modifier = Modifier.padding(vertical = 8.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
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
                .testTag("continue_to_review_btn")
        ) {
            Text(
                text = "Continue to Review Tutorial",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(imageVector = Icons.Default.ArrowForward, contentDescription = null)
        }
    }
}
