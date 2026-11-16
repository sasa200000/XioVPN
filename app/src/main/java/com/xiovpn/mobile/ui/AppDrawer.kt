package com.xiovpn.mobile.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Speed
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xiovpn.mobile.engine.EngineMode
import com.xiovpn.mobile.engine.EngineModeStore
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiovpn.mobile.ui.theme.XioColors
import com.xiovpn.mobile.update.AppUpdateManager
import kotlinx.coroutines.delay
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue

internal enum class DrawerDestination {
    HOME,
    CONFIGS,
    SNI_MAKER,
    ROUTE_SPEED_TEST,
    LIVE_LOGS,
    APP_BYPASS,
    SETTINGS,
    ADVANCED_SETTINGS,
    POW_SETTINGS,
    TOR_SETTINGS,
    DIRECT_SETTINGS,
    GLOW_SETTINGS,
    APPEARANCE,
    SUPPORT,
}

internal enum class DrawerLanguage {
    PERSIAN,
    ENGLISH,
}

private data class DrawerItem(
    val destination: DrawerDestination,
    val label: String,
    val icon: ImageVector,
)

private val DrawerBlue = Color(0xFF299EFF)
private val DrawerText = Color(0xFFE7EEF8)
private val DrawerMuted = Color(0xFF98A9BF)
private val DrawerDivider = Color(0x263E5874)

/**
 * Drawer row with an optional live rim light. Every second the colour steps and
 * a light segment laps the row outline — used for the Support entry so it is
 * always visible on every engine.
 */
@Composable
private fun AnimatedDrawerRow(
    selected: Boolean,
    animate: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(animate) {
        if (animate) {
            while (true) {
                delay(1_000)
                tick++
            }
        }
    }
    Box(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.clickable(onClick = onClick)) { content() }
        if (animate) {
            BorderLightDance(
                tick = tick,
                accent = DrawerBlue,
                connected = true,
                motionEnabled = true,
                cornerRadius = 14.dp,
                thickness = 1.8.dp,
                modifier = Modifier.matchParentSize(),
            )
        }
    }
}

internal fun DrawerDestination.visibleFor(mode: EngineMode): Boolean = when (this) {
    DrawerDestination.SNI_MAKER,
    DrawerDestination.ROUTE_SPEED_TEST,
    DrawerDestination.ADVANCED_SETTINGS -> mode.isXray
    DrawerDestination.POW_SETTINGS -> mode.isPow
    DrawerDestination.TOR_SETTINGS -> mode.isTor
    DrawerDestination.DIRECT_SETTINGS -> mode.isDirect
    else -> true
}

private fun drawerItemsFor(mode: EngineMode): List<DrawerItem> = buildList {
    add(DrawerItem(DrawerDestination.HOME, "Home", Icons.Outlined.Home))
    // Support sits directly under Home so it is the first thing a user sees,
    // on every engine.
    add(DrawerItem(DrawerDestination.SUPPORT, "Support", Icons.Outlined.SupportAgent))
    if (mode.isTor || mode.isPow) {
        add(DrawerItem(DrawerDestination.CONFIGS, "Select country", Icons.Outlined.Public))
    } else if (mode.isDirect) {
        // Engine 4 has no configs or countries: it dials sites directly.
    } else {
        add(DrawerItem(DrawerDestination.CONFIGS, "Configs", Icons.Outlined.Description))
        add(DrawerItem(DrawerDestination.SNI_MAKER, "Config Maker", Icons.Outlined.Code))
        add(DrawerItem(DrawerDestination.ROUTE_SPEED_TEST, "Route Speed Test", Icons.Outlined.Speed))
    }
    add(DrawerItem(DrawerDestination.LIVE_LOGS, "Logs", Icons.AutoMirrored.Outlined.ListAlt))
    add(DrawerItem(DrawerDestination.APP_BYPASS, "App Bypass", Icons.Outlined.Block))
    add(DrawerItem(DrawerDestination.SETTINGS, "General settings", Icons.Outlined.Settings))
    when {
        mode.isPow -> add(DrawerItem(DrawerDestination.POW_SETTINGS, "PoW settings", Icons.Outlined.Tune))
        mode.isTor -> add(DrawerItem(DrawerDestination.TOR_SETTINGS, "Tor settings", Icons.Outlined.Tune))
        mode.isDirect -> add(DrawerItem(DrawerDestination.DIRECT_SETTINGS, "Direct settings", Icons.Outlined.Tune))
        else -> add(DrawerItem(DrawerDestination.ADVANCED_SETTINGS, "Xray settings", Icons.Outlined.Tune))
    }
}

