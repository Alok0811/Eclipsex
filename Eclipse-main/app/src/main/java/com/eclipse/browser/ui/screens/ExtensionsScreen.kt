package com.eclipse.browser.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eclipse.browser.EclipseConfig
import com.eclipse.browser.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

// ── DATA MODELS ──
data class Extension(
    val id: String,
    val name: String,
    val description: String,
    val matchRules: List<String>,
    val scriptCode: String,
    var isEnabled: Boolean,
    val installedAt: Long,
    val source: String,
    val icon: String = "🧩"
)

data class DiscoverScript(
    val id: Int,
    val name: String,
    val description: String,
    val url: String,
    val totalInstalls: Int,
    val rating: Double?,
    val namespace: String
)

@Composable
fun ExtensionsScreen(
    extensions: List<Extension>,
    accentColor: Color,
    onToggle: (String, Boolean) -> Unit,
    onRemove: (String) -> Unit,
    onInstall: (DiscoverScript) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Section 18: Two tabs — INSTALLED and DISCOVER
    var selectedTab by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    // Sliding tab indicator offset with spring animation
    val indicatorOffset by animateFloatAsState(
        targetValue = selectedTab.toFloat(),
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 380f),
        label = "tabIndicator"
    )

    Column(modifier = modifier.fillMaxSize()) {

        // ── HEADER ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF08080F))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp).clip(CircleShape)
                    .background(EclipseSurface)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Text("←", color = TextPrimary, fontSize = 18.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = "Extensions",
                    fontFamily = Outfit, fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp, color = TextPrimary
                )
                Text(
                    text = "${extensions.size} installed",
                    fontFamily = SpaceMono, fontSize = 9.sp,
                    color = TextMuted2, letterSpacing = 1.sp
                )
            }
        }

        Box(Modifier.fillMaxWidth().height(1.dp).background(EclipseBorder))

        // Section 18: Tab row with SLIDING glassmorphism indicator (like WhatsApp)
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth()
                .background(Color(0xFF08080F))
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            val tabWidth = maxWidth / 2

            // Sliding glass indicator
            Box(
                modifier = Modifier
                    .width(tabWidth)
                    .height(36.dp)
                    .offset(x = tabWidth * indicatorOffset)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                accentColor.copy(alpha = 0.14f),
                                accentColor.copy(alpha = 0.08f)
                            )
                        )
                    )
                    .border(1.dp, accentColor.copy(alpha = 0.22f), RoundedCornerShape(10.dp))
            )

            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("INSTALLED", "DISCOVER").forEachIndexed { index, label ->
                    Box(
                        modifier = Modifier
                            .weight(1f).height(36.dp)
                            .clickable { selectedTab = index },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontFamily = SpaceMono,
                            fontSize = 10.sp,
                            letterSpacing = 2.sp,
                            color = if (selectedTab == index) accentColor else TextMuted2,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        Box(Modifier.fillMaxWidth().height(1.dp).background(EclipseBorder))

        // Section 18: Content slides left/right with spring animation
        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = {
                if (targetState > initialState) {
                    (slideInHorizontally(
                        animationSpec = spring(dampingRatio = 0.7f, stiffness = 300f)
                    ) { it } + fadeIn()) togetherWith
                            (slideOutHorizontally(
                                animationSpec = spring(dampingRatio = 0.7f, stiffness = 300f)
                            ) { -it } + fadeOut())
                } else {
                    (slideInHorizontally(
                        animationSpec = spring(dampingRatio = 0.7f, stiffness = 300f)
                    ) { -it } + fadeIn()) togetherWith
                            (slideOutHorizontally(
                                animationSpec = spring(dampingRatio = 0.7f, stiffness = 300f)
                            ) { it } + fadeOut())
                }
            },
            label = "extContent"
        ) { tab ->
            when (tab) {
                0 -> InstalledTab(
                    extensions = extensions,
                    accentColor = accentColor,
                    onToggle = onToggle,
                    onRemove = onRemove
                )
                1 -> DiscoverTab(
                    accentColor = accentColor,
                    installedIds = extensions.map { it.id }.toSet(),
                    onInstall = onInstall
                )
            }
        }
    }
}

