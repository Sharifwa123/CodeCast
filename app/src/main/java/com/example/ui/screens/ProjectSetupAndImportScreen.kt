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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun ProjectSetupAndImportScreen(
    repoSourceOption: String,
    projectNameInput: String,
    repoUrlInput: String,
    onRepoSourceChange: (String) -> Unit,
    onProjectNameChange: (String) -> Unit,
    onRepoUrlChange: (String) -> Unit,
    onContinueClick: () -> Unit,
    onQuickDemoClick: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Card
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Indigo500.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = null,
                            tint = Indigo400,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = "Automated Codebase-to-Video Engine",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate100
                    )
                }

                Text(
                    text = "Upload or connect your application codebase. CodeCast scans pages, AST routes, forms, and verified UI workflows to synthesize accurate video tutorials without manual prompt engineering.",
                    fontSize = 12.sp,
                    color = Slate300,
                    lineHeight = 18.sp
                )

                // Quick Demo Callout
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Slate850,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Indigo500.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onQuickDemoClick() }
                        .padding(top = 4.dp)
                        .testTag("demo_banner_card")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Ready to test right now?",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Slate100
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(Cyan500.copy(alpha = 0.2f))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text("INSTANT DEMO", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Cyan400)
                                }
                            }
                            Text(
                                text = "Load 'PayFlex SaaS' (Next.js 14 + Stripe + Paystack) pre-analyzed with 14 screens & verified workflows.",
                                fontSize = 11.sp,
                                color = Slate400,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        Button(
                            onClick = onQuickDemoClick,
                            colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier
                                .defaultMinSize(minHeight = 36.dp)
                                .padding(start = 8.dp)
                                .testTag("load_demo_project_btn")
                        ) {
                            Text("Launch Demo", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // Project Name
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
                    text = "PROJECT DETAILS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate400,
                    letterSpacing = 0.8.sp
                )

                OutlinedTextField(
                    value = projectNameInput,
                    onValueChange = onProjectNameChange,
                    label = { Text("Project Name", fontSize = 12.sp) },
                    placeholder = { Text("e.g. My Next.js SaaS App") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("project_name_field"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Indigo500,
                        unfocusedBorderColor = Slate700
                    )
                )
            }
        }

        // Connection Source
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
                    text = "CONNECT APPLICATION SOURCE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate400,
                    letterSpacing = 0.8.sp
                )

                val sources = listOf(
                    Triple("ZIP", "Upload ZIP Archive", Icons.Default.FolderZip),
                    Triple("GITHUB", "Connect GitHub Repo", Icons.Default.Source),
                    Triple("GITLAB", "Connect GitLab Repo", Icons.Default.DeviceHub),
                    Triple("BITBUCKET", "Connect Bitbucket", Icons.Default.CloudQueue)
                )

                sources.forEach { (id, title, icon) ->
                    val isSelected = repoSourceOption == id
                    Surface(
                        onClick = { onRepoSourceChange(id) },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) Slate800 else Slate950,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) Indigo500 else Slate800
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("source_option_$id")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = if (isSelected) Indigo400 else Slate400,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = title,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Slate50 else Slate300
                                )
                            }
                            RadioButton(
                                selected = isSelected,
                                onClick = { onRepoSourceChange(id) },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = Indigo500,
                                    unselectedColor = Slate600
                                )
                            )
                        }
                    }
                }

                if (repoSourceOption != "ZIP") {
                    OutlinedTextField(
                        value = repoUrlInput,
                        onValueChange = onRepoUrlChange,
                        label = { Text("Repository URL", fontSize = 12.sp) },
                        placeholder = { Text("https://github.com/org/repo") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("repo_url_field"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Indigo500,
                            unfocusedBorderColor = Slate700
                        )
                    )
                }
            }
        }

        // Supported Frameworks Detection Card
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Slate900),
            border = androidx.compose.foundation.BorderStroke(1.dp, Slate800),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "AUTOMATIC FRAMEWORK & AST SUPPORT",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate400,
                    letterSpacing = 0.6.sp
                )
                Text(
                    text = "CodeCast auto-indexes routes, components, and controllers across modern software stacks:",
                    fontSize = 11.sp,
                    color = Slate300
                )

                val frameworks = listOf(
                    "Next.js", "React", "Vue", "Angular", "Svelte", "Node.js", "Express",
                    "FastAPI", "Django", "Laravel", "Flutter", "React Native", "Android", "iOS"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    frameworks.take(5).forEach { fw ->
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Slate800
                        ) {
                            Text(
                                text = fw,
                                fontSize = 10.sp,
                                color = Slate200,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Slate800
                    ) {
                        Text(
                            text = "+9 more",
                            fontSize = 10.sp,
                            color = Indigo400,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }

        // Security Assurance
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Slate950,
            border = androidx.compose.foundation.BorderStroke(1.dp, Slate800),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Security",
                    tint = Emerald400,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Enterprise Privacy: Encrypted storage, credential redaction, private AST indexing. Code is never exposed publicly.",
                    fontSize = 10.sp,
                    color = Slate400
                )
            }
        }

        // Primary Action
        Button(
            onClick = onContinueClick,
            colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp)
                .testTag("analyze_codebase_btn")
        ) {
            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Analyze Codebase & Discover Workflows",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}
