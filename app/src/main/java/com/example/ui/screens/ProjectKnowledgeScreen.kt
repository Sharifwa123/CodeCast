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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ProjectEntity
import com.example.ui.theme.*

@Composable
fun ProjectKnowledgeScreen(
    project: ProjectEntity?,
    onExploreCodeClick: (() -> Unit)? = null,
    onBackClick: () -> Unit
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
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "PERSISTENT PROJECT KNOWLEDGE LAYER",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Cyan400,
                    letterSpacing = 0.8.sp
                )
                Text(
                    text = "Cached understanding of ${project?.name ?: "Application"} is stored locally. Generate videos, user manuals, onboarding materials, or API guides instantly without re-uploading or re-parsing.",
                    fontSize = 12.sp,
                    color = Slate300
                )

                if (onExploreCodeClick != null) {
                    Button(
                        onClick = onExploreCodeClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Browse Verified Source Code Files", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Indexed Knowledge Entities
        val knowledgeSections = listOf(
            Triple("Indexed Screens & Pages", "14 Pages with DOM bindings", listOf("/, /auth/signup, /auth/login, /dashboard, /products, /orders, /customers, /payments, /settings")),
            Triple("Detected API Interactors", "5 REST & Webhook endpoints", listOf("POST /api/v1/auth/register", "POST /api/v1/checkout/session", "POST /api/v1/paystack/webhook", "GET /api/v1/orders/[id]/receipt", "POST /api/v1/whatsapp/notify")),
            Triple("Form Schema & Validation", "Strict client + server schema", listOf("RegistrationForm: email(rfc5322), password(min:12), companyName(required)", "ProductForm: sku(unique), price(currency, decimal), inventory(int)")),
            Triple("Authentication & Permissions", "Multi-tenant RBAC", listOf("NextAuth JWT session tokens", "Protected routes: /dashboard/*, /settings/*", "Public routes: /, /auth/*"))
        )

        knowledgeSections.forEach { (title, subtitle, items) ->
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900),
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate800),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = title,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate100
                        )
                        Surface(
                            color = Slate800,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = subtitle,
                                fontSize = 9.sp,
                                color = Cyan400,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Slate950, RoundedCornerShape(6.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items.forEach { itm ->
                            Text(
                                text = "• $itm",
                                fontSize = 11.sp,
                                color = Slate300,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        OutlinedButton(
            onClick = onBackClick,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate200),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back to Studio")
        }
    }
}