// ── INSTALLED TAB ──
// Section 18: Skeuomorphic cards — look physical and pressable
@Composable
private fun InstalledTab(
    extensions: List<Extension>,
    accentColor: Color,
    onToggle: (String, Boolean) -> Unit,
    onRemove: (String) -> Unit
) {
    if (extensions.isEmpty()) {
        // Section 18: Empty state
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(40.dp)
            ) {
                Text("🧩", fontSize = 48.sp)
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "No extensions installed yet.",
                    fontFamily = Outfit, fontWeight = FontWeight.Medium,
                    fontSize = 16.sp, color = TextPrimary, textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Go to Discover to find some.",
                    fontFamily = Outfit, fontSize = 14.sp,
                    color = TextMuted, textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(extensions) { ext ->
            ExtensionCard(
                ext = ext,
                accentColor = accentColor,
                onToggle = { onToggle(ext.id, it) },
                onRemove = { onRemove(ext.id) }
            )
        }
    }
}

// Section 18: Skeuomorphic card — physical depth, real toggle switch
@Composable
private fun ExtensionCard(
    ext: Extension,
    accentColor: Color,
    onToggle: (Boolean) -> Unit,
    onRemove: () -> Unit
) {
    var isEnabled by remember(ext.id, ext.isEnabled) { mutableStateOf(ext.isEnabled) }

    // Skeuomorphic card — depth via shadow + inner highlights
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isEnabled) 6.dp else 2.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = if (isEnabled) accentColor.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.3f),
                spotColor = if (isEnabled) accentColor.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.2f)
            )
            .clip(RoundedCornerShape(16.dp))
            .background(
                // Skeuomorphic gradient — lighter top, darker bottom
                Brush.verticalGradient(
                    listOf(Color(0xFF1A1A2A), Color(0xFF0F0F1E))
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.08f), // top highlight
                        Color.Black.copy(alpha = 0.3f)  // bottom shadow
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon with skeuomorphic raised button look
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .shadow(4.dp, CircleShape)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    accentColor.copy(alpha = 0.2f),
                                    accentColor.copy(alpha = 0.05f)
                                )
                            )
                        )
                        .border(1.dp, accentColor.copy(alpha = 0.25f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(ext.icon, fontSize = 20.sp)
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = ext.name,
                        fontFamily = Outfit, fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp, color = TextPrimary,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = ext.description,
                        fontFamily = Outfit, fontSize = 11.sp,
                        color = TextMuted, maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.width(12.dp))

                // Section 18: Skeuomorphic toggle — looks like real physical switch
                SkeuomorphicToggle(
                    isOn = isEnabled,
                    accentColor = accentColor,
                    onToggle = {
                        isEnabled = it
                        onToggle(it)
                    }
                )
            }

            // Remove button — small, below card content
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "Remove",
                    fontFamily = SpaceMono,
                    fontSize = 9.sp,
                    letterSpacing = 1.sp,
                    color = TextMuted2,
                    modifier = Modifier.clickable { onRemove() }
                )
            }
        }
    }
}

// Section 18: Physical-looking toggle switch
@Composable
private fun SkeuomorphicToggle(
    isOn: Boolean,
    accentColor: Color,
    onToggle: (Boolean) -> Unit
) {
    val thumbPosition by animateFloatAsState(
        targetValue = if (isOn) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 500f),
        label = "thumb"
    )
    val trackColor by animateColorAsState(
        targetValue = if (isOn) accentColor else Color(0xFF2A2A3A),
        animationSpec = tween(200),
        label = "trackColor"
    )

    Box(
        modifier = Modifier
            .width(48.dp)
            .height(26.dp)
            .shadow(3.dp, RoundedCornerShape(13.dp))
            .clip(RoundedCornerShape(13.dp))
            .background(trackColor)
            // Inner shadow effect for skeuomorphism
            .border(
                1.dp,
                Color.Black.copy(alpha = 0.3f),
                RoundedCornerShape(13.dp)
            )
            .clickable { onToggle(!isOn) },
        contentAlignment = Alignment.CenterStart
    ) {
        // Thumb
        Box(
            modifier = Modifier
                .padding(3.dp)
                .offset(x = (22.dp * thumbPosition))
                .size(20.dp)
                .shadow(2.dp, CircleShape)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(Color.White, Color(0xFFDDDDDD))
                    )
                )
        )
    }
}

