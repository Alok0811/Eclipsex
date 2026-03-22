package com.eclipse.browser.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.eclipse.browser.ui.components.FilterTabs
import com.eclipse.browser.ui.components.StarFieldCanvas
import com.eclipse.browser.ui.theme.*
import com.eclipse.browser.ui.viewmodel.SearchResponse
import com.eclipse.browser.ui.viewmodel.SearchResult

@Composable
fun SearchResultsScreen(
    query: String,
    searchResponse: SearchResponse?,
    isLoading: Boolean,
    accentColor: Color,
    activeTab: String,
    starsOn: Boolean,
    bgTheme: String,
    onTabSelected: (String) -> Unit,
    onResultClick: (String) -> Unit,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onNewSearch: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val lazyState = rememberLazyListState()
    var pullDistance by remember { mutableStateOf(0f) }
    val pullThreshold = 140f

    // Section 30.9: Infinite scroll — preload next page before bottom
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisible = lazyState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = lazyState.layoutInfo.totalItemsCount
            !isLoading && total > 0 && lastVisible >= total - 3
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) onLoadMore()
    }

    Box(modifier = modifier.fillMaxSize()) {

        // Section 30.13: Stars at 30% opacity during search — faint to not compete with cards
        if (starsOn) {
            StarFieldCanvas(
                isNight = true,
                bgTheme = bgTheme,
                modifier = Modifier.graphicsLayer { alpha = 0.3f }
            )
        }

        Column(modifier = Modifier.fillMaxSize()) {

            // ── HEADER with inline search bar ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF08080F))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(EclipseSurface)
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("←", color = TextPrimary, fontSize = 16.sp)
                }
                Spacer(Modifier.width(12.dp))
                
                // Search bar showing current query, editable for new search
                SearchBarInline(
                    initialQuery = query,
                    accentColor = accentColor,
                    onSearch = onNewSearch,
                    modifier = Modifier.weight(1f)
                )
            }

            Box(Modifier.fillMaxWidth().height(1.dp).background(EclipseBorder))

            // Section 30.3: Filter tabs
            FilterTabs(
                activeTab = activeTab,
                accentColor = accentColor,
                onTabSelected = onTabSelected
            )

            Box(Modifier.fillMaxWidth().height(1.dp).background(EclipseBorder))

            // ── RESULTS ──
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(isLoading) {
                        detectVerticalDragGestures(
                            onVerticalDrag = { change, dragAmount ->
                                val atTop = lazyState.firstVisibleItemIndex == 0 && lazyState.firstVisibleItemScrollOffset == 0
                                if (atTop && dragAmount > 0 && !isLoading) {
                                    pullDistance = (pullDistance + dragAmount).coerceAtMost(220f)
                                    change.consume()
                                }
                            },
                            onDragEnd = {
                                if (pullDistance >= pullThreshold && !isLoading) onRefresh()
                                pullDistance = 0f
                            },
                            onDragCancel = { pullDistance = 0f }
                        )
                    }
            ) {
                when (activeTab) {
                    "all" -> AllTabResults(
                        searchResponse = searchResponse,
                        isLoading = isLoading,
                        accentColor = accentColor,
                        lazyState = lazyState,
                        onResultClick = onResultClick
                    )
                    "images" -> ImageTabResults(
                        results = searchResponse?.results ?: emptyList(),
                        isLoading = isLoading,
                        onResultClick = onResultClick
                    )
                    else -> StandardTabResults(
                        results = searchResponse?.results ?: emptyList(),
                        isLoading = isLoading,
                        accentColor = accentColor,
                        onResultClick = onResultClick
                    )
                }

                val showPullIndicator = isLoading || pullDistance > 0f
                if (showPullIndicator) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 6.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(EclipseSurface.copy(alpha = 0.95f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoading) {
                            SaturnLoadingSpinner(accentColor = accentColor, modifier = Modifier.size(22.dp))
                        } else {
                            Text(
                                text = if (pullDistance >= pullThreshold) "Release to refresh" else "Pull to refresh",
                                fontFamily = SpaceMono,
                                fontSize = 9.sp,
                                color = accentColor,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── ALL TAB — ORBIT CARDS ──
@Composable
private fun AllTabResults(
    searchResponse: SearchResponse?,
    isLoading: Boolean,
    accentColor: Color,
    lazyState: LazyListState,
    onResultClick: (String) -> Unit
) {
    var showIntroLoader by remember(searchResponse, isLoading) {
        mutableStateOf(isLoading && searchResponse == null)
    }
    LaunchedEffect(isLoading, searchResponse) {
        if (isLoading && searchResponse == null) {
            showIntroLoader = true
            kotlinx.coroutines.delay(350)
            if (isLoading && searchResponse == null) showIntroLoader = false
        } else {
            showIntroLoader = false
        }
    }

    if (showIntroLoader) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            SaturnLoadingSpinner(accentColor = accentColor, modifier = Modifier.size(64.dp))
        }
        return
    }

    LazyColumn(
        state = lazyState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {

        // Section 30.5: Skeleton loading — pre-render before network call
        if (isLoading && searchResponse == null) {
            items(5) { index ->
                SkeletonCard(modifier = Modifier.padding(bottom = 12.dp))
            }
        } else {
            // Section 30.10: AI answer card first if available
            searchResponse?.aiAnswer?.let { answer ->
                if (answer.isNotBlank()) {
                    item {
                        AiAnswerCard(
                            answer = answer,
                            accentColor = accentColor,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }
                }
            }

            // Section 30.4/30.6: Orbit cards with staggered fade-up reveal
            itemsIndexed(searchResponse?.results ?: emptyList()) { index, result ->
                OrbitCard(
                    result = result,
                    accentColor = accentColor,
                    animationDelay = (index * 40).coerceAtMost(200),
                    onClick = { onResultClick(result.url) },
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                // Section 30.11: Constellation dot separator
                if (index < (searchResponse?.results?.size ?: 0) - 1) {
                    ConstellationSeparator(accentColor = accentColor)
                }
            }

            // Loading more indicator
            if (isLoading && searchResponse != null) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        SaturnLoadingSpinner(accentColor = accentColor)
                    }
                }
            }
        }
    }
}

// Section 30.4: Eclipse Orbit Card
@Composable
private fun OrbitCard(
    result: SearchResult,
    accentColor: Color,
    animationDelay: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(animationDelay.toLong())
        visible = true
    }

    // Section 30.6: Fade + translate up animation
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(
            animationSpec = tween(200, easing = FastOutSlowInEasing)
        ) + slideInVertically(
            animationSpec = spring(dampingRatio = 0.75f, stiffness = 380f),
            initialOffsetY = { 36 }
        )
    ) {
        var isPressed by remember { mutableStateOf(false) }
        val scale by animateFloatAsState(
            targetValue = if (isPressed) 1.03f else 1f,
            animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
            label = "cardScale"
        )

        Box(
            modifier = modifier
                .fillMaxWidth()
                .graphicsLayer { scaleX = scale; scaleY = scale }
                // Section 30.4: Glassmorphism card
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0x0DFFFFFF))
                .drawBehind {
                    // Section 30.4: Subtle accent glow on edges
                    drawRoundRect(
                        color = accentColor.copy(alpha = 0.08f),
                        cornerRadius = CornerRadius(18.dp.toPx()),
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
                .clickable {
                    isPressed = true
                    onClick()
                }
                .padding(14.dp)
        ) {
            Column {
                // Favicon + domain
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = result.favicon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp).clip(CircleShape),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = result.displayUrl,
                        fontFamily = SpaceMono,
                        fontSize = 9.sp,
                        color = TextMuted2,
                        letterSpacing = 0.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(6.dp))
                // Title
                Text(
                    text = result.title,
                    fontFamily = Outfit,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    color = TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 20.sp
                )
                if (result.snippet.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = result.snippet,
                        fontFamily = Outfit,
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.sp,
                        color = TextMuted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 18.sp
                    )
                }
                result.publishedDate?.let { date ->
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = date,
                        fontFamily = SpaceMono,
                        fontSize = 9.sp,
                        color = TextMuted2
                    )
                }
            }
        }
    }
}

