package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.record.DeviceProfile
import com.example.record.UrlCandidate
import com.example.ui.theme.*

@Composable
fun RecordingTargetCard(
    recordMode: String,
    liveUrl: String,
    candidates: List<UrlCandidate>,
    urlStatus: String?,
    urlChecking: Boolean,
    deviceProfile: String,
    allowRealClicks: Boolean,
    onModeChange: (String) -> Unit,
    onUrlChange: (String) -> Unit,
    onCheckUrl: () -> Unit,
    onDeviceChange: (String) -> Unit,
    onRealClicksChange: (Boolean) -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Slate900),
        border = BorderStroke(1.dp, Slate800),
        modifier = Modifier.fillMaxWidth().testTag("recording_target_card")
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("RECORDING TARGET", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate400, letterSpacing = 0.8.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                listOf("SCREEN" to "Record live app", "SLIDES" to "Code slides only").forEach { (id, label) ->
                    val sel = recordMode == id
                    FilterChip(
                        selected = sel, onClick = { onModeChange(id) },
                        label = { Text(label, fontSize = 12.sp) },
                        modifier = Modifier.testTag("record_mode_$id")
                    )
                }
            }
            if (recordMode == "SCREEN") {
                Text(
                    "CodeCast opens your deployed app in a real browser view and records it while it visits the pages, fills the fields and points at the buttons your code describes.",
                    fontSize = 11.sp, color = Slate300
                )
                OutlinedTextField(
                    value = liveUrl, onValueChange = onUrlChange,
                    label = { Text("Live app URL", fontSize = 12.sp) },
                    placeholder = { Text("https://yourapp.com") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("live_url_field"),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Indigo500, unfocusedBorderColor = Slate700)
                )
                if (candidates.isNotEmpty()) {
                    Text("Found in your source:", fontSize = 10.sp, color = Slate400)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        candidates.take(4).forEach { c ->
                            AssistChip(onClick = { onUrlChange(c.url) }, label = { Text("${c.url}  ·  ${c.source}", fontSize = 10.sp, maxLines = 1) })
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onCheckUrl, enabled = !urlChecking, modifier = Modifier.testTag("check_url_btn")) {
                        if (urlChecking) CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                        else Text("Check URL", fontSize = 12.sp)
                    }
                    urlStatus?.let { Text(it, fontSize = 11.sp, color = if (it == "Reachable") Emerald400 else Color(0xFFF87171)) }
                }
                Text("Record for", fontSize = 11.sp, color = Slate400, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    DeviceProfile.values().forEach { d ->
                        FilterChip(
                            selected = deviceProfile == d.name, onClick = { onDeviceChange(d.name) },
                            label = { Text(d.label, fontSize = 11.sp) },
                            modifier = Modifier.testTag("device_${d.name}")
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = allowRealClicks, onCheckedChange = onRealClicksChange)
                    Column {
                        Text("Perform real clicks", fontSize = 12.sp, color = Slate200)
                        Text(
                            if (allowRealClicks) "Buttons WILL be clicked on the live site. Forms can be submitted and accounts or orders created. Use a test site."
                            else "Safe mode: fields are typed into and buttons are pointed at, but forms are not submitted. Links are followed.",
                            fontSize = 10.sp, color = if (allowRealClicks) Color(0xFFFBBF24) else Slate400
                        )
                    }
                }
            } else {
                Text("The video shows the code behind each step. Nothing is opened or recorded from your live app.", fontSize = 11.sp, color = Slate300)
            }
        }
    }
}
