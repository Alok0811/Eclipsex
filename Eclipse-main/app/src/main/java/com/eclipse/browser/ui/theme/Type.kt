package com.eclipse.browser.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp

val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = com.eclipse.browser.R.array.com_google_android_gms_fonts_certs
)

val Outfit = FontFamily(
    Font(googleFont = GoogleFont("Outfit"), fontProvider = provider, weight = FontWeight.ExtraLight),
    Font(googleFont = GoogleFont("Outfit"), fontProvider = provider, weight = FontWeight.Light),
    Font(googleFont = GoogleFont("Outfit"), fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = GoogleFont("Outfit"), fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = GoogleFont("Outfit"), fontProvider = provider, weight = FontWeight.SemiBold),
)

val SpaceMono = FontFamily(
    Font(googleFont = GoogleFont("Space Mono"), fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = GoogleFont("Space Mono"), fontProvider = provider, weight = FontWeight.Bold),
)

// Typography hierarchy
val GreetingMain = TextStyle(fontFamily = Outfit, fontWeight = FontWeight.Light, fontSize = 20.sp)
val SectionTitle = TextStyle(fontFamily = Outfit, fontWeight = FontWeight.Medium, fontSize = 18.sp)
val BodyText = TextStyle(fontFamily = Outfit, fontWeight = FontWeight.Normal, fontSize = 15.sp)
val BodySecondary = TextStyle(fontFamily = Outfit, fontWeight = FontWeight.Normal, fontSize = 14.sp)
val Caption = TextStyle(fontFamily = Outfit, fontWeight = FontWeight.Normal, fontSize = 13.sp)
val ClockText = TextStyle(fontFamily = SpaceMono, fontWeight = FontWeight.Normal, fontSize = 11.sp, letterSpacing = 2.sp)
val LabelText = TextStyle(fontFamily = SpaceMono, fontWeight = FontWeight.Normal, fontSize = 11.sp, letterSpacing = 2.sp)
val TinyMeta = TextStyle(fontFamily = SpaceMono, fontWeight = FontWeight.Normal, fontSize = 10.sp)
val DateLabel = TextStyle(fontFamily = SpaceMono, fontWeight = FontWeight.Normal, fontSize = 10.sp, letterSpacing = 2.5.sp)
val BrandText = TextStyle(fontFamily = SpaceMono, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 3.sp)