// Section 30.10: AI Answer Card
@Composable
private fun AiAnswerCard(
    answer: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0x0AFFFFFF))
            .drawBehind {
                drawRoundRect(
                    color = accentColor.copy(alpha = 0.3f),
                    cornerRadius = CornerRadius(20.dp.toPx()),
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }
            .padding(16.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("◐", fontSize = 14.sp, color = accentColor)
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "ECLIPSE AI",
                    fontFamily = SpaceMono,
                    fontSize = 9.sp,
                    color = accentColor,
                    letterSpacing = 2.sp
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = answer,
                fontFamily = Outfit,
                fontSize = 14.sp,
                color = TextPrimary,
                lineHeight = 22.sp
            )
        }
    }
}

// Section 30.5: Skeleton loading card with shimmer
@Composable
private fun SkeletonCard(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val shimmerX by transition.animateFloat(
        initialValue = -300f,
        targetValue = 300f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing)),
        label = "shimmerX"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0x08FFFFFF))
            .padding(14.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color(0x06FFFFFF), Color(0x14FFFFFF), Color(0x06FFFFFF)),
                            startX = shimmerX,
                            endX = shimmerX + 300f
                        )
                    )
            )
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color(0x06FFFFFF), Color(0x14FFFFFF), Color(0x06FFFFFF)),
                            startX = shimmerX,
                            endX = shimmerX + 300f
                        )
                    )
            )
            Spacer(Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color(0x06FFFFFF), Color(0x0AFFFFFF), Color(0x06FFFFFF)),
                            startX = shimmerX,
                            endX = shimmerX + 300f
                        )
                    )
            )
        }
    }
}

// Section 30.11: Constellation dot separator
@Composable
private fun ConstellationSeparator(accentColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) {
            Box(
                modifier = Modifier
                    .size(3.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f))
            )
            if (it < 2) Spacer(Modifier.width(6.dp))
        }
    }
}

