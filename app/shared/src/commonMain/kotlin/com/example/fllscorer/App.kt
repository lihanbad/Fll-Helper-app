package com.example.fllscorer

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import fllscorer.app.shared.generated.resources.*
import fllscorer.app.shared.generated.resources.Res

// ---------------------------------------------------------------- THEMES

private val MONO = FontFamily.Monospace

/**
 * One AppTheme = a full journal palette (ink on paper) plus an accent.
 * No purple anywhere (dropped the old VIOLET option).
 */
private data class AppTheme(val name: String, val accent: Color, val ink: Int, val paper: Int, val mute: Int)
private val appThemes = listOf(
    AppTheme("MINT",   Color(0xFF00A884), 0xFF0A1412.toInt(), 0xFFF3FAF7.toInt(), 0xFF6E8B80.toInt()),
    AppTheme("OCEAN",  Color(0xFF0E7BD8), 0xFF0B2431.toInt(), 0xFFE9F3F6.toInt(), 0xFF5D7A85.toInt()),
    AppTheme("SUNSET", Color(0xFFD45A32), 0xFF3B1F14.toInt(), 0xFFFDF4EC.toInt(), 0xFF8A6A5C.toInt()),
    AppTheme("NIGHT",  Color(0xFF2EC4B6), 0xFFE8E8EA.toInt(), 0xFF101014.toInt(), 0xFF87878F.toInt()),
    AppTheme("PAPER",  Color(0xFF0A0A0A), 0xFF0A0A0A.toInt(), 0xFFFAFAFA.toInt(), 0xFF8A8A8A.toInt()),
)
private var selectedTheme by mutableStateOf(appThemes.first())

private val BLACK get() = Color(selectedTheme.ink)
private val WHITE get() = Color(selectedTheme.paper)
private val GRAY get() = Color(selectedTheme.mute)

private class ScoreState {
    /** Stable id -> count/checked. */
    val selections = mutableStateMapOf<String, Int>()
    var tokensLeft by mutableStateOf(6)
    var inspectionBonus by mutableStateOf(true)
    var penalties by mutableStateOf(0)   // Future Edition interference fouls

    /** Recorded official matches (max 3). */
    val matchScores = mutableStateListOf<Int>()
}

private const val MATCH_SECONDS = 150  // 2:30

@Composable
private fun MatchTimer() {
    var secondsLeft by remember { mutableStateOf(MATCH_SECONDS) }
    var running by remember { mutableStateOf(false) }

    LaunchedEffect(running) {
        while (running && secondsLeft > 0) {
            kotlinx.coroutines.delay(1000)
            secondsLeft--
        }
        if (secondsLeft <= 0) running = false
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .background(if (running) WHITE else Color.Transparent)
                .border(2.dp, if (secondsLeft <= 15) WHITE else GRAY)
                .padding(12.dp, 6.dp),
        ) {
            Text(
                run {
                    val m = secondsLeft / 60; val s = secondsLeft % 60
                    "$m:" + s.toString().padStart(2, '0')
                },
                color = if (running) BLACK else WHITE,
                fontFamily = MONO, fontWeight = FontWeight.Black, fontSize = 22.sp,
                letterSpacing = 2.sp,
            )
        }
        Spacer(Modifier.width(10.dp))
        Box(
            Modifier
                .background(if (running) WHITE else Color.Transparent)
                .border(1.dp, WHITE)
                .clickable { running = !running }
                .padding(10.dp, 8.dp),
        ) {
            Text(if (running) "PAUSE" else "START 2:30",
                color = if (running) BLACK else WHITE,
                fontFamily = MONO, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }
        Spacer(Modifier.width(6.dp))
        Box(
            Modifier
                .border(1.dp, GRAY)
                .clickable { running = false; secondsLeft = MATCH_SECONDS }
                .padding(10.dp, 8.dp),
        ) {
            Text("RESET", color = GRAY, fontFamily = MONO, fontSize = 11.sp)
        }
    }
}

private fun tokenPoints(left: Int): Int =
    Bioglow.precisionTokens.firstOrNull { it.tokensLeft == left }?.points ?: 0

@Composable
private fun missionPainter(mission: Mission, edition: Edition) = painterResource(
    if (edition == Edition.FUTURE)
        when (mission.number) {
            1 -> Res.drawable.fe_01; 2 -> Res.drawable.fe_02
            3 -> Res.drawable.fe_03; 4 -> Res.drawable.fe_04
            5 -> Res.drawable.fe_05; else -> Res.drawable.fe_06
        }
    else
        when (mission.number) {
            1 -> Res.drawable.mission_01; 2 -> Res.drawable.mission_02
            3 -> Res.drawable.mission_03; 4 -> Res.drawable.mission_04
            5 -> Res.drawable.mission_05; 6 -> Res.drawable.mission_06
            7 -> Res.drawable.mission_07; 8 -> Res.drawable.mission_08
            9 -> Res.drawable.mission_09; 10 -> Res.drawable.mission_10
            11 -> Res.drawable.mission_11; 12 -> Res.drawable.mission_12
            13 -> Res.drawable.mission_13; 14 -> Res.drawable.mission_14
            else -> Res.drawable.mission_15
        }
)

private enum class Edition(val label: String) {
    FOUNDERS("FOUNDERS"), FUTURE("FUTURE");
}