// ── DISCOVER TAB ──
// Section 18: Greasy Fork library search
@Composable
private fun DiscoverTab(
    accentColor: Color,
    installedIds: Set<String>,
    onInstall: (DiscoverScript) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var scripts by remember { mutableStateOf<List<DiscoverScript>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var hasSearched by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    val client = remember {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    fun search() {
        val q = searchQuery.trim()
        isLoading = true
        hasSearched = true
        focusManager.clearFocus()
        scope.launch {
            try {
                val url = if (q.isBlank()) {
                    "${EclipseConfig.GREASYFORK_API}?page=1&per_page=${EclipseConfig.GREASYFORK_PER_PAGE}"
                } else {
                    "${EclipseConfig.GREASYFORK_API}?q=${q.replace(" ", "+")}&per_page=${EclipseConfig.GREASYFORK_PER_PAGE}"
                }
                val response = withContext(Dispatchers.IO) {
                    val req = Request.Builder().url(url).get().build()
                    client.newCall(req).execute()
                }
                val bodyStr = response.body?.string()
                if (response.isSuccessful && bodyStr != null) {
                    val arr = JSONArray(bodyStr)
                    val result = mutableListOf<DiscoverScript>()
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        result.add(
                            DiscoverScript(
                                id = obj.getInt("id"),
                                name = obj.optString("name", "Unknown"),
                                description = obj.optString("description", ""),
                                url = "https://greasyfork.org/scripts/${obj.getInt("id")}",
                                totalInstalls = obj.optInt("total_installs", 0),
                                rating = obj.optJSONObject("ratings")?.optDouble("average"),
                                namespace = obj.optString("namespace", "")
                            )
                        )
                    }
                    scripts = result
                }
            } catch (e: Exception) {
                // Silently handle — show empty state
            } finally {
                isLoading = false
            }
        }
    }

    // Load popular scripts on first open
    LaunchedEffect(Unit) { search() }

    Column(modifier = Modifier.fillMaxSize()) {

        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = {
                Text("Search scripts...", fontFamily = Outfit, fontSize = 13.sp, color = TextMuted2)
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { search() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accentColor,
                unfocusedBorderColor = EclipseBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                cursorColor = accentColor,
                focusedContainerColor = EclipseSurface,
                unfocusedContainerColor = EclipseSurface
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            textStyle = androidx.compose.ui.text.TextStyle(fontFamily = Outfit, fontSize = 13.sp)
        )

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                com.eclipse.browser.ui.screens.SaturnLoadingSpinner(accentColor = accentColor)
            }
        } else if (scripts.isEmpty() && hasSearched) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No scripts found.\nTry a different search.",
                    fontFamily = Outfit, fontSize = 14.sp,
                    color = TextMuted, textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(scripts) { script ->
                    val isInstalled = script.id.toString() in installedIds
                    DiscoverCard(
                        script = script,
                        accentColor = accentColor,
                        isInstalled = isInstalled,
                        onInstall = { onInstall(script) }
                    )
                }
            }
        }
    }
}

// Section 18: Discover script card
@Composable
private fun DiscoverCard(
    script: DiscoverScript,
    accentColor: Color,
    isInstalled: Boolean,
    onInstall: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(EclipseSurface)
            .border(1.dp, EclipseBorder, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = script.name,
                        fontFamily = Outfit, fontWeight = FontWeight.Medium,
                        fontSize = 13.sp, color = TextPrimary,
                        maxLines = 2, overflow = TextOverflow.Ellipsis
                    )
                    if (script.description.isNotBlank()) {
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = script.description,
                            fontFamily = Outfit, fontSize = 11.sp,
                            color = TextMuted, maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                // Install button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (isInstalled) Color.Transparent else accentColor
                        )
                        .border(
                            1.dp,
                            if (isInstalled) EclipseBorder else accentColor,
                            RoundedCornerShape(20.dp)
                        )
                        .then(if (!isInstalled) Modifier.clickable { onInstall() } else Modifier)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isInstalled) "Installed ✓" else "Install",
                        fontFamily = SpaceMono, fontSize = 9.sp,
                        letterSpacing = 1.sp,
                        color = if (isInstalled) TextMuted2 else Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Stats row
            Spacer(Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (script.totalInstalls > 0) {
                    Text(
                        text = "${formatInstalls(script.totalInstalls)} installs",
                        fontFamily = SpaceMono, fontSize = 8.sp,
                        color = TextMuted2, letterSpacing = 0.5.sp
                    )
                }
                script.rating?.let { r ->
                    Text(
                        text = "★ ${"%.1f".format(r)}",
                        fontFamily = SpaceMono, fontSize = 8.sp,
                        color = accentColor.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

private fun formatInstalls(count: Int): String {
    return when {
        count >= 1_000_000 -> "${"%.1f".format(count / 1_000_000f)}M"
        count >= 1_000 -> "${"%.1f".format(count / 1_000f)}K"
        else -> count.toString()
    }
}