// ── IMAGES TAB — standard grid ──
@Composable
private fun ImageTabResults(
    results: List<SearchResult>,
    isLoading: Boolean,
    onResultClick: (String) -> Unit
) {
    if (isLoading && results.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Searching images...", fontFamily = Outfit, fontSize = 14.sp, color = TextMuted)
        }
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(results) { result ->
            val thumb = result.thumbnail ?: result.favicon
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(EclipseSurface)
                    .clickable { onResultClick(result.url) }
            ) {
                AsyncImage(
                    model = thumb,
                    contentDescription = result.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

// ── VIDEOS / NEWS TAB — standard list ──
@Composable
private fun StandardTabResults(
    results: List<SearchResult>,
    isLoading: Boolean,
    accentColor: Color,
    onResultClick: (String) -> Unit
) {
    if (isLoading && results.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            SaturnLoadingSpinner(accentColor = accentColor)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(results) { result ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(EclipseSurface)
                    .clickable { onResultClick(result.url) }
                    .padding(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                AsyncImage(
                    model = result.favicon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp).clip(CircleShape),
                    contentScale = ContentScale.Fit
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = result.displayUrl,
                        fontFamily = SpaceMono,
                        fontSize = 9.sp,
                        color = TextMuted2
                    )
                    Text(
                        text = result.title,
                        fontFamily = Outfit,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (result.snippet.isNotBlank()) {
                        Text(
                            text = result.snippet,
                            fontFamily = Outfit,
                            fontSize = 12.sp,
                            color = TextMuted,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

// ── INLINE SEARCH BAR for re-searching ──
@Composable
private fun SearchBarInline(
    initialQuery: String,
    accentColor: Color,
    onSearch: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchText by remember { mutableStateOf(initialQuery) }
    var isFocused by remember { mutableStateOf(false) }
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

    // Update when initialQuery changes (e.g., new search)
    LaunchedEffect(initialQuery) {
        searchText = initialQuery
    }

    Box(
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(21.dp))
            .background(EclipseSurface)
            .border(
                width = 1.dp,
                color = if (isFocused) accentColor else EclipseBorder,
                shape = RoundedCornerShape(21.dp)
            )
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "🔍",
                fontSize = 14.sp
            )
            Spacer(Modifier.width(8.dp))
            BasicTextField(
                value = searchText,
                onValueChange = { searchText = it },
                textStyle = Outfit.copy(
                    fontSize = 14.sp,
                    color = TextPrimary
                ),
                cursorBrush = SolidColor(accentColor),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Search,
                    keyboardType = KeyboardType.Text
                ),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        val trimmed = searchText.trim()
                        if (trimmed.isNotBlank()) {
                            focusManager.clearFocus()
                            onSearch(trimmed)
                        }
                    }
                ),
                modifier = Modifier.weight(1f),
                decorationBox = { innerTextField ->
                    Box {
                        if (searchText.isEmpty()) {
                            Text(
                                text = "Search again...",
                                fontFamily = Outfit,
                                fontSize = 14.sp,
                                color = TextMuted
                            )
                        }
                        innerTextField()
                    }
                }
            )

            if (searchText.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(EclipseSurface)
                        .clickable {
                            searchText = ""
                            focusManager.clearFocus()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("✕", fontSize = 12.sp, color = TextMuted)
                }
            }

            Spacer(Modifier.width(4.dp))

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(accentColor.copy(alpha = 0.15f))
                    .clickable {
                        val trimmed = searchText.trim()
                        if (trimmed.isNotBlank()) {
                            focusManager.clearFocus()
                            onSearch(trimmed)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("→", fontSize = 14.sp, color = accentColor)
            }
        }
    }
}

// Section 19: Saturn loading spinner — replaces blank screen during search
@Composable
fun SaturnLoadingSpinner(
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "saturn")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing)),
        label = "rot"
    )
    val innerRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing)),
        label = "inner"
    )

    Canvas(
        modifier = modifier.size(48.dp)
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val r = size.minDimension * 0.25f

        // Planet core
        drawCircle(
            color = accentColor.copy(alpha = 0.15f),
            radius = r,
            center = Offset(cx, cy)
        )
        drawCircle(
            color = accentColor,
            radius = r * 0.5f,
            center = Offset(cx, cy)
        )

        // Rotating ring
        rotate(rotation, Offset(cx, cy)) {
            drawOval(
                color = accentColor.copy(alpha = 0.7f),
                topLeft = Offset(cx - r * 1.6f, cy - r * 0.5f),
                size = androidx.compose.ui.geometry.Size(r * 3.2f, r),
                style = Stroke(width = 2f)
            )
        }

        // Counter-rotating inner orbit dots
        rotate(innerRotation, Offset(cx, cy)) {
            drawCircle(
                color = accentColor.copy(alpha = 0.9f),
                radius = 3f,
                center = Offset(cx + r * 1.5f, cy)
            )
        }
    }
}
