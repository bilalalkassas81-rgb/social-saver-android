@file:OptIn(ExperimentalTextApi::class)

package com.socialsaver.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.socialsaver.R
import com.socialsaver.core.Platform

/** Bricolage Grotesque (variable font) for headings; body text stays in the system font. */
val Display = FontFamily(
    Font(R.font.bricolage, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    Font(R.font.bricolage, FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
    Font(R.font.bricolage, FontWeight.ExtraBold, variationSettings = FontVariation.Settings(FontVariation.weight(800))),
)

private val Light = lightColorScheme(
    primary = Color(0xFF4B3BC7), onPrimary = Color.White,
    primaryContainer = Color(0xFFE4DFFF), onPrimaryContainer = Color(0xFF221670),
    secondaryContainer = Color(0xFFE4DFFF), onSecondaryContainer = Color(0xFF221670),
    tertiary = Color(0xFFF2475F), onTertiary = Color.White,
    background = Color(0xFFF4F2FA), onBackground = Color(0xFF1E1A3A),
    surface = Color(0xFFFFFFFF), onSurface = Color(0xFF1E1A3A),
    surfaceVariant = Color(0xFFECE9F6), onSurfaceVariant = Color(0xFF6B6787),
    surfaceContainer = Color(0xFFFFFFFF),
    outline = Color(0xFFCAC5DD), outlineVariant = Color(0xFFE4E0F0),
    errorContainer = Color(0xFFFFE3E6), onErrorContainer = Color(0xFF6E0F1E),
)

private val Dark = darkColorScheme(
    primary = Color(0xFFA99BFF), onPrimary = Color(0xFF1B1446),
    primaryContainer = Color(0xFF332A80), onPrimaryContainer = Color(0xFFE4DFFF),
    secondaryContainer = Color(0xFF332A80), onSecondaryContainer = Color(0xFFE4DFFF),
    tertiary = Color(0xFFFF7686), onTertiary = Color(0xFF3E0710),
    background = Color(0xFF14112A), onBackground = Color(0xFFECE9FA),
    surface = Color(0xFF1F1B3A), onSurface = Color(0xFFECE9FA),
    surfaceVariant = Color(0xFF2A2549), onSurfaceVariant = Color(0xFFA29DC0),
    surfaceContainer = Color(0xFF1F1B3A),
    outline = Color(0xFF4A4470), outlineVariant = Color(0xFF2E2950),
    errorContainer = Color(0xFF5A1622), onErrorContainer = Color(0xFFFFD9DE),
)

private val base = Typography()
private val AppTypography = Typography(
    displaySmall = base.displaySmall.copy(fontFamily = Display, fontWeight = FontWeight.ExtraBold, fontSize = 40.sp, lineHeight = 42.sp, letterSpacing = (-1).sp),
    headlineSmall = base.headlineSmall.copy(fontFamily = Display, fontWeight = FontWeight.Bold, letterSpacing = (-0.4).sp),
    titleLarge = base.titleLarge.copy(fontFamily = Display, fontWeight = FontWeight.Bold, letterSpacing = (-0.2).sp),
    titleMedium = base.titleMedium.copy(fontFamily = Display, fontWeight = FontWeight.SemiBold),
    labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
)

@Composable
fun SocialSaverTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) Dark else Light,
        typography = AppTypography,
        content = content,
    )
}

@Composable
fun platformColor(p: Platform?): Color = when (p) {
    Platform.INSTAGRAM -> Color(0xFFD6336C)
    Platform.FACEBOOK -> Color(0xFF1877F2)
    Platform.X -> MaterialTheme.colorScheme.onSurface
    else -> MaterialTheme.colorScheme.primary
}

fun platformByLabel(label: String): Platform? = Platform.entries.firstOrNull { it.label == label }

/** Small rounded label with the site's colour dot. */
@Composable
fun PlatformTag(platform: Platform?, text: String, modifier: Modifier = Modifier) {
    Row(
        modifier
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.94f), CircleShape)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(8.dp).background(platformColor(platform), CircleShape))
        Spacer(Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}
