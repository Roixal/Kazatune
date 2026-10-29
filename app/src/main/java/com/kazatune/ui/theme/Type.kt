package com.kazatune.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val DinProFamily = FontFamily.Default

val RessoTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = DinProFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        color = RessoWhite
    ),
    headlineMedium = TextStyle(
        fontFamily = DinProFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        color = RessoWhite
    ),
    titleLarge = TextStyle(
        fontFamily = DinProFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        color = RessoWhite
    ),
    titleMedium = TextStyle(
        fontFamily = DinProFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        color = RessoWhite80
    ),
    bodyLarge = TextStyle(
        fontFamily = DinProFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        color = RessoWhite80
    ),
    bodyMedium = TextStyle(
        fontFamily = DinProFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        color = RessoWhite60
    ),
    labelSmall = TextStyle(
        fontFamily = DinProFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        color = RessoWhite60
    )
)
