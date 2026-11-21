package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val AlaapDisplayFont = FontFamily(
  Font(R.font.plus_jakarta_sans, FontWeight.Normal),
  Font(R.font.plus_jakarta_sans, FontWeight.Medium),
  Font(R.font.plus_jakarta_sans, FontWeight.SemiBold),
  Font(R.font.plus_jakarta_sans, FontWeight.Bold),
)

val AlaapBodyFont = FontFamily(
  Font(R.font.noto_sans_bengali, FontWeight.Normal),
  Font(R.font.noto_sans_bengali, FontWeight.Medium),
  Font(R.font.noto_sans_bengali, FontWeight.SemiBold),
  Font(R.font.noto_sans_bengali, FontWeight.Bold),
)

val Typography = Typography(
  headlineLarge = TextStyle(
    fontFamily = AlaapBodyFont,
    fontWeight = FontWeight.Bold,
    fontSize = 30.sp,
    lineHeight = 36.sp,
  ),
  headlineMedium = TextStyle(
    fontFamily = AlaapBodyFont,
    fontWeight = FontWeight.Bold,
    fontSize = 24.sp,
    lineHeight = 30.sp,
  ),
  titleLarge = TextStyle(
    fontFamily = AlaapBodyFont,
    fontWeight = FontWeight.Bold,
    fontSize = 20.sp,
    lineHeight = 26.sp,
  ),
  titleMedium = TextStyle(
    fontFamily = AlaapBodyFont,
    fontWeight = FontWeight.SemiBold,
    fontSize = 16.sp,
    lineHeight = 22.sp,
  ),
  bodyLarge = TextStyle(
    fontFamily = AlaapBodyFont,
    fontWeight = FontWeight.Normal,
    fontSize = 16.sp,
    lineHeight = 23.sp,
  ),
  bodyMedium = TextStyle(
    fontFamily = AlaapBodyFont,
    fontWeight = FontWeight.Normal,
    fontSize = 14.sp,
    lineHeight = 20.sp,
  ),
  labelLarge = TextStyle(
    fontFamily = AlaapBodyFont,
    fontWeight = FontWeight.SemiBold,
    fontSize = 14.sp,
    lineHeight = 18.sp,
  ),
  labelMedium = TextStyle(
    fontFamily = AlaapBodyFont,
    fontWeight = FontWeight.Medium,
    fontSize = 12.sp,
    lineHeight = 16.sp,
  ),
  labelSmall = TextStyle(
    fontFamily = AlaapBodyFont,
    fontWeight = FontWeight.Medium,
    fontSize = 11.sp,
    lineHeight = 14.sp,
  ),
)