@Composable
internal fun DrawerOverlay(
    visible: Boolean,
    selectedDestination: DrawerDestination,
    selectedLanguage: DrawerLanguage,
    onDestinationSelected: (DrawerDestination) -> Unit,
    onLanguageSelected: (DrawerLanguage) -> Unit,
    onDismiss: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(220)),
            exit = fadeOut(tween(180)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.67f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Button,
                        onClick = onDismiss,
                    ),
            )
        }

        AnimatedVisibility(
            visible = visible,
            modifier = Modifier.align(Alignment.TopStart),
            enter = slideInHorizontally(
                initialOffsetX = { -it },
                animationSpec = tween(310, easing = FastOutSlowInEasing),
            ) + fadeIn(tween(180)),
            exit = slideOutHorizontally(
                targetOffsetX = { -it },
                animationSpec = tween(250, easing = FastOutSlowInEasing),
            ) + fadeOut(tween(150)),
        ) {
            AppDrawer(
                selectedDestination = selectedDestination,
                selectedLanguage = selectedLanguage,
                onDestinationSelected = onDestinationSelected,
                onLanguageSelected = onLanguageSelected,
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.80f)
                    .widthIn(max = 340.dp)
                    .padding(start = 6.dp, top = 7.dp, bottom = 7.dp),
            )
        }
    }
}

@Composable
internal fun AppDrawer(
    selectedDestination: DrawerDestination,
    selectedLanguage: DrawerLanguage,
    onDestinationSelected: (DrawerDestination) -> Unit,
    onLanguageSelected: (DrawerLanguage) -> Unit,
    drawerOpen: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val panelShape = RoundedCornerShape(28.dp)
    val isPersian = selectedLanguage == DrawerLanguage.PERSIAN
    val context = LocalContext.current.applicationContext
    val engineStore = remember(context) { EngineModeStore.get(context) }
    val engineMode by engineStore.mode.collectAsStateWithLifecycle()
    val items = remember(engineMode) { drawerItemsFor(engineMode) }
    val firstItemFocus = remember { FocusRequester() }
    val lastNavFocus = remember { FocusRequester() }
    val languageFocus = remember { FocusRequester() }
    val navBottomFocus = if (items.size <= 1) firstItemFocus else lastNavFocus

    LaunchedEffect(drawerOpen, items.firstOrNull()?.destination) {
        if (!drawerOpen) return@LaunchedEffect
        delay(80)
        runCatching { firstItemFocus.requestFocus() }
    }

    CompositionLocalProvider(LocalHomePersian provides isPersian) {
        BoxWithConstraints(
            modifier = modifier
            .graphicsLayer()
            .shadow(
                elevation = 10.dp,
                shape = panelShape,
                ambientColor = DrawerBlue.copy(alpha = 0.16f),
                spotColor = DrawerBlue.copy(alpha = 0.18f),
            )
            .clip(panelShape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0A192C),
                        Color(0xFF071526),
                        Color(0xFF09182B),
                    ),
                ),
            )
            .border(1.dp, Color(0x4A34516F), panelShape),
        ) {
            val compact = maxHeight < 720.dp

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical),
                    ),
            ) {

            val viewportHeight = maxHeight
            // Fills the panel when there is room (bottom block sits at the bottom, no gap under it),
            // and scrolls when the screen is too short.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = viewportHeight)
                    .padding(horizontal = 17.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                DrawerHeader(compact = compact, engineMode = engineMode)
            HorizontalDivider(color = DrawerDivider, thickness = 1.dp)
            Spacer(Modifier.height(if (compact) 6.dp else 10.dp))

            items.forEachIndexed { index, item ->
                AnimatedDrawerRow(
                    selected = item.destination == selectedDestination,
                    animate = item.destination == DrawerDestination.SUPPORT,
                    onClick = { onDestinationSelected(item.destination) },
                ) {
                DrawerNavItem(
                    item = item,
                    selected = item.destination == selectedDestination,
                    compact = compact,
                    engineMode = engineMode,
                    onClick = { onDestinationSelected(item.destination) },
                    enabled = drawerOpen,
                    modifier = Modifier
                        .then(if (index == 0) Modifier.focusRequester(firstItemFocus) else Modifier)
                        .then(
                            if (index == items.lastIndex && index != 0) {
                                Modifier.focusRequester(lastNavFocus)
                            } else {
                                Modifier
                            },
                        )
                        .then(
                            if (index == items.lastIndex) {
                                Modifier.dpadMovesFocus(
                                    down = languageFocus,
                                )
                            } else {
                                Modifier
                            },
                        )
                        .keepFocusInDrawerHorizontally(consumeUp = index == 0),
                )
                }   // close AnimatedDrawerRow
                if (index != items.lastIndex) {
                    Spacer(Modifier.height(if (compact) 1.dp else 3.dp))
                }
            }
            }   // close the header + nav Column

            Column {
            Spacer(Modifier.height(if (compact) 8.dp else 14.dp))
            HorizontalDivider(color = DrawerDivider, thickness = 1.dp)
            Spacer(Modifier.height(if (compact) 6.dp else 10.dp))
            DrawerLanguageRow(
                selectedLanguage = selectedLanguage,
                onLanguageSelected = onLanguageSelected,
                compact = compact,
                enabled = drawerOpen,
                languageFocus = languageFocus,
                upFocus = lastNavFocus,
            )
            Spacer(Modifier.height(if (compact) 4.dp else 8.dp))
            }
            }
            }
        }
    }
}

