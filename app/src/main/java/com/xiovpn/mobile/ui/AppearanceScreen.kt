package com.xiovpn.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DashboardCustomize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiovpn.mobile.ui.theme.AccentSwatches
import com.xiovpn.mobile.ui.theme.AppearanceSettings
import com.xiovpn.mobile.ui.theme.ThemeStyle
import com.xiovpn.mobile.ui.theme.XioColors

/**
 * Appearance screen: accent, surface treatment and text size.
 *
 * Mirrors the Dark TV settings: five swatches, a liquid/OLED switch and a font
 * scale. Every control writes straight to [AppearanceSettings], whose state is
 * snapshot-backed, so the rest of the app re-themes immediately and the choice
 * survives a restart.
 */
@Composable
internal fun AppearanceScreen(onMenuClick: () -> Unit) {
    val accentIndex = AppearanceSettings.accentIndex
    val style = AppearanceSettings.style
    val fontPercent = AppearanceSettings.fontPercent
    val accent = XioColors.Accent

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        XioColors.BackgroundTop,
                        XioColors.BackgroundMiddle,
                        XioColors.BackgroundBottom,
                    ),
                ),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
                .padding(top = 14.dp, bottom = 28.dp),
        ) {
            ToolPageHeader(
                title = homeText("Appearance", "ظاهر برنامه"),
                subtitle = homeText("Colour, surface and text size", "رنگ، جنس سطح و اندازه متن"),
                icon = Icons.Outlined.DashboardCustomize,
                accent = accent,
                onMenuClick = onMenuClick,
            )

            Spacer(Modifier.height(18.dp))

            // ---- accent -------------------------------------------------
            AppearanceSection(
                title = homeText("Accent colour", "رنگ لهجه"),
                hint = homeText(
                    "Used for the connect ring, highlights and selected items",
                    "برای حلقه اتصال، برجسته‌ها و آیتم‌های انتخاب‌شده",
                ),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    AccentSwatches.forEachIndexed { index, swatch ->
                        val selected = index == accentIndex
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .background(swatch.color.copy(alpha = 0.16f), RoundedCornerShape(14.dp))
                                    .border(
                                        width = if (selected) 2.dp else 1.dp,
                                        color = if (selected) swatch.color else XioColors.CardBorder,
                                        shape = RoundedCornerShape(14.dp),
                                    )
                                    .clickable { AppearanceSettings.setAccent(index) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(if (selected) 22.dp else 18.dp)
                                        .background(swatch.color, CircleShape),
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = if (LocalHomePersian.current) swatch.persian else swatch.english,
                                color = if (selected) XioColors.TextPrimary else XioColors.TextSecondary,
                                fontSize = 10.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                fontFamily = homeLocalizedFont(),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ---- surface style ------------------------------------------
            AppearanceSection(
                title = homeText("Surface", "جنس سطح"),
                hint = homeText(
                    "Liquid keeps the layered glass look; OLED turns panels pure black",
                    "شیشه‌ای ظاهر شیشه‌ای لایه‌ای را نگه می‌دارد؛ OLED پنل‌ها را کاملاً سیاه می‌کند",
                ),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    StyleOption(
                        key = ThemeStyle.LIQUID,
                        selected = style == ThemeStyle.LIQUID,
                        title = homeText("Liquid glass", "شیشه‌ای"),
                        subtitle = homeText("Translucent layers", "لایه‌های نیمه‌شفاف"),
                        modifier = Modifier.weight(1f),
                    )
                    StyleOption(
                        key = ThemeStyle.OLED,
                        selected = style == ThemeStyle.OLED,
                        title = homeText("OLED black", "سیاه OLED"),
                        subtitle = homeText("Saves battery", "صرفه‌جویی باتری"),
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ---- text size ----------------------------------------------
            AppearanceSection(
                title = homeText("Text size", "اندازه متن"),
                hint = homeText(
                    "Overrides the phone's font setting for XioVPN only",
                    "تنظیم فونت گوشی را فقط برای XioVPN نادیده می‌گیرد",
                ),
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        listOf(85, 100, 115, 130).forEach { percent ->
                            val selected = fontPercent == percent
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .background(
                                        if (selected) accent.copy(alpha = 0.18f) else XioColors.Surface.copy(alpha = 0.7f),
                                        RoundedCornerShape(12.dp),
                                    )
                                    .border(
                                        width = if (selected) 1.5.dp else 1.dp,
                                        color = if (selected) accent else XioColors.CardBorder,
                                        shape = RoundedCornerShape(12.dp),
                                    )
                                    .clickable { AppearanceSettings.applyFontPercent(percent) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "$percent%",
                                    color = if (selected) XioColors.TextPrimary else XioColors.TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = homeText(
                            "Sample text at the selected size",
                            "متن نمونه با اندازه انتخاب‌شده",
                        ),
                        color = XioColors.TextPrimary,
                        fontSize = (13 * fontPercent / 100f).sp,
                        fontFamily = homeLocalizedFont(),
                    )
                }
            }

            Spacer(Modifier.height(22.dp))
            Text(
                text = homeText(
                    "Appearance is saved on this device and applied immediately.",
                    "ظاهر روی همین دستگاه ذخیره می‌شود و بی‌درنگ اعمال می‌شود.",
                ),
                color = XioColors.TextSecondary.copy(alpha = 0.7f),
                fontSize = 10.sp,
                fontFamily = homeLocalizedFont(),
            )
        }
    }
}

@Composable
private fun AppearanceSection(
    title: String,
    hint: String,
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            color = XioColors.TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = homeLocalizedFont(),
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = hint,
            color = XioColors.TextSecondary.copy(alpha = 0.78f),
            fontSize = 10.5.sp,
            fontFamily = homeLocalizedFont(),
        )
        Spacer(Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(XioColors.Surface.copy(alpha = 0.55f), RoundedCornerShape(18.dp))
                .border(0.8.dp, XioColors.GlassBorder, RoundedCornerShape(18.dp))
                .padding(12.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun StyleOption(
    key: String,
    selected: Boolean,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    val accent = XioColors.Accent
    Column(
        modifier = modifier
            .height(66.dp)
            .background(
                if (selected) accent.copy(alpha = 0.16f) else XioColors.Surface.copy(alpha = 0.7f),
                RoundedCornerShape(14.dp),
            )
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) accent else XioColors.CardBorder,
                shape = RoundedCornerShape(14.dp),
            )
            .clickable { AppearanceSettings.applyStyle(key) }
            .padding(horizontal = 10.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = title,
            color = if (selected) XioColors.TextPrimary else XioColors.TextSecondary,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            fontFamily = homeLocalizedFont(),
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = subtitle,
            color = XioColors.TextSecondary.copy(alpha = 0.72f),
            fontSize = 9.5.sp,
            fontFamily = homeLocalizedFont(),
        )
    }
}
