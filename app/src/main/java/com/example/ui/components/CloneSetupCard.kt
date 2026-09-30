package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

/** Free, self-hosted voice clone + animated presenter (see tools/clone-server). */
@Composable
fun CloneSetupCard(
    endpoint: String,
    token: String,
    status: String?,
    checking: Boolean,
    sampleInfo: String?,
    isRecording: Boolean,
    recordingSec: Int,
    hasPhoto: Boolean,
    onEndpointChange: (String) -> Unit,
    onTokenChange: (String) -> Unit,
    onTest: () -> Unit,
    onRecord: () -> Unit,
    onStop: () -> Unit,
    onPickAudio: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Slate900),
        border = BorderStroke(1.dp, Slate800),
        modifier = Modifier.fillMaxWidth().testTag("clone_setup_card")
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("CLONE YOUR VOICE AND PRESENTER (FREE, YOUR OWN SERVER)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate400, letterSpacing = 0.6.sp)
            Text(
                "A phone can't run these models. Run the free server in tools/clone-server (Google Colab or a Hugging Face Space), paste its URL here, and CodeCast will narrate in your voice and, with a photo, animate your presenter. Without it, the phone's own voice and a still photo are used.",
                fontSize = 11.sp, color = Slate300
            )
            OutlinedTextField(
                value = endpoint, onValueChange = onEndpointChange, singleLine = true,
                label = { Text("Clone server URL", fontSize = 12.sp) }, placeholder = { Text("https://xxxx.ngrok-free.app") },
                modifier = Modifier.fillMaxWidth().testTag("clone_endpoint"),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Indigo500, unfocusedBorderColor = Slate700)
            )
            OutlinedTextField(
                value = token, onValueChange = onTokenChange, singleLine = true, visualTransformation = PasswordVisualTransformation(),
                label = { Text("Server token (optional)", fontSize = 12.sp) },
                modifier = Modifier.fillMaxWidth().testTag("clone_token"),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Indigo500, unfocusedBorderColor = Slate700)
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onTest, enabled = !checking, modifier = Modifier.testTag("clone_test_btn")) {
                    if (checking) CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp) else Text("Test connection", fontSize = 12.sp)
                }
                status?.let { Text(it, fontSize = 11.sp, color = if (it == "Connected") Emerald400 else Color(0xFFF87171)) }
            }
            Text("Voice sample (10 to 15 seconds of clear speech)", fontSize = 11.sp, color = Slate400, fontWeight = FontWeight.SemiBold)
            Text(
                text = sampleInfo?.let { "✓ Saved: $it" } ?: "No sample yet",
                fontSize = 11.sp, color = if (sampleInfo != null) Emerald400 else Slate400
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isRecording) {
                    Button(onClick = onStop, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)), modifier = Modifier.testTag("clone_stop_btn")) {
                        Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp)); Text("Stop (${recordingSec}s)", fontSize = 12.sp)
                    }
                } else {
                    Button(onClick = onRecord, colors = ButtonDefaults.buttonColors(containerColor = Indigo600), modifier = Modifier.testTag("clone_record_btn")) {
                        Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp)); Text("Record", fontSize = 12.sp)
                    }
                }
                OutlinedButton(onClick = onPickAudio, modifier = Modifier.testTag("clone_pick_audio_btn")) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp)); Text("Choose audio file", fontSize = 12.sp)
                }
            }
            if (isRecording) Text("Read something aloud, naturally. It stops at 15 seconds.", fontSize = 10.sp, color = Slate400)
            Text(
                if (hasPhoto) "Presenter photo added: it will be animated when the server supports /talking-head."
                else "Add a presenter photo above to animate it as a talking presenter.",
                fontSize = 10.sp, color = Slate400
            )
            Text("Only clone a voice or face you own or have permission to use.", fontSize = 10.sp, color = Slate500)
        }
    }
}