@Composable
fun App(
    setupRequired: Boolean = false,
    initialEdition: String = "FOUNDERS",
    initialTheme: String = "MINT",
    showUpdateControls: Boolean = false,
    updateMessage: String? = null,
    updateAvailable: Boolean = false,
    onSetupComplete: (String, String) -> Unit = { _, _ -> },
    onCheckForUpdates: () -> Unit = {},
    onInstallUpdate: () -> Unit = {},
) {
    MaterialThemeSet()
    var setupDone by remember(setupRequired) { mutableStateOf(!setupRequired) }
    var tab by remember { mutableStateOf(0) }
    var edition by remember(initialEdition) {
        val persisted = loadSetting("edition")
        mutableStateOf(
            Edition.entries.firstOrNull { persisted != null && it.name == persisted }
                ?: Edition.entries.firstOrNull { it.name == initialEdition }
                ?: Edition.FOUNDERS
        )
    }
    var dropdownOpen by remember { mutableStateOf(false) }
    var themeMenuOpen by remember { mutableStateOf(false) }
    val tabs = listOf("MISSIONS", "ITEMS", "SCORER", "BOOK", "GUIDE", "BUILD")

    LaunchedEffect(initialTheme) {
        val persisted = loadSetting("theme")
        selectedTheme =
            appThemes.firstOrNull { persisted != null && it.name == persisted }
                ?: appThemes.firstOrNull { it.name == initialTheme }
                ?: appThemes.first()
    }

    if (!setupDone) {
        FirstLaunchSetup(
            edition = edition,
            theme = selectedTheme,
            onEditionSelected = { edition = it },
            onThemeSelected = { selectedTheme = it },
            onContinue = {
                setupDone = true
                onSetupComplete(edition.name, selectedTheme.name)
            },
        )
        return
    }

    Column(Modifier.background(WHITE).fillMaxSize()) {
        // Header
        Column(Modifier.background(BLACK).fillMaxWidth().padding(20.dp, 24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("FLL SCORER", color = WHITE, fontFamily = MONO,
                        fontWeight = FontWeight.Black, fontSize = 28.sp, letterSpacing = 4.sp)
                    Text("BIOGLOW 2026-27 // MISSION GUIDE + SCORE SHEET",
                        color = GRAY, fontFamily = MONO, fontSize = 11.sp, letterSpacing = 2.sp)
                }
                Column {
                    Box(Modifier.border(1.dp, WHITE).clickable { themeMenuOpen = !themeMenuOpen }
                        .padding(horizontal = 9.dp, vertical = 7.dp)) {
                        Text("● ${selectedTheme.name}", color = selectedTheme.accent,
                            fontFamily = MONO, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    }
                    if (themeMenuOpen) {
                        Column(Modifier.background(BLACK).border(1.dp, WHITE)) {
                            appThemes.forEach { theme ->
                                Text(theme.name, color = theme.accent,
                                    fontFamily = MONO, fontWeight = FontWeight.Bold, fontSize = 11.sp,
                                    modifier = Modifier.fillMaxWidth().clickable {
                                         selectedTheme = theme
                                         saveSetting("theme", theme.name)
                                         themeMenuOpen = false
                                    }.padding(horizontal = 12.dp, vertical = 8.dp))
                            }
                        }
                    }
                }
                Spacer(Modifier.width(8.dp))
                // ---- Edition dropdown ----
                Column {
                    Box(
                        Modifier
                            .background(WHITE)
                            .clickable { dropdownOpen = !dropdownOpen }
                            .padding(10.dp, 8.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(edition.label, color = BLACK, fontFamily = MONO,
                                fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Spacer(Modifier.width(8.dp))
                            Text(if (dropdownOpen) "\u25b2" else "\u25bc", color = BLACK, fontFamily = MONO, fontSize = 11.sp)
                        }
                    }
                    if (dropdownOpen) {
                        Column(
                            Modifier
                                .background(BLACK)
                                .border(2.dp, WHITE)
                                .width(190.dp),
                        ) {
                            Edition.entries.forEach { e ->
                                Column(
                                    Modifier
                                        .background(if (e == edition) WHITE else BLACK)
                                        .clickable {
                                            edition = e
                                            saveSetting("edition", e.name)
                                            dropdownOpen = false
                                        }
                                        .fillMaxWidth()
                                        .padding(12.dp, 10.dp),
                                ) {
                                    Text(
                                        if (e == Edition.FOUNDERS) "FOUNDERS EDITION" else "FUTURE EDITION",
                                        color = if (e == edition) BLACK else WHITE,
                                        fontFamily = MONO, fontWeight = FontWeight.Bold, fontSize = 12.sp,
                                    )
                                    Text(
                                        if (e == Edition.FOUNDERS)
                                            "SPIKE // 15 missions // solo field" else
                                            "CS+AI // shared field // 4 roles",
                                        color = if (e == edition) GRAY else GRAY,
                                        fontFamily = MONO, fontSize = 9.sp,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        if (showUpdateControls) {
            Row(
                Modifier.fillMaxWidth().background(BLACK).padding(horizontal = 16.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(updateMessage ?: "UPSTREAM RELEASE UPDATES", color = GRAY,
                    fontFamily = MONO, fontSize = 9.sp, modifier = Modifier.weight(1f))
                if (updateAvailable) {
                    Text("INSTALL UPDATE", color = selectedTheme.accent,
                        fontFamily = MONO, fontWeight = FontWeight.Bold, fontSize = 10.sp,
                        modifier = Modifier.clickable(onClick = onInstallUpdate).padding(6.dp))
                }
                Text("CHECK", color = WHITE, fontFamily = MONO,
                    fontWeight = FontWeight.Bold, fontSize = 10.sp,
                    modifier = Modifier.clickable(onClick = onCheckForUpdates).padding(6.dp))
            }
        }
        // Tabs
        ScrollableTabRow(
            selectedTabIndex = tab,
            containerColor = BLACK,
            contentColor = WHITE,
            edgePadding = 0.dp,
        ) {
            tabs.forEachIndexed { i, t ->
                Column(
                    Modifier
                        .background(if (tab == i) selectedTheme.accent else BLACK)
                        .clickable { tab = i }
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(t, color = if (tab == i) WHITE else GRAY,
                        fontFamily = MONO, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
        when (tab) {
            0 -> MissionsTab(edition)
            1 -> ItemsTab(edition)
            2 -> ScorerTab(edition)
            3 -> BookTab(edition)
            4 -> RulesTab(edition)
            else -> BuildInstructionsTab(edition)
        }
    }
}

@Composable
private fun FirstLaunchSetup(
    edition: Edition,
    theme: AppTheme,
    onEditionSelected: (Edition) -> Unit,
    onThemeSelected: (AppTheme) -> Unit,
    onContinue: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().background(WHITE).verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("WELCOME TO FLL SCORER", color = BLACK, fontFamily = MONO,
            fontWeight = FontWeight.Black, fontSize = 24.sp, letterSpacing = 2.sp)
        Spacer(Modifier.height(8.dp))
        Text("Choose your game edition and app colour. You can change both later.",
            color = GRAY, fontFamily = MONO, fontSize = 13.sp, lineHeight = 19.sp)
        Spacer(Modifier.height(28.dp))
        Section("YOUR GAME EDITION") {
            Edition.entries.forEach { option ->
                val selected = edition == option
                Row(
                    Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        .border(2.dp, if (selected) theme.accent else GRAY)
                        .clickable { onEditionSelected(option) }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(if (selected) "●" else "○", color = theme.accent,
                        fontFamily = MONO, fontSize = 18.sp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(if (option == Edition.FOUNDERS) "FOUNDERS EDITION" else "FUTURE EDITION",
                            color = BLACK, fontFamily = MONO, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(if (option == Edition.FOUNDERS) "SPIKE · 15 missions" else "CS + AI · shared field · 4 roles",
                            color = GRAY, fontFamily = MONO, fontSize = 11.sp)
                    }
                }
            }
        }
        Section("APP THEME") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                appThemes.forEach { option ->
                    Column(
                        Modifier.weight(1f).border(2.dp, if (theme == option) option.accent else GRAY)
                            .clickable { onThemeSelected(option) }.padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("●", color = option.accent, fontSize = 20.sp)
                        Text(option.name, color = BLACK, fontFamily = MONO, fontSize = 9.sp,
                            fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        Box(Modifier.fillMaxWidth().background(theme.accent).clickable(onClick = onContinue)
            .padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
            Text("GET STARTED", color = WHITE, fontFamily = MONO,
                fontWeight = FontWeight.Black, fontSize = 15.sp, letterSpacing = 2.sp)
        }
    }
}

@Composable
private fun MaterialThemeSet() {
    androidx.compose.material3.MaterialTheme(
        colorScheme = androidx.compose.material3.lightColorScheme(
            primary = BLACK,
            onPrimary = WHITE,
            surface = WHITE,
            onSurface = BLACK,
            background = WHITE,
            onBackground = BLACK,
        )
    ) {}
}

// Official Founders Edition build books, grouped the same way as the FIRST/IET PDFs.
private val foundersBuildBooks = listOf(
    "https://education.theiet.org/media/ib0psidw/mission-1-drone-survey-build-bags-1-and-2.pdf",
    "https://education.theiet.org/media/xhkp3lju/mission-2-exploding-seeds-build-bag-3.pdf",
    "https://education.theiet.org/media/azgbh4qj/mission-3-flip-the-rock-build-bags-4-and-5.pdf",
    "https://education.theiet.org/media/fnpd5ejz/mission-4-lucky-leaves-build-bag-4.pdf",
    "https://education.theiet.org/media/dmofjsx1/mission-5-reaching-roots-build-bag-7.pdf",
    "https://education.theiet.org/media/0uqanhkx/missions-6-and-7-leafcutter-frenzy-and-humongus-fungus-build-bags-8-9-and10.pdf",
    "https://education.theiet.org/media/0uqanhkx/missions-6-and-7-leafcutter-frenzy-and-humongus-fungus-build-bags-8-9-and10.pdf",
    "https://education.theiet.org/media/ulfhjv1j/missions-8-and-9-tangled-and-research-platform-build-bags-11-12-13-14-and-15.pdf",
    "https://education.theiet.org/media/ulfhjv1j/missions-8-and-9-tangled-and-research-platform-build-bags-11-12-13-14-and-15.pdf",
    "https://education.theiet.org/media/emjj2kev/mission-10-fragile-microhabitats-build-bag-16.pdf",
    "https://education.theiet.org/media/mgffwkwv/mission-11-window-to-the-past-build-bag-17.pdf",
    "https://education.theiet.org/media/44qaxcih/mission-12-forest-elder-build-bag-18.pdf",
    "https://education.theiet.org/media/3oll0b1x/mission-13-keystone-species-build-bag-19.pdf",
    "https://education.theiet.org/media/v4rhzoh3/mission-14-seeds-of-renewal-build-bag-21.pdf",
    "https://education.theiet.org/media/ysxfmawt/mission-15-biocentric-architecture-build-bags-22-23-and-24.pdf",
)

private const val FUTURE_BUILD_RESOURCES =
    "https://education.lego.com/en-us/first-lego-league/season-materials/"

@Composable
private fun BuildInstructionsTab(edition: Edition) {
    val uriHandler = LocalUriHandler.current
    val missions = EditionMissions(edition)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("MODEL BUILDING INSTRUCTIONS", fontFamily = MONO,
            fontWeight = FontWeight.Black, fontSize = 17.sp, color = BLACK)
        Spacer(Modifier.height(6.dp))
        Text(
            if (edition == Edition.FUTURE)
                "Official LEGO Education build books are grouped by Future Edition model number. Open the resource page and choose Future Edition → Building Instructions."
            else
                "Choose a model below to open its official illustrated build book. Some books cover more than one mission model. Internet access is needed to open the PDFs.",
            fontFamily = MONO, fontSize = 12.sp, lineHeight = 17.sp, color = GRAY,
        )
        Spacer(Modifier.height(14.dp))
        if (edition == Edition.FUTURE) {
            Box(Modifier.fillMaxWidth().background(BLACK).clickable {
                uriHandler.openUri(FUTURE_BUILD_RESOURCES)
            }.padding(12.dp)) {
                Text("OPEN OFFICIAL LEGO BUILD RESOURCES  ↗", color = WHITE,
                    fontFamily = MONO, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
        missions.forEachIndexed { index, mission ->
            Row(
                Modifier.fillMaxWidth().padding(bottom = 10.dp).border(1.dp, GRAY).padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    painter = missionPainter(mission, edition),
                    contentDescription = "${mission.name} model reference",
                    modifier = Modifier.width(96.dp).height(72.dp),
                    contentScale = ContentScale.Fit,
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("M${mission.number.toString().padStart(2, '0')}  ${mission.name}",
                        fontFamily = MONO, fontWeight = FontWeight.Bold, fontSize = 12.sp,
                        color = BLACK)
                    if (edition == Edition.FOUNDERS && mission.bags.isNotEmpty()) {
                        Text("KIT BAGS ${mission.bags.joinToString(", ")}",
                            fontFamily = MONO, fontSize = 10.sp, color = GRAY,
                            modifier = Modifier.padding(top = 3.dp))
                    }
                    Text(
                        if (edition == Edition.FUTURE) "OPEN BUILD RESOURCES  ↗" else "OPEN OFFICIAL BUILD PDF  ↗",
                        fontFamily = MONO, fontWeight = FontWeight.Bold, fontSize = 10.sp,
                        color = selectedTheme.accent,
                        modifier = Modifier.clickable {
                            uriHandler.openUri(if (edition == Edition.FUTURE)
                                FUTURE_BUILD_RESOURCES else foundersBuildBooks[index])
                        }.padding(top = 7.dp, bottom = 3.dp),
                    )
                }
            }
        }
        Text("Build instructions are provided by FIRST / LEGO Education. Follow the diagrams and use the printed bag labels to sort pieces.",
            fontFamily = MONO, fontSize = 10.sp, lineHeight = 14.sp, color = GRAY,
            modifier = Modifier.padding(top = 4.dp, bottom = 24.dp))
    }
}

// ---------------------------------------------------------------- MISSIONS

private fun EditionMissions(edition: Edition) =
    if (edition == Edition.FUTURE) BioglowFuture.missions else Bioglow.missions

@Composable
private fun MissionsTab(edition: Edition) {
    var expanded by remember { mutableStateOf<Int?>(null) }
    var query by remember { mutableStateOf("") }
    val missions = EditionMissions(edition)
    val filtered = missions.filter {
        query.isBlank() ||
            it.name.contains(query, ignoreCase = true) ||
            it.description.contains(query, ignoreCase = true) ||
            it.scoring.any { o -> o.label.contains(query, ignoreCase = true) }
    }
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            // Search bar
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(BLACK)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("> ", color = GRAY, fontFamily = MONO, fontSize = 14.sp)
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontFamily = MONO, fontSize = 13.sp,
                            color = WHITE,
                        ),
                        modifier = Modifier.weight(1f),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(WHITE),
                        decorationBox = { inner ->
                            if (query.isEmpty()) {
                                Text(
                                    "SEARCH ${if (edition == Edition.FUTURE) 6 else 15} MISSIONS...",
                                    color = Color(0xFF6A6A6A), fontFamily = MONO, fontSize = 13.sp,
                                )
                            }
                            inner()
                        },
                    )
                    if (query.isNotEmpty()) {
                        Text("×", color = GRAY, fontFamily = MONO, fontSize = 18.sp,
                            modifier = Modifier.clickable { query = "" }.padding(start = 8.dp))
                    }
                }
            }
        }
        item {
            InfoBanner(
                if (edition == Edition.FUTURE)
                    "FUTURE EDITION // SHARED FIELD, 4 ROLES (DRIVER / OPERATOR / TECHNICIAN / SPECIALIST). " +
                        "AVG OF ALL MATCHES COUNTS. TAP A MISSION FOR FULL RULES."
                else
                    "FOUNDERS EDITION // 15 MISSIONS // 3 MATCHES @ 2:30 // BEST SCORE COUNTS. " +
                        "TAP A MISSION FOR FULL RULES."
            )
        }
        items(filtered) { m ->
            val open = expanded == m.number
            Column(
                Modifier
                    .fillMaxWidth()
                    .border(2.dp, BLACK, RoundedCornerShape(0.dp))
                    .clickable { expanded = if (open) null else m.number }
                    .padding(0.dp),
            ) {
                Row(
                    Modifier.background(if (open) BLACK else WHITE).fillMaxWidth()
                        .clickable { expanded = if (open) null else m.number }
                        .padding(16.dp, 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(46.dp).background(if (open) WHITE else BLACK),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(m.number.toString().padStart(2, '0'), color = if (open) BLACK else WHITE,
                            fontFamily = MONO, fontWeight = FontWeight.Black, fontSize = 18.sp)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(m.name, color = if (open) WHITE else BLACK,
                            fontFamily = MONO, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Row {
                            Text(
                                when {
                                    m.number >= 13 && edition == Edition.FOUNDERS -> "DOCK MISSION"
                                    m.bags.isEmpty() -> "FUTURE EDITION"
                                    else -> "BAG ${m.bags.joinToString(", ")}"
                                },
                                color = GRAY,
                                fontFamily = MONO, fontSize = 11.sp, letterSpacing = 1.sp,
                            )
                            if (m.noEquipmentConstraint) {
                                Text("  // NO-EQUIPMENT", color = if (open) WHITE else BLACK,
                                    fontFamily = MONO, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Box(
                        Modifier.background(if (open) WHITE else BLACK)
                            .border(1.dp, if (open) WHITE else BLACK).padding(6.dp, 3.dp)
                    ) {
                        Text("${m.maxPoints} PTS", color = if (open) BLACK else WHITE,
                            fontFamily = MONO, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
                if (open) {
                    Column(Modifier.padding(16.dp)) {
                        Image(
                            painter = missionPainter(m, edition),
                            contentDescription = "${m.name} mission model",
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            contentScale = ContentScale.Fit,
                        )
                        Row(Modifier.padding(bottom = 8.dp)) {
                            Box(Modifier.background(BLACK).padding(5.dp, 2.dp)) {
                                Text("BAG ${m.bags.joinToString(" + ")}", color = WHITE,
                                    fontFamily = MONO, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                            Spacer(Modifier.width(8.dp))
                            Box(Modifier.border(1.dp, BLACK).padding(5.dp, 2.dp)) {
                                Text(
                                    if (m.noEquipmentConstraint) "NO-EQUIPMENT MISSION" else "EQUIPMENT OK",
                                    color = BLACK, fontFamily = MONO,
                                    fontWeight = FontWeight.Bold, fontSize = 11.sp,
                                )
                            }
                        }
                        Text(m.description, fontFamily = MONO, fontSize = 13.sp,
                            lineHeight = 19.sp, color = BLACK)
                        Spacer(Modifier.height(14.dp))
                        Text("SCORING", color = GRAY, fontFamily = MONO,
                            fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                        m.scoring.forEach { s ->
                            Row(
                                Modifier.fillMaxWidth().padding(top = 8.dp),
                            ) {
                                Box(
                                    Modifier.background(BLACK).padding(5.dp, 2.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text("${s.points}", color = WHITE, fontFamily = MONO,
                                        fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    s.label + (" ${s.unitLabel ?: ""}".trim()),
                                    fontFamily = MONO, fontSize = 13.sp, lineHeight = 18.sp,
                                    color = if (s.isBonus) GRAY else BLACK,
                                )
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        Text("RULES & TIPS", color = GRAY, fontFamily = MONO,
                            fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                        m.notes.forEach {
                            Row(Modifier.padding(top = 6.dp)) {
                                Text("> ", color = BLACK, fontFamily = MONO,
                                    fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text(it, fontFamily = MONO, fontSize = 13.sp,
                                    lineHeight = 18.sp, color = BLACK)
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

// ------------------------------------------------------------------ ITEMS

private fun editionItems(edition: Edition): List<GameItem> =
    if (edition == Edition.FUTURE) futureItems else foundersItems

@Composable
private fun itemPainter(tag: String?) = tag?.let {
    painterResource(
        when (it) {
            "m01" -> Res.drawable.mission_01; "m02" -> Res.drawable.mission_02
            "m03" -> Res.drawable.mission_03; "m04" -> Res.drawable.mission_04
            "m06" -> Res.drawable.mission_06; "m07" -> Res.drawable.mission_07
            "m09" -> Res.drawable.mission_09; "m12" -> Res.drawable.mission_12
            "m13" -> Res.drawable.mission_13; "m15" -> Res.drawable.mission_15
            "f02" -> Res.drawable.fe_02; "f03" -> Res.drawable.fe_03
            "f05" -> Res.drawable.fe_05
            else -> Res.drawable.fe_06
        }
    )
}

@Composable
private fun ItemsTab(edition: Edition) {
    val items = editionItems(edition)
    val names = EditionMissions(edition).associate { it.number to it.name }
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            InfoBanner(
                if (edition == Edition.FUTURE)
                    "GAME ITEMS // FUTURE EDITION — loose tokens, species, cards and hardware. " +
                        "THESE MOVE BETWEEN TEAMS AND SCORING ZONES."
                else
                    "GAME ITEMS // FOUNDERS EDITION — every loose piece your robot handles, " +
                        "with its missions and scoring rules."
            )
        }
        items(items) { item ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .border(2.dp, BLACK),
            ) {
                Row(
                    Modifier
                        .background(BLACK)
                        .fillMaxWidth()
                        .padding(14.dp, 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(item.name, color = WHITE, fontFamily = MONO,
                        fontWeight = FontWeight.Bold, fontSize = 14.sp,
                        modifier = Modifier.weight(1f))
                    item.quantity?.let { qty ->
                        Box(Modifier.background(WHITE).padding(5.dp, 2.dp)) {
                            Text(qty.substringBefore(" ").uppercase(),
                                color = BLACK, fontFamily = MONO,
                                fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        }
                    }
                }
                Column(Modifier.padding(14.dp)) {
                    val qty = item.quantity
                    itemPainter(item.imageTag)?.let { painter ->
                        Image(
                            painter = painter,
                            contentDescription = item.name,
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, BLACK)
                                .padding(bottom = 0.dp),
                            contentScale = ContentScale.Fit,
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    Text(item.origin, fontFamily = MONO, fontSize = 11.sp,
                        lineHeight = 15.sp, color = GRAY)
                    Spacer(Modifier.height(8.dp))
                    if (qty != null) {
                        val first = qty.substringBefore(" ")
                        val rest = qty.substringAfter(" ", missingDelimiterValue = "")
                        Text("QTY: " + (rest.ifEmpty { first }),
                            fontFamily = MONO, fontWeight = FontWeight.Bold,
                            fontSize = 11.sp, color = BLACK)
                        Spacer(Modifier.height(6.dp))
                    }
                    item.notes.forEach {
                        Row(Modifier.padding(bottom = 5.dp)) {
                            Text("> ", color = BLACK, fontFamily = MONO,
                                fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(it, fontFamily = MONO, fontSize = 12.sp,
                                lineHeight = 17.sp)
                        }
                    }
                    if (item.usedBy.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("USED BY:", color = GRAY, fontFamily = MONO,
                                fontWeight = FontWeight.Bold, fontSize = 10.sp)
                            Spacer(Modifier.width(6.dp))
                            item.usedBy.forEach { no ->
                                Box(
                                    Modifier.padding(2.dp)
                                        .border(1.dp, BLACK)
                                        .clickable {
                                            // jump to the missions tab with this mission expanded? simple: nothing (keep quick ref)
                                        }
                                        .padding(6.dp, 2.dp),
                                ) {
                                    Text(
                                        run {
                                            val label = names[no]
                                            "M" + no.toString().padStart(2, '0') +
                                                (label?.let { " ${it.uppercase()}" } ?: "")
                                        },
                                        fontFamily = MONO, fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
        item {
            Spacer(Modifier.height(24.dp))
        }
    }
}

// ------------------------------------------------------------------ SCORER

@Composable
private fun ScorerTab(edition: Edition) {
    val isFuture = edition == Edition.FUTURE
    val state = remember(edition) { ScoreState() }
    Column(Modifier.fillMaxSize()) {
        // Score header
        Column(Modifier.background(BLACK).fillMaxWidth().padding(16.dp)) {
            MatchTimer()
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text("SCORE", color = GRAY, fontFamily = MONO, fontSize = 12.sp)
                Spacer(Modifier.weight(1f))
                Text("${totalScore(state, edition)}", color = WHITE, fontFamily = MONO,
                    fontWeight = FontWeight.Black, fontSize = 44.sp)
                Text(" / ${EditionMax(edition)}",
                    color = GRAY, fontFamily = MONO, fontSize = 13.sp)
            }
            if (!isFuture) {
                Row {
                    // Quick token loss — one tap removes a token
                    Box(
                        Modifier
                            .background(WHITE)
                            .border(2.dp, WHITE)
                            .clickable { if (state.tokensLeft > 0) state.tokensLeft -= 1 }
                            .padding(10.dp, 7.dp),
                    ) {
                        Text(
                            "−1 TOKEN (INTERRUPT)",
                            color = BLACK, fontFamily = MONO,
                            fontWeight = FontWeight.Black, fontSize = 11.sp,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Box(
                        Modifier
                            .border(1.dp, WHITE)
                            .clickable { state.tokensLeft = 6 }
                            .padding(10.dp, 7.dp),
                    ) {
                        Text("RESET 6", color = GRAY, fontFamily = MONO, fontSize = 11.sp)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row {
                    // Inspection bonus
                    ToggleChip(
                        "INSPECTION +20", state.inspectionBonus,
                        onColor = WHITE, offColor = GRAY,
                    ) { state.inspectionBonus = it }
                    Spacer(Modifier.width(8.dp))
                    // Precision tokens (6..0)
                    (6 downTo 0).forEach { tk ->
                        val selected = state.tokensLeft == tk
                        Box(
                            Modifier
                                .padding(2.dp)
                                .background(if (selected) WHITE else Color.Transparent)
                                .border(1.dp, WHITE)
                                .clickable { state.tokensLeft = tk }
                                .padding(7.dp, 4.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                if (tk == 0) "X" else "$tk",
                                color = if (selected) BLACK else GRAY,
                                fontFamily = MONO, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                    Spacer(Modifier.width(6.dp))
                    Text("TOKENS", color = GRAY, fontFamily = MONO, fontSize = 10.sp)
                }
            } else {
                // Future Edition: interference penalty counters
                Row {
                    Box(
                        Modifier
                            .background(WHITE)
                            .border(2.dp, WHITE)
                            .clickable { if (state.penalties < 3) state.penalties += 1 }
                            .padding(10.dp, 7.dp),
                    ) {
                        Text(
                            "+1 INTERFERENCE FOUL  (1st −10 // 2nd −30 // 3rd ZERO)",
                            color = BLACK, fontFamily = MONO,
                            fontWeight = FontWeight.Black, fontSize = 11.sp,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Box(
                        Modifier
                            .border(1.dp, WHITE)
                            .clickable { state.penalties = 0 }
                            .padding(10.dp, 7.dp),
                    ) {
                        Text("FOULS: ${state.penalties}", color = GRAY,
                            fontFamily = MONO, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }
        LazyColumn(Modifier.weight(1f)) {
            items(EditionMissions(edition)) { m ->
                MissionScorerCard(m, state, edition)
            }
            item {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    // Match recorder — save the score of each official match
                    val scores = state.matchScores
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .background(WHITE)
                                .border(2.dp, WHITE)
                                .clickable {
                                    if (scores.size < 3) {
                                        scores.add(totalScore(state, edition))
                                        state.selections.clear()
                                        state.tokensLeft = 6
                                        state.inspectionBonus = true
                                        state.penalties = 0
                                    }
                                }
                                .padding(12.dp, 8.dp),
                        ) {
                            Text(
                                if (scores.size < 3) "SAVE MATCH ${scores.size + 1}"
                                else "ALL 3 MATCHES SAVED",
                                color = BLACK, fontFamily = MONO,
                                fontWeight = FontWeight.Black, fontSize = 11.sp,
                            )
                        }
                        if (scores.isNotEmpty()) {
                            Spacer(Modifier.width(8.dp))
                            Box(
                                Modifier
                                    .border(1.dp, BLACK)
                                    .clickable { scores.clear() }
                                    .padding(10.dp, 7.dp),
                            ) {
                                Text("CLEAR", color = BLACK, fontFamily = MONO,
                                    fontWeight = FontWeight.Bold, fontSize = 10.sp)
                            }
                        }
                    }
                    if (scores.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Row {
                            scores.forEachIndexed { i, s ->
                                Column(Modifier.weight(1f).border(2.dp, BLACK)) {
                                    Box(Modifier.background(BLACK).fillMaxWidth()) {
                                        Text(
                                            "MATCH ${i + 1}", color = WHITE, fontFamily = MONO,
                                            fontSize = 9.sp,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth(),
                                        )
                                    }
                                    Text(
                                        "$s", fontFamily = MONO, fontWeight = FontWeight.Black,
                                        fontSize = 20.sp,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                                Spacer(Modifier.width(6.dp))
                            }
                            if (scores.size < 3) {
                                for (i in scores.size until 3) {
                                    Column(Modifier.weight(1f).border(2.dp, GRAY)) {
                                        Text("MATCH ${i + 1}", color = GRAY, fontFamily = MONO,
                                            fontSize = 9.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth())
                                        Text("--", color = GRAY, fontFamily = MONO,
                                            fontSize = 20.sp, fontWeight = FontWeight.Black,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth())
                                    }
                                    Spacer(Modifier.width(6.dp))
                                }
                            }
                            Column(Modifier.weight(1f)) {
                                Text(
                                    if (edition == Edition.FOUNDERS) "BEST" else "AVG",
                                    fontFamily = MONO, color = GRAY, fontSize = 9.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                Text(
                                    if (edition == Edition.FOUNDERS) "${scores.max()}"
                                    else "${scores.sum() / scores.size}",
                                    fontFamily = MONO, fontWeight = FontWeight.Black,
                                    fontSize = 20.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "RESET RUN",
                        fontFamily = MONO, fontWeight = FontWeight.Bold,
                        color = WHITE, fontSize = 14.sp,
                        modifier = Modifier
                            .background(BLACK)
                            .clickable {
                                state.selections.clear()
                                state.tokensLeft = 6
                                state.inspectionBonus = true
                                state.penalties = 0
                            }
                            .fillMaxWidth()
                            .padding(14.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                    Text(
                        "Base = best of 3 matches. Gracious Professionalism (C/V) is " +
                            "scored separately in judging, not here.",
                        color = GRAY, fontFamily = MONO, fontSize = 10.sp,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

private fun totalScore(state: ScoreState, @Suppress("UNUSED_PARAMETER") edition: Edition): Int {
    val missions = if (edition == Edition.FUTURE) BioglowFuture.missions else Bioglow.missions
    var sum = 0
    missions.forEach { m ->
        var missionSum = 0
        m.scoring.forEach { o ->
            val n = state.selections[o.id] ?: 0
            missionSum += o.points * n
        }
        sum += missionSum
    }
    if (edition == Edition.FOUNDERS) {
        if (state.inspectionBonus) sum += Bioglow.inspectionBonus
        sum += tokenPoints(state.tokensLeft)
    } else {
        when (state.penalties) {
            1 -> sum -= 10
            2 -> sum -= 30
            3 -> return 0
        }
    }
    return sum
}

private fun EditionMax(edition: Edition): Int =
    if (edition == Edition.FUTURE) BioglowFuture.totalMaxPoints else Bioglow.totalMaxPoints

@Composable
private fun MissionScorerCard(m: Mission, state: ScoreState, edition: Edition) {
    var open by remember(m.number, edition) { mutableStateOf(false) }
    val reached = m.scoring.sumOf { s ->
        val n = state.selections[s.id] ?: 0
        s.points * n
    }
    Column(
        Modifier.fillMaxWidth().border(1.dp, BLACK).clickable { open = !open },
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(30.dp).background(BLACK), contentAlignment = Alignment.Center) {
                Text(m.number.toString().padStart(2, '0'), color = WHITE, fontFamily = MONO,
                    fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            Spacer(Modifier.width(10.dp))
            Text(m.name, fontFamily = MONO, fontWeight = FontWeight.Bold,
                fontSize = 13.sp, modifier = Modifier.weight(1f))
            Text(
                if (reached > 0) "$reached" else "-",
                fontFamily = MONO, fontWeight = FontWeight.Black, fontSize = 16.sp,
                color = if (reached > 0) BLACK else GRAY,
            )
            Text(" / ${m.maxPoints}", fontFamily = MONO, fontSize = 11.sp, color = GRAY)
        }
        if (open) {
            Column(Modifier.background(Color(0xFFF0F0F0))) {
                m.scoring.forEach { o ->
                    val count = state.selections[o.id] ?: 0
                    if (o.maxCount == 1) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    // radio behavior for Missions 05/10 solo conditions
                                    state.selections[o.id] = if (count > 0) 0 else 1
                                    if (o.id == "m5.partial" && count == 0)
                                        state.selections["m5.full"] = 0
                                    if (o.id == "m5.full" && count == 0)
                                        state.selections["m5.partial"] = 0
                                }
                                .padding(12.dp, 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            ScorerBox(count > 0)
                            Spacer(Modifier.width(10.dp))
                            Text(o.label, fontFamily = MONO, fontSize = 12.sp,
                                lineHeight = 16.sp, modifier = Modifier.weight(1f),
                                color = if (o.isBonus) GRAY else BLACK)
                            Box(Modifier.background(BLACK).padding(5.dp, 2.dp)) {
                                Text("${o.points}", color = WHITE, fontFamily = MONO,
                                    fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    } else {
                        // counting stepper
                        Row(
                            Modifier.fillMaxWidth().padding(12.dp, 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            ScorerBox(count > 0)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                o.label.let { if (o.unitLabel != null) "$it (${o.unitLabel})" else it },
                                fontFamily = MONO, fontSize = 12.sp, lineHeight = 16.sp,
                                modifier = Modifier.weight(1f),
                                color = if (o.isBonus) GRAY else BLACK,
                            )
                            StepButton("\u2212", enabled = count > 0) {
                                state.selections[o.id] = (count - 1).coerceAtLeast(0)
                            }
                            Text("$count", fontFamily = MONO, fontWeight = FontWeight.Bold,
                                fontSize = 15.sp, modifier = Modifier.padding(6.dp, 0.dp))
                            StepButton("+", enabled = count < o.maxCount) {
                                state.selections[o.id] = (count + 1).coerceAtMost(o.maxCount)
                            }
                            Box(Modifier.background(BLACK).padding(5.dp, 2.dp)) {
                                Text("${o.points}", color = WHITE, fontFamily = MONO,
                                    fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun ScorerBox(checked: Boolean) {
    Box(
        Modifier
            .size(22.dp)
            .background(if (checked) BLACK else WHITE)
            .border(2.dp, BLACK),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) Text("X", color = WHITE, fontFamily = MONO,
            fontWeight = FontWeight.Black, fontSize = 13.sp)
    }
}

@Composable
private fun StepButton(sym: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(26.dp)
            .background(if (enabled) BLACK else Color.Transparent)
            .border(1.dp, if (enabled) BLACK else GRAY)
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(sym, color = if (enabled) WHITE else GRAY,
            fontFamily = MONO, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}

@Composable
private fun ToggleChip(label: String, checked: Boolean, onColor: Color, offColor: Color, onToggle: (Boolean) -> Unit) {
    Box(
        Modifier
            .background(if (checked) onColor else Color.Transparent)
            .border(1.dp, WHITE)
            .clickable { onToggle(!checked) }
            .padding(8.dp, 5.dp),
    ) {
        Text(label, color = if (checked) BLACK else offColor,
            fontFamily = MONO, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

// ------------------------------------------------------------------ BOOK

private data class Rule(val number: String?, val text: String)

@Composable
private fun BookTab(edition: Edition) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
        Text(
            if (edition == Edition.FUTURE)
                "FULL RULEBOOK — BIOGLOW FUTURE EDITION 8+ (2026-27)"
            else
                "FULL ROBOT GAME RULEBOOK — BIOGLOW FOUNDERS EDITION (V1, 30082603)",
            fontFamily = MONO, fontWeight = FontWeight.Black, fontSize = 13.sp,
            lineHeight = 18.sp,
        )
        if (edition == Edition.FUTURE) {
            Spacer(Modifier.height(10.dp))
            Text(
                "FUTURE EDITION 8+ // CORE PRINCIPLES: Keep it simple — if something isn't mentioned, " +
                    "it doesn't matter. Benefit of the doubt goes to the team. Head referee's decision " +
                    "is final. ALL match scores are AVERAGED for game rank.",
                fontFamily = MONO, fontSize = 12.sp, lineHeight = 17.sp,
            )
            Spacer(Modifier.height(12.dp))
            Image(
                painter = painterResource(Res.drawable.fe_05),
                contentDescription = "BIOGLOW Future Edition game field",
                modifier = Modifier.fillMaxWidth().border(2.dp, BLACK),
                contentScale = ContentScale.Fit,
            )
            Spacer(Modifier.height(12.dp))
            Section("RULES 3-7 // MATCH & EQUIPMENT") {
                RuleLine("3", "Matches on official event tables, begin with a countdown, last 2:30. " +
                    "Scores come from the FINAL visible condition on each team's side of the shared field.")
                RuleLine("3a", "Teams play at least three official matches; the team's average score " +
                    "determines game rank.")
                RuleLine("4", "Non-electric LEGO bricks in any quantity from any set. No glue, paint or " +
                    "modification. String and tubing may be cut. NO wind-up motors, pneumatics, or extra game models.")
                RuleLine("5", "Hardware ONLY: 2x controller, 2x double motor, 2x single motor, 2x color sensor. " +
                    "+1 required connection card for field hardware, up to 3 optional connection cards, " +
                    "up to 1 laptop/tablet. No charging cords, power banks or mice.")
                RuleLine("6", "Connection cards: 1 required field-hardware card (held by referee) + up to 3 team cards.")
            }
            Section("RULES 8-13 // INSPECTION & ROLES") {
                RuleLine("8", "Required field hardware: 1 double motor (grand tree), 1 color sensor " +
                    "(young forest), 1 single motor (hive), 1 connection card. It counts toward equipment.")
                RuleLine("9", "Driving base and its connection cards start in the TECHNICIAN area; grand-tree " +
                    "device/cards start in the SPECIALIST area; everything else starts in its designated area, " +
                    "max 8 in. (203 mm) tall during inspection.")
                RuleLine("10", "Roles: one player each as DRIVER, OPERATOR, TECHNICIAN and SPECIALIST. " +
                    "Players stay in their areas and may not switch once the match begins.")
                RuleLine("12", "Up to two controllers: 1 Driver + 1 Specialist. Monitored by referees.")
                RuleLine("13", "Nothing stored on the floor; equipment may briefly extend past the field edge " +
                    "but may not be held there for storage.")
            }
            Section("RULES 14-18 // TOOLS") {
                RuleLine("14", "Players may not disassemble or modify mission models, operate another player's " +
                    "tool, or handle items outside their area by hand. Items leave player areas ONLY via a tool.")
                RuleLine("15", "DRIVER: controller + driving base (double motor). May be driven or autonomous. " +
                    "Stuck? Courtesy rule: notify referee; carried items go to the referee — strategic misuse = penalty.")
                RuleLine("16", "OPERATOR: non-electric mechanical tool(s). Only keystone species and resource " +
                    "tokens may leave the area. Mangrove roots: use a tool inside them; once a species/resource " +
                    "is fully out of the tangle roots you may handle it by hand. Retrieval zone: you may retrieve " +
                    "keystone species/tokens by hand if they rest at least partly in the dotted zone.")
                RuleLine("17", "TECHNICIAN: robotic tool with single motor + color sensor; direct motor control, " +
                    "color-triggered, or autonomous. Can modify the driving base (change attachments) when fully " +
                    "inside their area.")
                RuleLine("18", "SPECIALIST: controller + grand-tree double motor + laptop/tablet. May run programs " +
                    "that operate other players' tools. If items rest partly in the Specialist area: tokens go into " +
                    "the token recycler (back into play); other items stay put or go to the referee.")
            }
            Section("RULES 19-22 // PENALTIES") {
                RuleLine("19", "Stranded equipment out of control outside its area stays as-is unless moved by normal gameplay.")
                RuleLine("20", "Interference: no blocking, restricting, or redirecting the other team's movement, " +
                    "access or scoring. Interfering points don't count; points lost by the other team are restored.")
                RuleLine("21", "Interference penalties escalate: 1st = −10, 2nd = −30 more, 3rd = match score ZERO.")
                RuleLine("22", "Referees have final authority over intent, interference and penalties.")
            }
            Spacer(Modifier.height(8.dp))
            Image(
                painter = painterResource(Res.drawable.fe_02),
                contentDescription = "Grand Tree — Roots of Renewal mission model",
                modifier = Modifier.fillMaxWidth().border(2.dp, BLACK),
                contentScale = ContentScale.Fit,
            )
            Text(
                "Grand Tree model — the heart of the shared field (Roots of Renewal + Specialist role).",
                fontFamily = MONO, fontSize = 10.sp, color = GRAY,
                modifier = Modifier.padding(top = 6.dp),
            )
            Spacer(Modifier.height(24.dp))
            return@Column
        }
        Image(
            painter = painterResource(Res.drawable.rb_field),
            contentDescription = "BIOGLOW field overview",
            modifier = Modifier.fillMaxWidth().border(2.dp, BLACK),
            contentScale = ContentScale.Fit,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "THE ROBOT GAME — The robot game is the culmination of your team's work. " +
                "During each 2.5-minute match your robot autonomously completes missions for " +
                "points. Plan your mission strategy, design and program your LEGO robot, " +
                "complete missions, understand scoring, and demonstrate Gracious " +
                "Professionalism. Mission requirements must be visibly met at the end of the " +
                "match unless stated otherwise. You play three official matches and only your " +
                "best score counts toward awards and advancement.",
            fontFamily = MONO, fontSize = 12.sp, lineHeight = 17.sp,
        )
        Spacer(Modifier.height(16.dp))

        Section("RULE 11 // EQUIPMENT") {
            RuleLine("1", "All equipment must be LEGO building pieces, in original factory condition. " +
                "Exception: LEGO string and pneumatic tubes may be cut to length.")
            RuleLine("2", "Teams may use as many non-electric LEGO pieces from any set as they'd like.")
            RuleLine("3", "Electric LEGO equipment (SPIKE Prime, SPIKE Essential, MINDSTORMS " +
                "EV3/Robot Inventor, Powered Up, NXT, RCX) is allowed as shown in the rulebook.")
            RuleLine("3a", "Controller: maximum of ONE in any one match.")
            RuleLine("3b", "Motors: maximum of FOUR (any mix) in any one match.")
            RuleLine("3c", "Sensors: only touch/force, color, distance/ultrasonic, and gyro sensors " +
                "are allowed (any mix and number).")
            RuleLine("3d", "LEGO wires, one controller's power pack OR six AA batteries, and one " +
                "microSD card may be used in any one match.")
            RuleLine("4", "Any software or programming language is allowed. Robots must be autonomous " +
                "outside home. NO remote controllers of any type.")
            RuleLine("5", "One sheet of notebook paper per home area for program notes; does not count as equipment.")
            RuleLine("6", "Additional or duplicate mission models are not allowed.")
        }
        Section("RULES 12-13 // HOME AREAS") {
            RuleLine("7", "Home is divided into two sections, each with its own launch area. Technicians " +
                "may use their hands to handle the robot, equipment, and mission models when these " +
                "items are completely within their home area.")
            RuleLine("8", "Technicians may not hand anything from one home area to the other, touch anything " +
                "outside their home (except for interrupting the robot), or cause anything to move or " +
                "extend outside the area except for launching. Points scored in these ways will not count.")
            RuleLine("9", "When launching: technicians may not keep anything from moving. The robot and " +
                "anything it is about to move must fit completely inside the launch area.")
            RuleLine("10", "Exception: upon returning home, robots may extend past the walls of home without penalty.")
        }
        Section("RULES 14-17 // INTERRUPTING & TOKENS") {
            RuleLine("11", "After any launch, allow the robot and anything it contacts to return completely " +
                "into home before interrupting.")
            RuleLine("12", "If technicians interrupt the robot, it must be relaunched. If the robot or any object " +
                "in contact with it was outside home (even partly), the team loses one PRECISION TOKEN.")
            RuleLine("12a", "Partly outside home => return objects to that same home area. Completely outside => " +
                "they may be placed in either home area.")
            RuleLine("13", "Objects obtained outside home after the robot's last launch must be handed to the " +
                "referee for the remainder of the match.")
            RuleLine("13a", "If the team does not intend to launch again, they may stop their robot in place " +
                "WITHOUT losing a precision token.")
            RuleLine("14", "Teams may not interrupt the robot in a way that earns points from it. Those points don't count.")
            RuleLine("15", "Objects dropped or left outside home by the robot stay where they come to rest, unless " +
                "the robot moves them. If partly in home, technicians may remove it by hand — mission models " +
                "go to the referee; equipment taken into home costs one precision token.")
            RuleLine("16", "Teams may not separate Dual Lock, take models apart, or break a mission model — points " +
                "scored in these ways will not count.")
            RuleLine("17", "Combinations with a model must be loose/simple enough that a technician can free the " +
                "model in perfect original condition in a single motion.")
            RuleLine("17a", "Precision tokens: you start with 6 worth 50 total. Lose one for each interruption " +
                "outside home. Values: 1=10, 2=15, 3=25, 4=35, 5=50, 6=50.")
        }
        Section("RULES 18-20 // FAIR PLAY") {
            RuleLine("18", "Teams may not interact with the opposing team's field, robot, equipment, or shared " +
                "models/interactions, unless there is a mission exception. Interference: the other team scores automatically.")
            RuleLine("19", "Decorative mission-model elements may fall off. Teams should design to avoid damage, but " +
                "referees give benefit of the doubt for reasonable gameplay.")
            RuleLine("20", "Teams may not separate Dual Lock, take models apart, or damage models (see 16).")
        }
        Section("RULES 21-24 // MATCH SETUP & SCORING") {
            RuleLine("21", "At events, matches are played on official tables. Storage: areas left and right of the " +
                "mat, approx. 6.75 x 45 in each; equipment may extend past the left/right walls.")
            RuleLine("22", "After passing inspection, set up: distribute loose models and equipment between the two " +
                "home areas, place the robot in the launch area, calibrate sensors, ask the referee to check things.")
            RuleLine("23", "Team members divide into two groups, one per side, and may not switch during the match. " +
                "Max 2 technicians per home area; members may swap in from the same side.")
            RuleLine("24", "After 2.5 minutes, stop the robot and touch nothing else — scoring begins then.")
            RuleLine("24a", "For scoring, mission requirements must be visibly met at the END of the match unless a " +
                "specific method was required. \"In\" an area includes the lines and airspace above it.")
            RuleLine("24b", "The referee documents results with the team; agreed results become official. Best of three " +
                "matches counts; ties broken by second and third best scores.")
            RuleLine("25", "Gracious Professionalism points are added to the total Core Values score from judging. " +
                "Every team starts at ACCOMPLISHED (3); referees can award EXCEEDS (4) or DEVELOPING (2).")
        }
        Section("INSPECTION BONUS") {
            Text("If your robot and all equipment fit completely in ONE launch area, under a height " +
                "limit of 12 in. (305 mm) during inspection: +20 points.",
                fontFamily = MONO, fontSize = 12.sp, lineHeight = 17.sp)
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "All robot game wording means precisely and only what it says. If a detail is not " +
                "mentioned, it does not matter. If a situation is unclear or hard to call, teams get " +
                "the benefit of the doubt. At an event, your head referee makes the final decision. " +
                "Check official FIRST Challenge Updates — they supersede earlier materials.",
            color = GRAY, fontFamily = MONO, fontSize = 11.sp, lineHeight = 15.sp,
        )
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun RuleLine(no: String?, text: String) {
    Row(Modifier.padding(bottom = 6.dp)) {
        Text(
            (no?.let { "R$no  " } ?: "     "),
            fontFamily = MONO, fontWeight = FontWeight.Bold, fontSize = 12.sp,
            color = BLACK,
        )
        Text(text, fontFamily = MONO, fontSize = 12.sp, lineHeight = 17.sp, color = BLACK)
    }
}

// ------------------------------------------------------------------ RULES

@Composable
private fun RulesTab(edition: Edition) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
        if (edition == Edition.FUTURE) {
            FutureGuide()
        } else {
            FoundersGuide()
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun FoundersGuide() {
    Column {
        Image(
            painter = painterResource(Res.drawable.rb_field),
            contentDescription = "BIOGLOW Founders Edition field",
            modifier = Modifier.fillMaxWidth().border(2.dp, BLACK),
            contentScale = ContentScale.Fit,
        )
        Spacer(Modifier.height(6.dp))
        Text("Founders Edition field — missions 1-15, with the mine / city / farm docks on the right wall.",
            fontFamily = MONO, fontSize = 10.sp, color = GRAY)
        Spacer(Modifier.height(16.dp))
        Section("MATCH-DAY QUICK CHECK") {
            Bullet("Before inspection: check robot size, allowed equipment and every mission model attachment.")
            Bullet("Before the clock: agree who is on each side, confirm the first mission, and stage the next attachment.")
            Bullet("During the 2:30 match: launch only from home; wait for a full return before interrupting.")
            Bullet("At the buzzer: stop and keep hands off the field. Score the final visible state with the referee.")
            Bullet("Between rounds: record the score, note one change, and check official updates before your event.")
        }
        Section("PRACTICE TIPS") {
            Bullet("Make a reliable short route before attempting a long route. A repeatable mission is worth more than an untested plan.")
            Bullet("Test each attachment and launch position several times; write down what failed and change one thing at a time.")
            Bullet("Plan the return path as carefully as the scoring action so the robot gets back to home safely.")
        }
        Section("THE MATCH") {
            Bullet("Match length is 2.5 minutes; the robot is fully autonomous outside home.")
            Bullet("You play three official matches — only your BEST score counts.")
            Bullet("Team splits into two groups, one on each side of the field, that cannot switch.")
            Bullet("Handing anything between home areas = points scored that way don't count.")
        }
        Section("EQUIPMENT & INSPECTION") {
            Bullet("All equipment must be LEGO pieces in original factory condition " +
                "(string and pneumatic tubes may be cut).")
            Bullet("Max: 1 controller, 4 motors, only touch/force, color, distance and gyro sensors.")
            Bullet("Robot + all equipment must fit completely in ONE launch area under " +
                "12 in. (305 mm) at inspection: +20 points.")
            Bullet("Your Mission 13 keystone species counts as equipment — build it with bag 20 " +
                "bricks and know its name for inspection.")
        }
        Section("PRECISION TOKENS") {
            Text("You start with 6 tokens (worth 50 pts). Interrupt the robot outside home " +
                "and you LOSE one. First loss is free-ish, then it gets expensive:",
                fontFamily = MONO, fontSize = 12.sp, lineHeight = 17.sp)
            Spacer(Modifier.height(8.dp))
            Row {
                (0..4).forEach { i ->
                    Column(
                        Modifier.weight(1f).background(BLACK).padding(1.dp)
                    ) {
                        Column(Modifier.background(WHITE).border(1.dp, BLACK).fillMaxWidth()) {
                            Text("${i + 1}", fontFamily = MONO, fontWeight = FontWeight.Bold,
                                fontSize = 13.sp, modifier = Modifier.fillMaxWidth(),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            Box(Modifier.background(BLACK).fillMaxWidth()) {
                                Text("${Bioglow.precisionTokens[i].points}",
                                    color = WHITE, fontFamily = MONO, fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp, modifier = Modifier.fillMaxWidth(),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            }
                        }
                    }
                    Spacer(Modifier.width(4.dp))
                }
                Text("+5", color = GRAY, fontFamily = MONO, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 2.dp, top = 4.dp), fontSize = 13.sp)
            }
            Text("(tokens 5 & 6 remaining both pay 50 — losing 2+ from 6 drops you to " +
                "35 → 25 → 15 → 10.)",
                fontFamily = MONO, fontSize = 11.sp, color = GRAY,
                modifier = Modifier.padding(top = 6.dp))
        }
        Section("NO-EQUIPMENT MISSIONS") {
            Text("For missions marked NO-EQUIPMENT, the model can't earn points if it is " +
                "touching YOUR equipment at the end of the match. Affected: 01, 04, 07, 09, 12, 15. " +
                "Add a small back-off move to your programs!",
                fontFamily = MONO, fontSize = 12.sp, lineHeight = 17.sp)
        }
        Section("SCORING BASICS") {
            Bullet("Mission requirements must be visibly met at the END of the match, " +
                "unless the mission requires a specific method.")
            Bullet("\"In\" an area means the lines and airspace above count as \"in\".")
            Bullet("Don't separate Dual Lock, disassemble, or break models — points don't count.")
            Bullet("Combinations must be loose/simple enough for a referee to free the model " +
                "in one motion.")
            Bullet("Interfering with the other team's field: the other team scores automatically.")
            Bullet("Referee gets final say; benefit of the doubt goes to the team when a call is unclear.")
        }
        Section("NEW THIS YEAR") {
            Bullet("Missions 13–15 use an interchangeable DOCK system (mine / city / farm). " +
                "You choose which model sits on each dock before the match — placement is strategy.")
            Bullet("Trail the docks so your robot doesn't waste travel time.")
        }
        Section("EASY POINTS CHECKLIST") {
            Bullet("Inspection bonus: +20")
            Bullet("Fragile Microhabitats untouched: +20")
            Bullet("Keep 5-6 Precision Tokens: up to +50")
            Text("That's up to 90 points before any mission. Get these first!",
                fontFamily = MONO, fontWeight = FontWeight.Bold, fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp))
        }
        Section("MISSIONS WORTH SEEING UP CLOSE") {
            GuideImage(Res.drawable.mission_08, "M08 Tangled — free the vine to the mat: 30 pts, no bonus, great starter mission.")
            GuideImage(Res.drawable.mission_15, "M15 Biocentric Architecture — dock building with canopy, skylight and compost hatch; 40 max.")
            GuideImage(Res.drawable.mission_13, "M13 Keystone Species — build your OWN animal from bag 20 bricks; counts as equipment!")
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "SOURCE: FLL Challenge BIOGLOW Robot Game Rulebook V1 (30082603). " +
                "Always check official Challenge Updates before your event.",
            color = GRAY, fontFamily = MONO, fontSize = 10.sp, lineHeight = 14.sp,
        )
    }
}

@Composable
private fun FutureGuide() {
    Column {
        Image(
            painter = painterResource(Res.drawable.fe_05),
            contentDescription = "BIOGLOW Future Edition shared field",
            modifier = Modifier.fillMaxWidth().border(2.dp, BLACK),
            contentScale = ContentScale.Fit,
        )
        Spacer(Modifier.height(6.dp))
        Text("The shared game field — two teams play side by side; the black line is the border.",
            fontFamily = MONO, fontSize = 10.sp, color = GRAY)
        Spacer(Modifier.height(16.dp))
        Section("MATCH-DAY QUICK CHECK") {
            Bullet("Before inspection: check each tool, required field hardware, connection cards and starting areas.")
            Bullet("Before the clock: assign all four roles, confirm each player's area, and agree on shared-field communication.")
            Bullet("During the 2:30 match: keep players in their areas and use only the tool allowed for each role.")
            Bullet("At the buzzer: stop and keep hands off the field. Score the final visible state with the referee.")
            Bullet("Between rounds: record the score, review coordination, and check official game updates before your event.")
        }
        Section("PRACTICE TIPS") {
            Bullet("Rehearse handoffs and area boundaries without the clock before combining roles in a full run.")
            Bullet("Coordinate with the other team on shared objectives; avoid blocking their tools or access.")
            Bullet("Start with a reliable route, then add missions after the team can repeat the basics consistently.")
        }
        Section("FOUR ROLES — FOUR TOOLS") {
            Text("Each role has one player and one tool. Any player can attempt any mission, " +
                "roles just guide the strategy.",
                fontFamily = MONO, fontSize = 12.sp, lineHeight = 17.sp)
            Spacer(Modifier.height(8.dp))
            RoleBox("DRIVER", "Controller + driving base (double motor). Driven or autonomous. " +
                "You may NOT touch equipment or models — only your controller. Modifying the base is the Technician's job.")
            RoleBox("OPERATOR", "Non-electric LEGO mechanical tool(s). Places keystone species " +
                "and resource tokens. Tool(s) stay in your area at all times. Retrieval zone around your area: " +
                "you may retrieve species/tokens by hand if they rest at least partly in it.")
            RoleBox("TECHNICIAN", "Robotic tool (single motor + color sensor): motor-driven, " +
                "color-triggered, or autonomous. Only your tool or the driving base may exit your area. " +
                "You can modify the driving base and change attachments while it's fully inside.")
            RoleBox("SPECIALIST", "Controller + grand-tree double motor + laptop/tablet. Runs programs " +
                "that drive other players' tools. Tokens resting in your area go into the token recycler " +
                "(back into play); other items stay put or are forfeited to the referee.")
        }
        Section("MATCH SETUP") {
            Bullet("Inspection: equipment max 8 in. (203 mm) tall, everything starts in its designated area.")
            Bullet("Required field hardware counts toward your kit: 1 double motor (grand tree), " +
                "1 color sensor (young forest), 1 single motor (hive), 1 connection card.")
            Bullet("Up to 2 controllers: 1 Driver + 1 Specialist. Up to 3 optional connection cards for team use.")
            Bullet("Roles are assigned per match and players may NOT switch once the match begins.")
        }
        Section("COOPERTITION IS THE FORMAT") {
            Bullet("The field is shared — your actions can affect BOTH sides.")
            Bullet("Central Haven (M05) scores for BOTH teams. Talk strategy with the other team.")
            Bullet("Cave Waterfall (M03): only the waterfall mechanism may advance the counter.")
            Bullet("Interference penalties: 1st −10, 2nd −30 more, 3rd = match score ZERO.")
        }
        Section("EASY POINTS CHECKLIST") {
            Bullet("Rainforest Awakening: release the nest (30) + hollow tree (20) = 50 quick points.")
            Bullet("Mighty Microbiomes: each placed keystone species = 20; max 3 per microbiome.")
            Bullet("Level Up (optional): 20 per invasive species you dare to add pre-match.")
            Text("Coordinate roles early — a good Operator + Technician lane is worth a lot.",
                fontFamily = MONO, fontWeight = FontWeight.Bold, fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp))
        }
        Section("MODELS WORTH SEEING UP CLOSE") {
            GuideImage(Res.drawable.fe_02, "Grand Tree (bags 1-4) — Roots of Renewal and the Specialist's motor live here; place resources in the base, cycle them up to the canopy chamber.")
            GuideImage(Res.drawable.fe_04, "Hollow Tree & Nest (bags 12-15) — release the resource and keystone-species stash here during Rainforest Awakening.")
            GuideImage(Res.drawable.fe_06, "Hive + invasive species (bags 16-17) — the required invasive queen lives with the hive; invasive placement options sit by the grand tree and nest.")
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "SOURCE: FLL Future Edition 8+ Game Missions + Rulebook (2026-27). " +
                "Always check official Game Updates before your event.",
            color = GRAY, fontFamily = MONO, fontSize = 10.sp, lineHeight = 14.sp,
        )
    }
}

@Composable
private fun GuideImage(resId: DrawableResource, caption: String) {
    Column(Modifier.padding(bottom = 12.dp)) {
        Image(
            painter = painterResource(resId),
            contentDescription = caption,
            modifier = Modifier.fillMaxWidth().border(2.dp, BLACK),
            contentScale = ContentScale.Fit,
        )
        Text(caption, fontFamily = MONO, fontSize = 10.sp, lineHeight = 14.sp,
            color = GRAY, modifier = Modifier.padding(top = 6.dp))
    }
}

@Composable
private fun RoleBox(title: String, text: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .border(1.dp, BLACK)
            .padding(10.dp),
    ) {
        Box(Modifier.background(BLACK).padding(5.dp, 2.dp)) {
            Text(title, color = WHITE, fontFamily = MONO,
                fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 2.sp)
        }
        Spacer(Modifier.height(6.dp))
        Text(text, fontFamily = MONO, fontSize = 12.sp, lineHeight = 17.sp)
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.padding(bottom = 20.dp)) {
        Text(title, fontFamily = MONO, fontWeight = FontWeight.Black,
            fontSize = 15.sp, letterSpacing = 2.sp, color = BLACK,
            modifier = Modifier.padding(bottom = 2.dp))
        Box(
            Modifier.background(selectedTheme.accent).fillMaxWidth().height(3.dp)
        )
        Spacer(Modifier.height(8.dp))
        content()
    }
}

@Composable
private fun Bullet(text: String) {
    Row(Modifier.padding(bottom = 6.dp)) {
        Text("- ", fontFamily = MONO, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        Text(text, fontFamily = MONO, fontSize = 12.sp, lineHeight = 17.sp, color = BLACK)
    }
}

@Composable
private fun InfoBanner(text: String) {
    Box(
        Modifier.fillMaxWidth().background(BLACK).padding(12.dp)
    ) {
        Text(text, color = WHITE, fontFamily = MONO, fontSize = 11.sp, lineHeight = 16.sp)
    }
}


