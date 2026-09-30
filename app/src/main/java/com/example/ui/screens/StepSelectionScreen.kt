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
import com.example.data.model.TutorialStepEntity
import com.example.ui.components.EvidenceBottomSheet
import com.example.ui.components.VerificationStatusBadge
import com.example.ui.theme.*

@Composable
fun StepSelectionScreen(
    workflowName: String?,
    steps: List<TutorialStepEntity>,
    inspectingEvidenceStep: TutorialStepEntity?,
    onToggleCheck: (Int) -> Unit,
    onMoveUp: (Int) -> Unit,
    onMoveDown: (Int) -> Unit,
    onRemoveStep: (Int) -> Unit,
    onUpdateStep: (id: Int, title: String, instruction: String) -> Unit,
    onAddManualStep: (screen: String, action: String, title: String, instruction: String) -> Unit,
    onInspectEvidence: (TutorialStepEntity?) -> Unit,
    onExploreCode: ((String) -> Unit)? = null,
    onContinueClick: () -> Unit
) {
    val scrollState = rememberScrollState()
    var showAddDialog by remember { mutableStateOf(false) }

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
                    text = "Step selection & verification",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Cyan400,
                )
                Text(
                    text = "Discovered steps for: \"${workflowName ?: "Workflow"}\". Each step is tied to real component code and verified application screens.",
                    fontSize = 13.sp,
                    color = Slate300
                )
            }
        }

        // Warning Alert on Manual Edits
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Amber500.copy(alpha = 0.12f),
            border = androidx.compose.foundation.BorderStroke(1.dp, Amber500.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.WarningAmber,
                    contentDescription = null,
                    tint = Amber500,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Notice: Manually removing verified steps or editing route sequence may alter accuracy against runtime software states.",
                    fontSize = 12.sp,
                    color = Amber500
                )
            }
        }

        // Header Actions Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "STEPS IN WORKFLOW (${steps.filter { it.isChecked }.size} ACTIVE)",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Slate400,
            )

            OutlinedButton(
                onClick = { showAddDialog = true },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Indigo400),
                border = androidx.compose.foundation.BorderStroke(1.dp, Indigo500.copy(alpha = 0.4f)),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier
                    .defaultMinSize(minHeight = 32.dp)
                    .testTag("add_manual_step_btn")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Step", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        // List of Steps
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            steps.forEachIndexed { index, step ->
                var isExpanded by remember { mutableStateOf(false) }
                var editTitle by remember(step.id) { mutableStateOf(step.title) }
                var editInstruction by remember(step.id) { mutableStateOf(step.instruction) }

                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (step.isChecked) Slate900 else Slate950
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (step.isChecked) Slate800 else Slate850
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("step_card_${step.id}")
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Checkbox(
                                    checked = step.isChecked,
                                    onCheckedChange = { onToggleCheck(step.id) },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = Indigo500,
                                        uncheckedColor = Slate600
                                    ),
                                    modifier = Modifier.testTag("step_checkbox_${step.id}")
                                )

                                Text(
                                    text = "${step.stepOrder}.",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (step.isChecked) Indigo400 else Slate500
                                )

                                Column {
                                    Text(
                                        text = step.title,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (step.isChecked) Slate100 else Slate500
                                    )
                                    Row(
                                        modifier = Modifier.padding(top = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Surface(
                                            color = Slate850,
                                            shape = RoundedCornerShape(3.dp)
                                        ) {
                                            Text(
                                                text = step.screenName,
                                                fontSize = 11.sp,
                                                color = Slate300,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                        VerificationStatusBadge(status = step.verificationStatus)
                                    }
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                IconButton(
                                    onClick = { onMoveUp(step.id) },
                                    enabled = index > 0,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowUp,
                                        contentDescription = "Move Up",
                                        tint = if (index > 0) Slate300 else Slate700,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { onMoveDown(step.id) },
                                    enabled = index < steps.size - 1,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Move Down",
                                        tint = if (index < steps.size - 1) Slate300 else Slate700,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { isExpanded = !isExpanded },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.Edit,
                                        contentDescription = "Edit Step",
                                        tint = Indigo400,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { onRemoveStep(step.id) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Remove Step",
                                        tint = Slate500,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        // Expanded Edit Drawer & Evidence Button
                        AnimatedVisibility(visible = isExpanded) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp)
                                    .background(Slate950, RoundedCornerShape(6.dp))
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = editTitle,
                                    onValueChange = {
                                        editTitle = it
                                        onUpdateStep(step.id, it, editInstruction)
                                    },
                                    label = { Text("Step Title", fontSize = 12.sp) },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Indigo500,
                                        unfocusedBorderColor = Slate700
                                    )
                                )

                                OutlinedTextField(
                                    value = editInstruction,
                                    onValueChange = {
                                        editInstruction = it
                                        onUpdateStep(step.id, editTitle, it)
                                    },
                                    label = { Text("Custom Narration Instruction", fontSize = 12.sp) },
                                    placeholder = { Text("e.g. Advise user to verify password length") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Indigo500,
                                        unfocusedBorderColor = Slate700
                                    )
                                )
                            }
                        }

                        // Evidence Button: "Why is this step here?"
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = { onInspectEvidence(step) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                colors = ButtonDefaults.textButtonColors(contentColor = Cyan400),
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 28.dp)
                                    .testTag("why_is_step_here_${step.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.HelpOutline,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Why is this step here?",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
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
            enabled = steps.any { it.isChecked },
            colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp)
                .testTag("continue_to_audience_btn")
        ) {
            Text(
                text = "Continue to Audience Selection",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(imageVector = Icons.Default.ArrowForward, contentDescription = null)
        }
    }

    // Evidence Dialog
    inspectingEvidenceStep?.let { step ->
        EvidenceBottomSheet(
            step = step,
            onExploreCode = onExploreCode,
            onDismiss = { onInspectEvidence(null) }
        )
    }

    // Add Manual Step Dialog
    if (showAddDialog) {
        var newTitle by remember { mutableStateOf("") }
        var newScreen by remember { mutableStateOf("Dashboard") }
        var newAction by remember { mutableStateOf("Click") }
        var newInstruction by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = {
                Text(
                    text = "Add Step Manually",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate100
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("Step Name") },
                        placeholder = { Text("e.g. Click Export Report") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newScreen,
                        onValueChange = { newScreen = it },
                        label = { Text("Screen / Page") },
                        placeholder = { Text("e.g. Orders Table") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newInstruction,
                        onValueChange = { newInstruction = it },
                        label = { Text("Instruction (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTitle.isNotEmpty()) {
                            onAddManualStep(newScreen, newAction, newTitle, newInstruction)
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Indigo600)
                ) {
                    Text("Add Step")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            },
            containerColor = Slate900,
            shape = RoundedCornerShape(12.dp)
        )
    }
}
