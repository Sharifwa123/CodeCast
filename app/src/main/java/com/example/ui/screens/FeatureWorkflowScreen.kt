package com.example.ui.screens

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DetectedFeature
import com.example.data.model.DetectedWorkflow
import com.example.ui.theme.*

@Composable
fun FeatureWorkflowScreen(
    features: List<DetectedFeature>,
    selectedFeatureName: String,
    selectedWorkflow: DetectedWorkflow?,
    onSelectFeature: (String) -> Unit,
    onSelectWorkflow: (DetectedWorkflow) -> Unit,
    onContinueClick: () -> Unit
) {
    val scrollState = rememberScrollState()
    val activeFeature = features.find { it.name == selectedFeatureName } ?: features.firstOrNull()

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
                    text = "Discovered features & workflows",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Cyan400,
                )
                Text(
                    text = "All options below were verified from your uploaded codebase. Select a feature to choose which verified user journey to generate.",
                    fontSize = 13.sp,
                    color = Slate300
                )
            }
        }

        // Features Selector (Horizontal chips or vertical list)
        Text(
            text = "Available features in your project",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Slate400,
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            features.forEach { feature ->
                val isSelected = feature.name == selectedFeatureName
                Surface(
                    onClick = { onSelectFeature(feature.name) },
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) Slate800 else Slate900,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSelected) Indigo500 else Slate800
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("feature_tile_${feature.name.lowercase().replace(" ", "_")}")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) Indigo600 else Slate850),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = getFeatureIcon(feature.iconName),
                                    contentDescription = null,
                                    tint = if (isSelected) Slate50 else Indigo400,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = feature.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Slate50 else Slate200
                                )
                                Text(
                                    text = "${feature.workflows.size} verified workflows",
                                    fontSize = 12.sp,
                                    color = Slate400
                                )
                            }
                        }

                        RadioButton(
                            selected = isSelected,
                            onClick = { onSelectFeature(feature.name) },
                            colors = RadioButtonDefaults.colors(selectedColor = Indigo500)
                        )
                    }
                }
            }
        }

        // Workflows for Selected Feature
        activeFeature?.let { feat ->
            Text(
                text = "${feat.name.uppercase()} WORKFLOWS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Slate400,
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                feat.workflows.forEach { wf ->
                    val isWfSelected = selectedWorkflow?.id == wf.id
                    Surface(
                        onClick = { onSelectWorkflow(wf) },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isWfSelected) Slate800 else Slate900,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isWfSelected) Cyan400 else Slate800
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("workflow_item_${wf.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = wf.name,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isWfSelected) Slate50 else Slate100
                                    )
                                    Surface(
                                        color = Emerald500.copy(alpha = 0.16f),
                                        shape = RoundedCornerShape(3.dp)
                                    ) {
                                        Text(
                                            text = "${wf.defaultSteps.size} steps",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Emerald400,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = wf.description,
                                    fontSize = 12.sp,
                                    color = Slate400,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }

                            RadioButton(
                                selected = isWfSelected,
                                onClick = { onSelectWorkflow(wf) },
                                colors = RadioButtonDefaults.colors(selectedColor = Cyan400)
                            )
                        }
                    }
                }
            }
        }

        // Continue Button
        Button(
            onClick = onContinueClick,
            enabled = selectedWorkflow != null,
            colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp)
                .testTag("continue_to_step_selection_btn")
        ) {
            Text(
                text = "Review Discovered Steps (${selectedWorkflow?.defaultSteps?.size ?: 0} Steps)",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(imageVector = Icons.Default.ArrowForward, contentDescription = null)
        }
    }
}

private fun getFeatureIcon(name: String): ImageVector = when (name) {
    "security" -> Icons.Default.Security
    "inventory" -> Icons.Default.Inventory2
    "receipt_long" -> Icons.Default.ReceiptLong
    "chat" -> Icons.Default.Chat
    "credit_card" -> Icons.Default.CreditCard
    else -> Icons.Default.Category
}
