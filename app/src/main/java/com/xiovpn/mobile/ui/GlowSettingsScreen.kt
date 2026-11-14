package com.xiovpn.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xiovpn.mobile.settings.GlowSettingsStore
import com.xiovpn.mobile.settings.GlowShape
import com.xiovpn.mobile.ui.theme.XioColors

/** Fixed tints offered next to "follow connection colour". */
private val GlowPalette = listOf(
    "Mint" to 0xFF3CF0AF.toInt(),
    "Cyan" to 0xFF2CC7FF.toInt(),
    "Pink" to 0xFFFF5FA8.toInt(),
    "Violet" to 0xFFA98BFF.toInt(),
    "Amber" to 0xFFFFC061.toInt(),
    "Red" to 0xFFFF5C7A.toInt(),
)

/**
 * Glow ("light dance") settings: turn the rim light on/off, pick its shape and
 * colour, and tune speed / thickness / length. Nothing here touches connection
 * logic — it only drives how ConnectButton draws itself.
 */
@Composable
internal fun GlowSettingsScreen(
    onMenuClick: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val store = remember(context) { GlowSettingsStore.get(context) }
    val glow by store.settings.collectAsStateWithLifecycle()
    val accent = XioColors.ConnectedGreen
    val isPersian = LocalHomePersian.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.AutoAwesome, null, tint = accent, modifier = Modifier.size(22.dp))
            Spacer(Modifier.size(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    homeText("Light dance", "رقص نور"),
                    color = XioColors.TextPrimary,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    homeText(
                        "Shape and colour of the light around the Connect button",
                        "شکل و رنگ نوری که دور دکمه اتصال میچرخد",
                    ),
                    color = XioColors.TextSecondary,
                    fontSize = 12.sp,
                )
            }
            TextButton(onClick = { store.reset() }) {
                Text(homeText("Reset", "بازنشانی"), color = accent)
            }
        }

        // ---- master switch
        GlowCard {
            GlowToggleRow(
                title = homeText("Enabled", "فعال"),
                summary = if (glow.enabled) homeText("On", "روشن") else homeText("Off", "خاموش"),
                checked = glow.enabled,
                onCheckedChange = { store.setEnabled(it) },
            )
        }

        // ---- shapes
        GlowCard {
            SectionTitle(homeText("Shape", "شکل"))
            GlowShape.entries.forEach { shape ->
                val label = when (shape) {
                    GlowShape.COMET -> homeText("Comet", "دنبالهدار")
                    GlowShape.PULSE -> homeText("Pulse", "نبض")
                    GlowShape.TWIN -> homeText("Twin beams", "دو پرتو")
                }
                val hint = when (shape) {
                    GlowShape.COMET -> homeText("A bright head laps the rim", "یک نور درخشان دور لبه میچرخد")
                    GlowShape.PULSE -> homeText("The whole ring breathes", "کل حلقه کم و زیاد میشود")
                    GlowShape.TWIN -> homeText("Two beams sweep opposite ways", "دو پرتو در جهت مخالف میچرخند")
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { store.setShape(shape) }
                        .padding(vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(
                                if (glow.shape == shape) accent else Color.Transparent,
                                CircleShape,
                            )
                            .border(
                                if (glow.shape == shape) 2.dp else 1.dp,
                                if (glow.shape == shape) accent else Color.White.copy(alpha = 0.30f),
                                CircleShape,
                            ),
                    )
                    Spacer(Modifier.size(11.dp))
                    Column {
                        Text(label, color = XioColors.TextPrimary, fontSize = 14.sp)
                        Text(hint, color = XioColors.TextSecondary, fontSize = 11.sp)
                    }
                }
            }
        }

        // ---- colour
        GlowCard {
            SectionTitle(homeText("Colour", "رنگ"))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { store.setColor(null) }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(
                            if (glow.colorArgb == null) accent else Color.Transparent,
                            CircleShape,
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.30f), CircleShape),
                )
                Spacer(Modifier.size(11.dp))
                Text(
                    homeText("Follow connection colour", "همرنگ وضعیت اتصال"),
                    color = XioColors.TextPrimary,
                    fontSize = 14.sp,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GlowPalette.forEach { (name, argb) ->
                    val selected = glow.colorArgb == argb
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(Color(argb), CircleShape)
                            .border(
                                if (selected) 3.dp else 1.dp,
                                if (selected) Color.White else Color.White.copy(alpha = 0.25f),
                                CircleShape,
                            )
                            .clickable { store.setColor(argb) },
                    )
                }
            }
        }

        // ---- geometry
        GlowCard {
            SectionTitle(homeText("Tuning", "تنظیم دقیق"))
            GlowSlider(
                label = homeText("Speed", "سرعت"),
                value = glow.speed,
                range = 0.25f..3f,
                display = { "%.2fx".format(it) },
                onChange = { store.setSpeed(it) },
            )
            GlowSlider(
                label = homeText("Thickness", "ضخامت"),
                value = glow.thickness,
                range = 0.5f..2.5f,
                display = { "%.2fx".format(it) },
                onChange = { store.setThickness(it) },
            )
            GlowSlider(
                label = homeText("Length", "طول دنباله"),
                value = glow.length,
                range = 0.3f..1.8f,
                display = { "%.2fx".format(it) },
                onChange = { store.setLength(it) },
            )
        }

        Text(
            homeText(
                "Respects the system 'remove animations' setting.",
                "به تنظیم «حذف انیمیشن» سیستم احترام میگذارد.",
            ),
            color = XioColors.TextSecondary,
            fontSize = 11.sp,
        )
        if (isPersian) Spacer(Modifier.height(2.dp))
    }
}

@Composable
private fun GlowCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(XioColors.Surface.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
            .border(1.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) { content() }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text.uppercase(),
        color = XioColors.TextSecondary,
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(bottom = 4.dp),
    )
}

@Composable
private fun GlowToggleRow(
    title: String,
    summary: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = XioColors.TextPrimary, fontSize = 14.sp)
            Text(summary, color = XioColors.TextSecondary, fontSize = 11.sp)
        }
        androidx.compose.material3.Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun GlowSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    display: (Float) -> String,
    onChange: (Float) -> Unit,
) {
    var local by remember(value) { mutableStateOf(value) }
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = XioColors.TextPrimary, fontSize = 13.sp)
            Text(display(local), color = XioColors.TextSecondary, fontSize = 12.sp)
        }
        Slider(
            value = local,
            onValueChange = {
                local = it
                onChange(it)
            },
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = XioColors.ConnectedGreen,
                activeTrackColor = XioColors.ConnectedGreen,
                inactiveTrackColor = Color.White.copy(alpha = 0.14f),
            ),
        )
    }
}
