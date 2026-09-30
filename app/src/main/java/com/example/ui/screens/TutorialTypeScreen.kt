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
import com.example.ui.theme.*

data class TutorialTypeItem(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val isPrimaryDefault: Boolean = false
)

@Composable
fun TutorialTypeScreen(
    selectedType: String,
    customPromptInput: String,
    onSelectType: (String) -> Unit,
    onCustomPromptChange: (String) -> Unit,
    onResolveCustomPrompt: () -> Unit,
    onContinueClick: () -> Unit
) {
    val scrollState = rememberScrollState()

    val options = listOf(
        TutorialTypeItem("SIGN_UP", "Sign Up", "Show users how to create an account and register.", Icons.Default.PersonAdd, isPrimaryDefault = true),
        TutorialTypeItem("LOGIN", "Login", "Show users how to sign in via email or SSO.", Icons.Default.Login),
        TutorialTypeItem("GETTING_STARTED", "Getting Started", "Help a new user understand the core interface and dashboard.", Icons.Default.Flag),
        TutorialTypeItem("FEATURE_WALKTHROUGH", "Feature Walkthrough", "Explain how a specific business feature works end-to-end.", Icons.Default.Apps),
        TutorialTypeItem("HOW_TO", "How-To Guide", "Step-by-step instructional guide to complete a concrete task.", Icons.Default.MenuBook),
        TutorialTypeItem("PASSWORD_RESET", "Password Reset", "Show users how to recover their account via email token.", Icons.Default.LockReset),
        TutorialTypeItem("PRODUCT_TOUR", "Product Tour", "Introduce the main value propositions of the product.", Icons.Default.Explore),
        TutorialTypeItem("ADMIN_GUIDE", "Admin Guide", "Explain administrative settings, webhooks, and permissions.", Icons.Default.AdminPanelSettings),
        TutorialTypeItem("DEVELOPER_GUIDE", "Developer Guide", "Explain technical functionality, REST endpoints, and SDKs.", Icons.Default.Terminal),
        TutorialTypeItem("TROUBLESHOOTING", "Troubleshooting", "Show users how to diagnose and solve a common issue.", Icons.Default.BuildCircle),
        TutorialTypeItem("CUSTOM", "Custom Tutorial", "Describe a specific workflow in plain English to auto-discover steps.", Icons.Default.AutoFixHigh)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
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
                    text = "What would you like to teach?",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Cyan400,
                )
                Text(
                    text = "Choose a structured tutorial format. CodeCast automatically inspects the discovered screens and controls matching your selection.",
                    fontSize = 13.sp,
                    color = Slate300
                )
            }
        }

        // Standard Options Grid / List
        options.filter { it.id != "CUSTOM" }.forEach { item ->
            val isSelected = selectedType == item.id
            Surface(
                onClick = { onSelectType(item.id) },
                shape = RoundedCornerShape(10.dp),
                color = if (isSelected) Slate800 else Slate900,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isSelected) Indigo500 else Slate800
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tutorial_type_${item.id.lowercase()}")
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Indigo600 else Slate850),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = null,
                                tint = if (isSelected) Slate50 else Indigo400,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = item.title,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Slate50 else Slate100
                                )
                                if (item.isPrimaryDefault) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(Emerald500.copy(alpha = 0.2f))
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Text("Popular", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Emerald400)
                                    }
                                }
                            }
                            Text(
                                text = item.description,
                                fontSize = 12.sp,
                                color = Slate400,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }

                    RadioButton(
                        selected = isSelected,
                        onClick = { onSelectType(item.id) },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = Indigo500,
                            unselectedColor = Slate600
                        )
                    )
                }
            }
        }

        // CUSTOM TUTORIAL CARD (STRUCTURED FALLBACK)
        val isCustomSelected = selectedType == "CUSTOM"
        Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = if (isCustomSelected) Slate800 else Slate900),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isCustomSelected) Cyan400 else Slate800
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("tutorial_type_custom")
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectType("CUSTOM") },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Cyan500.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoFixHigh,
                                contentDescription = null,
                                tint = Cyan400,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Custom Tutorial (Flexible Fallback)",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCustomSelected) Slate50 else Slate200
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(Slate700)
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text("Fallback", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Slate300)
                                }
                            }
                            Text(
                                text = "Use when your workflow is not listed above.",
                                fontSize = 12.sp,
                                color = Slate400
                            )
                        }
                    }

                    RadioButton(
                        selected = isCustomSelected,
                        onClick = { onSelectType("CUSTOM") },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = Cyan400,
                            unselectedColor = Slate600
                        )
                    )
                }

                if (isCustomSelected) {
                    HorizontalDivider(color = Slate750, modifier = Modifier.padding(vertical = 4.dp))

                    Text(
                        text = "What would you like to teach?",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Slate200
                    )

                    OutlinedTextField(
                        value = customPromptInput,
                        onValueChange = onCustomPromptChange,
                        placeholder = { Text("e.g. Show customers how to download their invoice.") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_tutorial_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Cyan500,
                            unfocusedBorderColor = Slate700
                        )
                    )

                    Text(
                        text = "Example: \"Show customers how to download their invoice.\" The system matches your intent against indexed routes and forms to build verified steps.",
                        fontSize = 12.sp,
                        color = Slate400
                    )

                    Button(
                        onClick = {
                            onResolveCustomPrompt()
                            onContinueClick()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Cyan500),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("discover_custom_workflow_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = Slate950)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Discover Workflow From Application Knowledge",
                            color = Slate950,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // Next Button for standard types
        if (!isCustomSelected) {
            Button(
                onClick = onContinueClick,
                colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("tutorial_type_continue_btn")
            ) {
                Text(
                    text = "Continue to Workflow Selection",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(imageVector = Icons.Default.ArrowForward, contentDescription = null)
            }
        }
    }
}
