package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.CodebaseFile
import com.example.ui.theme.*

@Composable
fun CodebaseExplorerDialog(
    files: List<CodebaseFile>,
    initialSelectedFile: CodebaseFile? = null,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
                .clip(RoundedCornerShape(16.dp)),
            color = Slate950,
            border = androidx.compose.foundation.BorderStroke(1.dp, Slate800)
        ) {
            CodebaseExplorerContent(
                files = files,
                initialSelectedFile = initialSelectedFile,
                onClose = onDismiss
            )
        }
    }
}

@Composable
fun CodebaseExplorerContent(
    files: List<CodebaseFile>,
    initialSelectedFile: CodebaseFile? = null,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var selectedCategory by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFile by remember(initialSelectedFile, files) {
        mutableStateOf(initialSelectedFile ?: files.firstOrNull())
    }

    val filteredFiles = remember(files, selectedCategory, searchQuery) {
        files.filter { file ->
            val matchesCategory = if (selectedCategory == "All") true else file.category == selectedCategory
            val matchesSearch = if (searchQuery.isBlank()) true else {
                file.path.contains(searchQuery, ignoreCase = true) ||
                        file.content.contains(searchQuery, ignoreCase = true)
            }
            matchesCategory && matchesSearch
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // TOP IDE BAR
        Surface(
            color = Slate900,
            border = androidx.compose.foundation.BorderStroke(1.dp, Slate800)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Indigo600),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Code, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                    Column {
                        Text("Codebase Source Explorer", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Slate50)
                        Text(
                            text = "${files.size} indexed files • Real project source tree",
                            fontSize = 11.sp,
                            color = Slate400
                        )
                    }
                }

                IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Slate300)
                }
            }
        }

        // FILTER CHIPS & SEARCH
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Slate900.copy(alpha = 0.5f))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search files, symbols, routes...", fontSize = 12.sp, color = Slate500) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Slate400, modifier = Modifier.size(16.dp)) },
                trailingIcon = if (searchQuery.isNotEmpty()) {
                    {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Slate400, modifier = Modifier.size(16.dp))
                        }
                    }
                } else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Slate950,
                    unfocusedContainerColor = Slate950,
                    focusedBorderColor = Indigo500,
                    unfocusedBorderColor = Slate800,
                    focusedTextColor = Slate100,
                    unfocusedTextColor = Slate200
                ),
                singleLine = true
            )

            // Category Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("All", "Frontend", "Backend", "Database", "Config").forEach { cat ->
                    val isSel = selectedCategory == cat
                    Surface(
                        onClick = { selectedCategory = cat },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSel) Indigo600 else Slate850,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) Indigo400 else Slate700)
                    ) {
                        Text(
                            text = cat,
                            fontSize = 11.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSel) Color.White else Slate300,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // MAIN WORKSPACE (Split File List & Editor)
        Row(modifier = Modifier.fillMaxSize()) {
            // LEFT: FILE TREE LIST (Width 160.dp on mobile / 200.dp)
            Surface(
                modifier = Modifier
                    .width(150.dp)
                    .fillMaxHeight(),
                color = Slate900.copy(alpha = 0.8f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate800)
            ) {
                val listScroll = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(listScroll)
                        .padding(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "FILES (${filteredFiles.size})",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate400,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                    )

                    filteredFiles.forEach { file ->
                        val isSelected = selectedFile?.path == file.path
                        val fileIcon = when (file.language) {
                            "typescript" -> Icons.Default.IntegrationInstructions
                            "javascript" -> Icons.Default.Javascript
                            "python" -> Icons.Default.Terminal
                            "prisma", "sql" -> Icons.Default.Storage
                            "json", "config" -> Icons.Default.Settings
                            else -> Icons.Default.InsertDriveFile
                        }
                        val iconColor = when (file.language) {
                            "typescript" -> Cyan400
                            "javascript" -> Amber500
                            "python" -> Emerald400
                            "prisma", "sql" -> Indigo400
                            else -> Slate400
                        }

                        Surface(
                            onClick = { selectedFile = file },
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSelected) Slate800 else Color.Transparent,
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, Indigo500) else null,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(fileIcon, contentDescription = null, tint = iconColor, modifier = Modifier.size(14.dp))
                                Column {
                                    Text(
                                        text = file.path.substringAfterLast("/"),
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Slate50 else Slate300,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = file.category,
                                        fontSize = 8.sp,
                                        color = Slate500
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // RIGHT: SYNTAX-HIGHLIGHTED CODE VIEWER
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(Slate950)
            ) {
                if (selectedFile != null) {
                    val curr = selectedFile!!
                    Column(modifier = Modifier.fillMaxSize()) {
                        // File Breadcrumb & Action Header
                        Surface(
                            color = Slate900,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Slate800)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.FolderOpen, contentDescription = null, tint = Cyan400, modifier = Modifier.size(13.dp))
                                    Text(
                                        text = curr.path,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = Slate200,
                                        maxLines = 1
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(color = Slate800, shape = RoundedCornerShape(4.dp)) {
                                        Text(
                                            text = "${curr.lineCount} lines",
                                            fontSize = 9.sp,
                                            color = Slate400,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val clip = ClipData.newPlainText("Code", curr.content)
                                            clipboard.setPrimaryClip(clip)
                                            Toast.makeText(context, "Code copied to clipboard!", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy Code", tint = Cyan400, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }

                        // Exported symbols chips
                        if (curr.exportedSymbols.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Slate900.copy(alpha = 0.4f))
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("AST Symbols:", fontSize = 9.sp, color = Slate500, fontWeight = FontWeight.Bold)
                                curr.exportedSymbols.forEach { sym ->
                                    Surface(color = Indigo500.copy(alpha = 0.25f), shape = RoundedCornerShape(3.dp)) {
                                        Text(
                                            text = sym,
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = Cyan300,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Code Editor Lines
                        val codeScrollVertical = rememberScrollState()
                        val codeScrollHorizontal = rememberScrollState()
                        val lines = remember(curr.content) { curr.content.lines() }

                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(codeScrollVertical)
                        ) {
                            // Line Number Gutter
                            Column(
                                modifier = Modifier
                                    .background(Slate900.copy(alpha = 0.6f))
                                    .padding(horizontal = 8.dp, vertical = 8.dp)
                            ) {
                                lines.indices.forEach { idx ->
                                    Text(
                                        text = "${idx + 1}",
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = Slate600
                                    )
                                }
                            }

                            // Code Text with Syntax Coloring
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(codeScrollHorizontal)
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                lines.forEach { line ->
                                    Text(
                                        text = highlightSyntax(line),
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No file selected", color = Slate500, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

/**
 * Lightweight syntax highlighter for TypeScript, JavaScript, Python, Prisma, and JSON
 */
fun highlightSyntax(line: String): AnnotatedString {
    val trimmed = line.trim()
    if (trimmed.startsWith("//") || trimmed.startsWith("#")) {
        return AnnotatedString(line, SpanStyle(color = Color(0xFF64748B))) // Slate500
    }

    val keywords = setOf(
        "import", "from", "export", "async", "await", "function", "const", "let", "var", "return",
        "if", "else", "try", "catch", "throw", "new", "type", "interface", "class", "default",
        "enum", "model", "datasource", "generator", "def", "while", "for"
    )

    val types = setOf(
        "string", "number", "boolean", "any", "void", "Promise", "Request", "NextResponse",
        "String", "Int", "DateTime", "Decimal", "Role", "OrderStatus"
    )

    return buildAnnotatedString {
        val tokens = line.split(Regex("(?<=[^a-zA-Z0-9_])|(?=[^a-zA-Z0-9_])"))
        var inQuotes = false
        var quoteChar = ' '

        tokens.forEach { token ->
            when {
                token.startsWith("\"") || token.startsWith("'") || token.startsWith("`") -> {
                    inQuotes = true
                    quoteChar = token[0]
                    pushStyle(SpanStyle(color = Color(0xFF34D399))) // Emerald400
                    append(token)
                    if (token.length > 1 && token.endsWith(quoteChar)) {
                        pop()
                        inQuotes = false
                    }
                }
                inQuotes -> {
                    append(token)
                    if (token.contains(quoteChar)) {
                        pop()
                        inQuotes = false
                    }
                }
                keywords.contains(token) -> {
                    pushStyle(SpanStyle(color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)) // Cyan400
                    append(token)
                    pop()
                }
                types.contains(token) -> {
                    pushStyle(SpanStyle(color = Color(0xFFFBBF24))) // Amber400
                    append(token)
                    pop()
                }
                token.toIntOrNull() != null -> {
                    pushStyle(SpanStyle(color = Color(0xFFA78BFA))) // Violet400
                    append(token)
                    pop()
                }
                token in listOf("{", "}", "(", ")", "[", "]", "=>", "==", "===", "!=", ":", ";", "=", "+", "-") -> {
                    pushStyle(SpanStyle(color = Color(0xFF94A3B8))) // Slate400
                    append(token)
                    pop()
                }
                else -> {
                    pushStyle(SpanStyle(color = Color(0xFFF1F5F9))) // Slate100
                    append(token)
                    pop()
                }
            }
        }
    }
}
