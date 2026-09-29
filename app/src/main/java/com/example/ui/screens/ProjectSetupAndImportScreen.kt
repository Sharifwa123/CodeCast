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

data class SampleCodebase(
    val fileName: String,
    val framework: String,
    val fileCount: Int,
    val sizeStr: String
)

@Composable
fun ProjectSetupAndImportScreen(
    repoSourceOption: String,
    projectNameInput: String,
    repoUrlInput: String,
    uploadedZipFileName: String? = null,
    uploadedZipFileSize: String? = null,
    uploadedZipEntryCount: Int = 0,
    uploadedZipFramework: String? = null,
    onRepoSourceChange: (String) -> Unit,
    onProjectNameChange: (String) -> Unit,
    onRepoUrlChange: (String) -> Unit,
    onPickZipFile: () -> Unit = {},
    onSelectSampleZip: (fileName: String, framework: String, filesCount: Int, sizeStr: String) -> Unit = { _, _, _, _ -> },
    onClearZip: () -> Unit = {},
    onExploreCode: (() -> Unit)? = null,
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

                if (repoSourceOption == "ZIP") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (uploadedZipFileName != null) {
                            // Uploaded ZIP Card
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Slate800,
                                border = androidx.compose.foundation.BorderStroke(1.dp, Emerald400),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("verified_zip_card")
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = Emerald400,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Column {
                                                Text(
                                                    text = uploadedZipFileName,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = Slate100
                                                )
                                                Text(
                                                    text = "Archive size: $uploadedZipFileSize • $uploadedZipEntryCount files indexed",
                                                    fontSize = 11.sp,
                                                    color = Slate300
                                                )
                                            }
                                        }
                                        IconButton(onClick = onClearZip, modifier = Modifier.size(32.dp)) {
                                            Icon(Icons.Default.Close, contentDescription = "Clear ZIP", tint = Slate400)
                                        }
                                    }

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(color = Slate900, shape = RoundedCornerShape(4.dp)) {
                                            Text(
                                                text = "Framework: ${uploadedZipFramework ?: "Detected"}",
                                                fontSize = 10.sp,
                                                color = Cyan400,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                            )
                                        }
                                        Surface(color = Slate900, shape = RoundedCornerShape(4.dp)) {
                                            Text(
                                                text = "AST Indexed ✓",
                                                fontSize = 10.sp,
                                                color = Emerald400,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        if (onExploreCode != null) {
                                            Button(
                                                onClick = onExploreCode,
                                                colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                                modifier = Modifier.weight(1.2f).defaultMinSize(minHeight = 36.dp)
                                            ) {
                                                Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(15.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Inspect Code", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        OutlinedButton(
                                            onClick = onPickZipFile,
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Indigo400),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, Indigo500.copy(alpha = 0.5f)),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                            modifier = Modifier.weight(1f).defaultMinSize(minHeight = 36.dp)
                                        ) {
                                            Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(15.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Replace ZIP", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        } else {
                            // Empty Drop / Browse Box
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Slate950,
                                border = androidx.compose.foundation.BorderStroke(1.dp, Slate700),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onPickZipFile() }
                                    .testTag("zip_drop_zone")
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(Cyan500.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CloudUpload,
                                            contentDescription = "Upload ZIP",
                                            tint = Cyan400,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                    Text(
                                        text = "Drop or Choose Codebase ZIP Archive",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Slate100
                                    )
                                    Text(
                                        text = "Supports React, Next.js, Vue, Angular, Svelte, Flutter, FastAPI, Laravel & Node.js codebases.",
                                        fontSize = 11.sp,
                                        color = Slate400,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                    Button(
                                        onClick = onPickZipFile,
                                        colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                        modifier = Modifier
                                            .padding(top = 4.dp)
                                            .testTag("browse_zip_btn")
                                    ) {
                                        Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Browse Device Files (.zip)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }

                            // Quick Sample Codebases
                            Text(
                                text = "OR SELECT A PRE-VERIFIED CODEBASE SAMPLE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate400,
                                letterSpacing = 0.5.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )

                            val samples = listOf(
                                SampleCodebase("ecommerce-nextjs-app.zip", "Next.js 14 + Stripe + Prisma", 148, "14.2 MB"),
                                SampleCodebase("banking-dashboard-react.zip", "React 18 + Vite + Redux", 96, "11.5 MB"),
                                SampleCodebase("mobile-fitness-flutter.zip", "Flutter 3.22 + Riverpod", 184, "22.8 MB"),
                                SampleCodebase("fastapi-celery-service.zip", "FastAPI + PostgreSQL + Celery", 64, "8.4 MB")
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                samples.forEach { (fileName, framework, fileCount, sizeStr) ->
                                    Surface(
                                        onClick = { onSelectSampleZip(fileName, framework, fileCount, sizeStr) },
                                        shape = RoundedCornerShape(8.dp),
                                        color = Slate900,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Slate800),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.FolderZip,
                                                    contentDescription = null,
                                                    tint = Indigo400,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Column {
                                                    Text(text = fileName, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Slate200)
                                                    Text(text = "$framework • $fileCount files ($sizeStr)", fontSize = 10.sp, color = Slate400)
                                                }
                                            }
                                            Text(text = "Select", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Indigo400)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
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
