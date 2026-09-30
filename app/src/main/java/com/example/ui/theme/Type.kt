package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private fun s(size: Int, line: Int, w: FontWeight = FontWeight.Normal) =
    TextStyle(fontFamily = FontFamily.Default, fontWeight = w, fontSize = size.sp, lineHeight = line.sp, letterSpacing = 0.sp)

val Typography = Typography(
    headlineMedium = s(26, 32, FontWeight.SemiBold),
    headlineSmall = s(22, 28, FontWeight.SemiBold),
    titleLarge = s(20, 26, FontWeight.SemiBold),
    titleMedium = s(16, 22, FontWeight.Medium),
    titleSmall = s(14, 20, FontWeight.Medium),
    bodyLarge = s(16, 24),
    bodyMedium = s(14, 21),
    bodySmall = s(12, 17),
    labelLarge = s(14, 20, FontWeight.Medium),
    labelMedium = s(12, 16, FontWeight.Medium),
    labelSmall = s(11, 15, FontWeight.Medium)
)