@Composable
private fun DrawerHeader(compact: Boolean, engineMode: EngineMode) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = if (compact) 4.dp else 14.dp,
                bottom = if (compact) 6.dp else 12.dp,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Outlined.VerifiedUser,
            contentDescription = null,
            tint = DrawerBlue,
            modifier = Modifier.size(if (compact) 28.dp else 38.dp),
        )
        Spacer(Modifier.height(if (compact) 4.dp else 9.dp))
        Text(
            text = when {
                engineMode.isTor -> "XioVPN Tor"
                engineMode.isPow -> "XioVPN PoW"
                engineMode.isDirect -> "XioVPN Direct"
                else -> "XioVPN"
            },
            color = DrawerText,
            fontSize = if (compact) 16.sp else 19.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.3).sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(if (compact) 1.dp else 3.dp))
        Text(
            text = homeText("Secure network tools", "ابزارهای امن شبکه"),
            color = DrawerMuted,
            fontSize = if (compact) 10.sp else 12.sp,
            fontWeight = FontWeight.Normal,
            fontFamily = homeLocalizedFont(),
            letterSpacing = 0.25.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun DrawerNavItem(
    item: DrawerItem,
    selected: Boolean,
    compact: Boolean,
    engineMode: EngineMode,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(16.dp)
    val background = if (selected) {
        Brush.horizontalGradient(
            colors = listOf(
                DrawerBlue.copy(alpha = 0.18f),
                DrawerBlue.copy(alpha = 0.075f),
            ),
        )
    } else {
        Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(if (compact) 35.dp else 46.dp)
            .clip(shape)
            .background(background)
            .then(
                if (selected) Modifier.border(1.dp, DrawerBlue.copy(alpha = 0.19f), shape)
                else Modifier,
            )
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(3.dp)
                    .height(if (compact) 20.dp else 26.dp)
                    .background(DrawerBlue, RoundedCornerShape(50)),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = if (compact) 14.dp else 18.dp,
                    end = 12.dp,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = if (selected) Color(0xFFAAD7FF) else Color(0xFF95A9C4),
                modifier = Modifier.size(if (compact) 18.dp else 22.dp),
            )
            Spacer(Modifier.width(if (compact) 11.dp else 14.dp))
            Text(
                text = homeText(
                    item.label,
                    when (item.destination) {
                        DrawerDestination.HOME -> "خانه"
                        DrawerDestination.CONFIGS -> if (engineMode.isTor || engineMode.isPow) "انتخاب کشور" else "کانفیگ‌ها"
                        DrawerDestination.SNI_MAKER -> "ساخت کانفیگ"
                        DrawerDestination.ROUTE_SPEED_TEST -> "تست سرعت مسیر"
                        DrawerDestination.LIVE_LOGS -> "لاگ‌ها"
                        DrawerDestination.APP_BYPASS -> "عبور انتخابی برنامه‌ها"
                        DrawerDestination.SETTINGS -> "تنظیمات کلی"
                        DrawerDestination.ADVANCED_SETTINGS -> "تنظیمات Xray"
                        DrawerDestination.POW_SETTINGS -> "تنظیمات PoW"
                        DrawerDestination.TOR_SETTINGS -> "تنظیمات Tor"
                        DrawerDestination.DIRECT_SETTINGS -> "تنظیمات Direct"
                        DrawerDestination.GLOW_SETTINGS -> "رقص نور"
                        DrawerDestination.APPEARANCE -> "ظاهر برنامه"
                        DrawerDestination.SUPPORT -> "پشتیبانی"
                    },
                ),
                color = if (selected) DrawerText else Color(0xFFC2CEE0),
                fontSize = if (compact) 12.5.sp else 14.sp,
                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                fontFamily = homeLocalizedFont(),
            )
        }
    }
}

@Composable
private fun DrawerLanguageRow(
    selectedLanguage: DrawerLanguage,
    onLanguageSelected: (DrawerLanguage) -> Unit,
    compact: Boolean,
    enabled: Boolean = true,
    languageFocus: FocusRequester,
    upFocus: FocusRequester,
) {
    val persianFocus = remember { FocusRequester() }
    val englishFocus = remember { FocusRequester() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (compact) 40.dp else 50.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.Language,
                contentDescription = null,
                tint = Color(0xFFA9BAD0),
                modifier = Modifier.size(if (compact) 18.dp else 21.dp),
            )
            Spacer(Modifier.width(if (compact) 6.dp else 8.dp))
            Text(
                text = homeText("Language", "زبان"),
                color = Color(0xFFC8D4E5),
                fontSize = if (compact) 11.sp else 12.5.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = homeLocalizedFont(),
            )
        }

        Row(
            modifier = Modifier
                .width(if (compact) 126.dp else 142.dp)
                .height(if (compact) 31.dp else 36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF07111F))
                .border(1.dp, Color(0x393B5875), RoundedCornerShape(12.dp))
                .padding(3.dp),
        ) {
            LanguageSegment(
                text = "فارسی",
                selected = selectedLanguage == DrawerLanguage.PERSIAN,
                compact = compact,
                localizedFont = true,
                onClick = { onLanguageSelected(DrawerLanguage.PERSIAN) },
                enabled = enabled,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(persianFocus)
                    .then(
                        if (selectedLanguage == DrawerLanguage.PERSIAN) {
                            Modifier.focusRequester(languageFocus)
                        } else {
                            Modifier
                        },
                    )
                    .drawerLanguageDpad(right = englishFocus)
                    .dpadMovesFocus(up = upFocus),
            )
            LanguageSegment(
                text = "English",
                selected = selectedLanguage == DrawerLanguage.ENGLISH,
                compact = compact,
                localizedFont = false,
                onClick = { onLanguageSelected(DrawerLanguage.ENGLISH) },
                enabled = enabled,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(englishFocus)
                    .then(
                        if (selectedLanguage == DrawerLanguage.ENGLISH) {
                            Modifier.focusRequester(languageFocus)
                        } else {
                            Modifier
                        },
                    )
                    .drawerLanguageDpad(left = persianFocus)
                    .dpadMovesFocus(up = upFocus),
            )
        }
    }
}

@Composable
private fun LanguageSegment(
    text: String,
    selected: Boolean,
    compact: Boolean,
    localizedFont: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(9.dp))
            .background(if (selected) DrawerBlue.copy(alpha = 0.88f) else Color.Transparent)
            .clickable(enabled = enabled, role = Role.RadioButton, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (selected) Color.White else DrawerMuted,
            fontSize = if (compact) 9.sp else 10.5.sp,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            fontFamily = if (localizedFont) homeLocalizedFont() else null,
            maxLines = 1,
        )
    }
}
