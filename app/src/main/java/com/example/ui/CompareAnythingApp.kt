package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.database.Comparison
import com.example.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

private fun getContenderIcon(name: String): androidx.compose.ui.graphics.vector.ImageVector {
    val lower = name.lowercase()
    return when {
        lower.contains("car") || lower.contains("tesla") || lower.contains("ev") || lower.contains("model") || lower.contains("prius") || lower.contains("vehicle") -> Icons.Default.DirectionsCar
        lower.contains("gas") || lower.contains("fuel") || lower.contains("petrol") || lower.contains("station") -> Icons.Default.LocalGasStation
        lower.contains("coffee") || lower.contains("cafe") || lower.contains("matcha") || lower.contains("tea") || lower.contains("drink") -> Icons.Default.LocalCafe
        lower.contains("phone") || lower.contains("ios") || lower.contains("android") || lower.contains("pixel") || lower.contains("samsung") || lower.contains("iphone") || lower.contains("mobile") -> Icons.Default.PhoneIphone
        lower.contains("mac") || lower.contains("windows") || lower.contains("pc") || lower.contains("laptop") || lower.contains("computer") || lower.contains("dev") -> Icons.Default.Laptop
        lower.contains("home") || lower.contains("house") || lower.contains("flat") || lower.contains("apartment") || lower.contains("suburb") -> Icons.Default.Home
        lower.contains("run") || lower.contains("sport") || lower.contains("fitness") || lower.contains("yoga") -> Icons.Default.DirectionsRun
        else -> Icons.Default.Category
    }
}

@Composable
fun DualMetricProgressBar(
    scoreA: Int,
    scoreB: Int,
    modifier: Modifier = Modifier
) {
    val total = (scoreA + scoreB).toFloat()
    val fractionA = if (total > 0f) scoreA / total else 0.5f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(GeoTrackBg)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            if (fractionA > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(fractionA)
                        .background(GeoPrimary)
                )
            }
            Box(
                modifier = Modifier
                    .width(1.5.dp)
                    .fillMaxHeight()
                    .background(GeoBackground)
            )
            if (fractionA < 1f) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f - fractionA)
                        .background(GeoSecondary)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompareAnythingApp(viewModel: ComparisonViewModel) {
    val uiState by viewModel.compareUiState.collectAsStateWithLifecycle()
    val historyList by viewModel.history.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val showOnlyFavorites by viewModel.showOnlyFavorites.collectAsStateWithLifecycle()

    val focusManager = LocalFocusManager.current

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Compare Anything",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    if (uiState is CompareUiState.Success) {
                        IconButton(onClick = { viewModel.resetToForm() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to Input"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (val state = uiState) {
                is CompareUiState.Idle -> {
                    ComparisonFormAndHistory(
                        history = historyList,
                        searchQuery = searchQuery,
                        showOnlyFavorites = showOnlyFavorites,
                        onSearchChange = { viewModel.updateSearchQuery(it) },
                        onToggleFavorites = { viewModel.toggleFavoritesFilter() },
                        onCompareTrigger = { itemA, itemB, context, criteria ->
                            focusManager.clearFocus()
                            viewModel.compare(itemA, itemB, context, criteria)
                        },
                        onSelectPastComparison = { viewModel.selectComparison(it) },
                        onToggleFavorite = { viewModel.toggleFavorite(it) },
                        onDeleteComparison = { viewModel.deleteComparison(it.id) },
                        onClearAll = { viewModel.clearAllHistory() }
                    )
                }

                is CompareUiState.Loading -> {
                    ComparingLoaderScreen(stage = state.stage)
                }

                is CompareUiState.Success -> {
                    ComparisonResultScreen(
                        comparison = state.comparison,
                        onBack = { viewModel.resetToForm() },
                        onToggleFavorite = { viewModel.toggleFavorite(state.comparison) },
                        onDelete = {
                            viewModel.deleteComparison(state.comparison.id)
                        }
                    )
                }

                is CompareUiState.Error -> {
                    ErrorScreen(
                        message = state.message,
                        onDismiss = { viewModel.resetToForm() }
                    )
                }
            }
        }
    }
}

@Composable
fun ComparisonFormAndHistory(
    history: List<Comparison>,
    searchQuery: String,
    showOnlyFavorites: Boolean,
    onSearchChange: (String) -> Unit,
    onToggleFavorites: () -> Unit,
    onCompareTrigger: (String, String, String, String) -> Unit,
    onSelectPastComparison: (Comparison) -> Unit,
    onToggleFavorite: (Comparison) -> Unit,
    onDeleteComparison: (Comparison) -> Unit,
    onClearAll: () -> Unit
) {
    var itemA by remember { mutableStateOf("") }
    var itemB by remember { mutableStateOf("") }
    var context by remember { mutableStateOf("") }
    var criteria by remember { mutableStateOf("") }
    var isHistoryExpanded by remember { mutableStateOf(false) }
    var activeTemplate by remember { mutableStateOf<ComparisonTemplate?>(null) }

    val templates = listOf(
        ComparisonTemplate(
            title = "Smartphones",
            description = "Compare design, cameras, battery life, and software.",
            icon = Icons.Default.PhoneIphone,
            defaultItemA = "iPhone 15 Pro",
            defaultItemB = "Galaxy S24 Ultra",
            defaultContext = "For daily multi-tasking, photography, and long-term durability",
            suggestedCriteria = listOf("Camera Zoom", "Battery Longevity", "Display Brightness", "Build Quality", "Software Updates", "Value for Money")
        ),
        ComparisonTemplate(
            title = "Cars / Vehicles",
            description = "Analyze efficiency, comfort, cargo, and performance.",
            icon = Icons.Default.DirectionsCar,
            defaultItemA = "Tesla Model Y",
            defaultItemB = "Toyota Prius",
            defaultContext = "Daily commuting and weekend family road trips",
            suggestedCriteria = listOf("Fuel/EV Efficiency", "Safety Ratings", "Cargo Space", "Infotainment Tech", "Annual Maintenance", "Cabin Noise")
        ),
        ComparisonTemplate(
            title = "Movies / Shows",
            description = "Compare acting, screenplay, visuals, and emotional impact.",
            icon = Icons.Default.Movie,
            defaultItemA = "Oppenheimer",
            defaultItemB = "Barbie",
            defaultContext = "Weekend evening entertainment with family",
            suggestedCriteria = listOf("Plot Originality", "Acting Depth", "Visual FX & Cinematography", "Pacing & Editing", "Soundtrack", "Rewatchability")
        ),
        ComparisonTemplate(
            title = "Laptops",
            description = "Evaluate performance, screen, keyboard, and portability.",
            icon = Icons.Default.Laptop,
            defaultItemA = "MacBook Air M3",
            defaultItemB = "Dell XPS 13",
            defaultContext = "Software engineering and remote office productivity",
            suggestedCriteria = listOf("Processing Speed", "Battery Life", "Keyboard Comfort", "Portability & Weight", "Display Resolution", "Thermal Cooling")
        ),
        ComparisonTemplate(
            title = "Universities",
            description = "Contrast tuition fees, academics, campus, and career placement.",
            icon = Icons.Default.School,
            defaultItemA = "Stanford University",
            defaultItemB = "MIT",
            defaultContext = "Undergraduate degree in Computer Science",
            suggestedCriteria = listOf("Tuition Cost", "Academic Reputation", "Campus Culture", "Location", "Career Placement", "Financial Aid")
        )
    )

    val quickComparisonIdeas = listOf(
        QuickIdea("💻 macOS", "🪟 Windows", "For development and everyday productivity"),
        QuickIdea("☕ Coffee", "🍵 Matcha", "For morning energy focus and minimal jitters"),
        QuickIdea("📱 iOS", "🤖 Android", "For an upgrade in smart ecosystem longevity"),
        QuickIdea("🏠 City Flat", "🏡 Suburb House", "For raising a young active family"),
        QuickIdea("🧘 Yoga", "🏃 Running", "For stress reduction and general fitness")
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // Hero Intro Section
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.secondary
                                    )
                                ),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Compare,
                            contentDescription = "Compare",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Unbiased AI Comparisons",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Compare any two options, items, or decisions. Powered by Gemini AI to give multi-criteria scores, pros/cons, and a final verdict.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        // Compare Input Fields
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "New Comparison",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (activeTemplate != null || itemA.isNotBlank() || itemB.isNotBlank() || context.isNotBlank() || criteria.isNotBlank()) {
                            TextButton(
                                onClick = {
                                    activeTemplate = null
                                    itemA = ""
                                    itemB = ""
                                    context = ""
                                    criteria = ""
                                }
                            ) {
                                Text("Reset Form", fontSize = 12.sp)
                            }
                        }
                    }

                    // Pre-built Comparison Templates Library
                    Text(
                        text = "Choose a Template",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = GeoSecondary
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(templates) { template ->
                            val isSelected = activeTemplate?.title == template.title
                            Card(
                                onClick = {
                                    activeTemplate = template
                                    itemA = template.defaultItemA
                                    itemB = template.defaultItemB
                                    context = template.defaultContext
                                    criteria = template.suggestedCriteria.joinToString(", ")
                                },
                                modifier = Modifier
                                    .width(160.dp)
                                    .height(90.dp),
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) GeoPrimary else GeoOutline
                                ),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) GeoSurfaceVariant else Color.White
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .background(
                                                    if (isSelected) ContenderABg else GeoSurfaceVariant,
                                                    RoundedCornerShape(8.dp)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = template.icon,
                                                contentDescription = null,
                                                tint = if (isSelected) ContenderAOn else GeoPrimary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = "Selected",
                                                tint = GeoPrimary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }

                                    Text(
                                        text = template.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = GeoOnBackground,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = itemA,
                            onValueChange = { itemA = it },
                            label = { Text("Item A") },
                            placeholder = { Text("e.g. iPhone 15 Pro") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("item_a_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .size(32.dp)
                                .background(
                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "VS",
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.secondary,
                                fontSize = 12.sp
                            )
                        }

                        OutlinedTextField(
                            value = itemB,
                            onValueChange = { itemB = it },
                            label = { Text("Item B") },
                            placeholder = { Text("e.g. Galaxy S24 Ultra") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("item_b_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    OutlinedTextField(
                        value = context,
                        onValueChange = { context = it },
                        label = { Text("Context / Purpose (Optional)") },
                        placeholder = { Text("e.g. For photo-taking on a holiday trip") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("context_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.HelpOutline, contentDescription = null)
                        }
                    )

                    OutlinedTextField(
                        value = criteria,
                        onValueChange = { criteria = it },
                        label = { Text("Specific Criteria (Optional)") },
                        placeholder = { Text("e.g. Camera zoom, Battery, Ergonomics") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("criteria_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.List, contentDescription = null)
                        }
                    )

                    if (activeTemplate != null) {
                        val currentCriteriaList = criteria.split(",")
                            .map { it.trim().lowercase() }
                            .filter { it.isNotBlank() }

                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(
                                text = "Suggested Criteria (Tap to toggle):",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = GeoPrimary
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(activeTemplate!!.suggestedCriteria) { criterion ->
                                    val isSelected = currentCriteriaList.contains(criterion.lowercase())
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            criteria = toggleCriterion(criteria, criterion)
                                        },
                                        label = {
                                            Text(text = criterion, fontSize = 11.sp)
                                        },
                                        leadingIcon = {
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Default.Add,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            }
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = ContenderABg,
                                            selectedLabelColor = ContenderAOn,
                                            selectedLeadingIconColor = ContenderAOn,
                                            containerColor = GeoSurfaceVariant,
                                            labelColor = GeoSecondary,
                                            iconColor = GeoSecondary
                                        ),
                                        border = BorderStroke(
                                            width = 1.dp,
                                            color = if (isSelected) GeoPrimary else GeoOutline
                                        ),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    Button(
                        onClick = {
                            if (itemA.isNotBlank() && itemB.isNotBlank()) {
                                onCompareTrigger(itemA, itemB, context, criteria)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("compare_button"),
                        enabled = itemA.isNotBlank() && itemB.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(imageVector = Icons.Default.BarChart, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Generate Comparison & Scores",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }

        // Quick Ideas Row
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Quick Compare Ideas",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(end = 16.dp)
                ) {
                    items(quickComparisonIdeas) { idea ->
                        Card(
                            onClick = {
                                itemA = idea.itemA
                                itemB = idea.itemB
                                context = idea.context
                                criteria = ""
                                onCompareTrigger(idea.itemA, idea.itemB, idea.context, "")
                            },
                            modifier = Modifier
                                .width(220.dp)
                                .height(110.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${idea.itemA} vs ${idea.itemB}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Bolt,
                                        contentDescription = "Quick compare",
                                        tint = Amber500,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Text(
                                    text = idea.context,
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // History list with Filters and Search
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Comparison History (${history.size})",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    if (history.isNotEmpty() && !isHistoryExpanded) {
                        TextButton(onClick = { isHistoryExpanded = true }) {
                            Text("Show All")
                        }
                    } else if (isHistoryExpanded) {
                        TextButton(onClick = { isHistoryExpanded = false }) {
                            Text("Collapse")
                        }
                    }
                }

                // Show Search / Filters if there are items in history, or if a filter is active
                if (history.isNotEmpty() || searchQuery.isNotEmpty() || showOnlyFavorites) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = onSearchChange,
                            label = { Text("Search history...") },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Search, contentDescription = null)
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { onSearchChange("") }) {
                                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear search")
                                    }
                                }
                            }
                        )

                        IconButton(
                            onClick = onToggleFavorites,
                            modifier = Modifier
                                .size(50.dp)
                                .background(
                                    if (showOnlyFavorites) Amber500.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                                    CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = if (showOnlyFavorites) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = "Filter Favorites",
                                tint = if (showOnlyFavorites) Amber500 else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // History items rendering
        if (history.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.HistoryToggleOff,
                            contentDescription = "Empty History",
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Text(
                            text = if (showOnlyFavorites || searchQuery.isNotEmpty()) "No matching comparisons found." else "No comparisons recorded yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        if (showOnlyFavorites || searchQuery.isNotEmpty()) {
                            Button(
                                onClick = {
                                    onSearchChange("")
                                    if (showOnlyFavorites) onToggleFavorites()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Reset Filters", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        } else {
            // Display either limited history or full history
            val itemsToShow = if (isHistoryExpanded) history else history.take(3)
            items(itemsToShow, key = { it.id }) { comparison ->
                HistoryItemCard(
                    comparison = comparison,
                    onClick = { onSelectPastComparison(comparison) },
                    onFavoriteToggle = { onToggleFavorite(comparison) },
                    onDelete = { onDeleteComparison(comparison) }
                )
            }

            if (history.size > 3 && !isHistoryExpanded) {
                item {
                    Button(
                        onClick = { isHistoryExpanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "View ${history.size - 3} More Comparisons",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (isHistoryExpanded) {
                item {
                    TextButton(
                        onClick = onClearAll,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        colors = ButtonDefaults.textButtonColors(contentColor = Rose500)
                    ) {
                        Icon(imageVector = Icons.Default.DeleteForever, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Clear Comparison History", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryItemCard(
    comparison: Comparison,
    onClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("history_item_${comparison.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = comparison.itemA,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Text("vs", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    Text(
                        text = comparison.itemB,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                if (comparison.context.isNotBlank()) {
                    Text(
                        text = "For: ${comparison.context}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                if (comparison.winner == "A") Emerald500.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${comparison.itemA}: ${comparison.scoreA}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (comparison.winner == "A") Emerald500 else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box(
                        modifier = Modifier
                            .background(
                                if (comparison.winner == "B") Emerald500.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${comparison.itemB}: ${comparison.scoreB}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (comparison.winner == "B") Emerald500 else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onFavoriteToggle) {
                    Icon(
                        imageVector = if (comparison.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Favorite",
                        tint = if (comparison.isFavorite) Amber500 else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete",
                        tint = Rose500.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
fun ComparingLoaderScreen(stage: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "loader")

    val scaleAngle by infiniteTransition.animateFloat(
        initialValue = -12f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scaleAngle"
    )

    val bounceTranslation by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounce"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Canvas(
            modifier = Modifier
                .size(160.dp)
                .padding(16.dp)
        ) {
            val cx = size.width / 2
            val cy = size.height * 0.7f

            // Drawing the base & support
            drawLine(
                color = Slate600,
                start = Offset(cx - 30.dp.toPx(), cy + 12.dp.toPx()),
                end = Offset(cx + 30.dp.toPx(), cy + 12.dp.toPx()),
                strokeWidth = 5.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                color = Slate600,
                start = Offset(cx, cy),
                end = Offset(cx, cy + 12.dp.toPx()),
                strokeWidth = 6.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawCircle(
                color = Slate600,
                radius = 6.dp.toPx(),
                center = Offset(cx, cy)
            )

            // Drawing the beam (tilting by scaleAngle)
            val angleRad = Math.toRadians(scaleAngle.toDouble())
            val halfBeamLen = 50.dp.toPx()
            val beamEndX = cx + halfBeamLen * cos(angleRad).toFloat()
            val beamEndY = cy - halfBeamLen * sin(angleRad).toFloat()
            val beamStartX = cx - halfBeamLen * cos(angleRad).toFloat()
            val beamStartY = cy + halfBeamLen * sin(angleRad).toFloat()

            drawLine(
                color = Amber500,
                start = Offset(beamStartX, beamStartY),
                end = Offset(beamEndX, beamEndY),
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Hanging strings & pans (draw relative to tilted beam endpoints)
            val panW = 18.dp.toPx()
            val panH = 4.dp.toPx()
            val stringL = 24.dp.toPx()

            // Left pan
            val lPanCenter = Offset(beamStartX, beamStartY + stringL)
            drawLine(
                color = Slate600,
                start = Offset(beamStartX, beamStartY),
                end = Offset(lPanCenter.x - panW / 2, lPanCenter.y),
                strokeWidth = 1.2.dp.toPx()
            )
            drawLine(
                color = Slate600,
                start = Offset(beamStartX, beamStartY),
                end = Offset(lPanCenter.x + panW / 2, lPanCenter.y),
                strokeWidth = 1.2.dp.toPx()
            )
            drawRect(
                color = Color.White,
                topLeft = Offset(lPanCenter.x - panW / 2, lPanCenter.y),
                size = androidx.compose.ui.geometry.Size(panW, panH)
            )

            // Right pan
            val rPanCenter = Offset(beamEndX, beamEndY + stringL)
            drawLine(
                color = Slate600,
                start = Offset(beamEndX, beamEndY),
                end = Offset(rPanCenter.x - panW / 2, rPanCenter.y),
                strokeWidth = 1.2.dp.toPx()
            )
            drawLine(
                color = Slate600,
                start = Offset(beamEndX, beamEndY),
                end = Offset(rPanCenter.x + panW / 2, rPanCenter.y),
                strokeWidth = 1.2.dp.toPx()
            )
            drawRect(
                color = Color.White,
                topLeft = Offset(rPanCenter.x - panW / 2, rPanCenter.y),
                size = androidx.compose.ui.geometry.Size(panW, panH)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Analyzing Balance...",
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stage,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.height(24.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))
        // Modern horizontal progress loader bar
        Box(
            modifier = Modifier
                .width(180.dp)
                .height(6.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant,
                    RoundedCornerShape(3.dp)
                )
        ) {
            val barWidthProgress by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "barWidth"
            )
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(barWidthProgress)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary
                            )
                        ),
                        RoundedCornerShape(3.dp)
                    )
            )
        }
    }
}

@Composable
fun ComparisonResultScreen(
    comparison: Comparison,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // Sticky Header / Actions bar
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("New Compare", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                            .size(40.dp)
                    ) {
                        Icon(
                            imageVector = if (comparison.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Favorite",
                            tint = if (comparison.isFavorite) Amber500 else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = {
                            onDelete()
                            onBack()
                        },
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                            .size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete",
                            tint = Rose500
                        )
                    }
                }
            }
        }

        // Versus Header Block - Side-by-Side Contender Cards
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (comparison.context.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Context: ${comparison.context}",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Contender A Card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(28.dp),
                        border = BorderStroke(1.dp, GeoOutline)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            // Verified / Winner Badge
                            if (comparison.winner == "A") {
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = "Winner",
                                    tint = GeoPrimary,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .align(Alignment.TopEnd)
                                )
                            }

                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                // Icon Circle
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(ContenderABg, RoundedCornerShape(16.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = getContenderIcon(comparison.itemA),
                                        contentDescription = null,
                                        tint = ContenderAOn,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                Text(
                                    text = comparison.itemA,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = GeoOnBackground,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                Text(
                                    text = "CONTENDER A",
                                    fontSize = 9.sp,
                                    color = GeoSecondary,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }

                    // VS Divider
                    Box(
                        modifier = Modifier
                            .width(24.dp)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .fillMaxHeight()
                                .background(GeoOutline)
                        )
                        Box(
                            modifier = Modifier
                                .background(GeoBackground)
                                .padding(vertical = 8.dp)
                        ) {
                            Text(
                                text = "vs",
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                fontWeight = FontWeight.Bold,
                                color = GeoPrimary,
                                fontSize = 16.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Serif
                            )
                        }
                    }

                    // Contender B Card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(28.dp),
                        border = BorderStroke(1.dp, GeoOutline)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            // Verified / Winner Badge
                            if (comparison.winner == "B") {
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = "Winner",
                                    tint = GeoPrimary,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .align(Alignment.TopEnd)
                                )
                            }

                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                // Icon Circle
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(ContenderBBg, RoundedCornerShape(16.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = getContenderIcon(comparison.itemB),
                                        contentDescription = null,
                                        tint = ContenderBOn,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                Text(
                                    text = comparison.itemB,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = GeoOnBackground,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                Text(
                                    text = "CONTENDER B",
                                    fontSize = 9.sp,
                                    color = GeoSecondary,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // AI Verdict Section
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "AI Verdict & Analysis",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = GeoSurfaceVariant
                    ),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "AI Verdict",
                                tint = Amber500,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = comparison.verdict,
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                color = GeoOnBackground
                            )
                        }
                    }
                }
            }
        }

        // Side-by-Side Pros & Cons Card Layouts
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Strengths & Weaknesses",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Option A Pros Cons
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, GeoOutline),
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = comparison.itemA,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = GeoPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Text("Pros", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Emerald500)
                            comparison.prosConsA.pros.forEach { pro ->
                                Row(
                                    modifier = Modifier.padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text("• ", color = Emerald500, fontWeight = FontWeight.Bold)
                                    Text(pro, fontSize = 11.sp, lineHeight = 15.sp, color = GeoOnBackground)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Cons", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Rose500)
                            comparison.prosConsA.cons.forEach { con ->
                                Row(
                                    modifier = Modifier.padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text("• ", color = Rose500, fontWeight = FontWeight.Bold)
                                    Text(con, fontSize = 11.sp, lineHeight = 15.sp, color = GeoOnBackground)
                                }
                            }
                        }
                    }

                    // Option B Pros Cons
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, GeoOutline),
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = comparison.itemB,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = GeoSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Text("Pros", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Emerald500)
                            comparison.prosConsB.pros.forEach { pro ->
                                Row(
                                    modifier = Modifier.padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text("• ", color = Emerald500, fontWeight = FontWeight.Bold)
                                    Text(pro, fontSize = 11.sp, lineHeight = 15.sp, color = GeoOnBackground)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Cons", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Rose500)
                            comparison.prosConsB.cons.forEach { con ->
                                Row(
                                    modifier = Modifier.padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text("• ", color = Rose500, fontWeight = FontWeight.Bold)
                                    Text(con, fontSize = 11.sp, lineHeight = 15.sp, color = GeoOnBackground)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Geometric Balanced Metric Comparison & Overall Verdict Breakdown Card
        if (comparison.criteriaScores.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Metric Comparison",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = GeoSurfaceVariant),
                        shape = RoundedCornerShape(32.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            Text(
                                text = "METRIC COMPARISON",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = GeoSecondary,
                                letterSpacing = 1.5.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )

                            comparison.criteriaScores.forEach { catScore ->
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = catScore.category,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = GeoPrimary
                                        )
                                        Text(
                                            text = "${catScore.scoreA}% vs ${catScore.scoreB}%",
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 12.sp,
                                            color = GeoSecondary
                                        )
                                    }

                                    DualMetricProgressBar(
                                        scoreA = catScore.scoreA,
                                        scoreB = catScore.scoreB,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    if (catScore.analysis.isNotBlank()) {
                                        Text(
                                            text = catScore.analysis,
                                            fontSize = 11.sp,
                                            lineHeight = 15.sp,
                                            color = GeoSecondary.copy(alpha = 0.8f),
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                }
                            }

                            HorizontalDivider(color = GeoOutline, thickness = 1.dp)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "OVERALL VERDICT",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GeoSecondary,
                                        letterSpacing = 0.5.sp
                                    )
                                    Text(
                                        text = when (comparison.winner) {
                                            "A" -> "${comparison.itemA} Preferred"
                                            "B" -> "${comparison.itemB} Preferred"
                                            else -> "Utility Balanced"
                                        },
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Light,
                                        color = GeoOnBackground
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    val averageScore = (comparison.scoreA + comparison.scoreB) / 20f
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = String.format("%.1f", averageScore),
                                            fontSize = 32.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = GeoPrimary
                                        )
                                        Text(
                                            text = "FINAL SCORE",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = GeoSecondary
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .border(2.dp, GeoPrimary, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Analytics,
                                            contentDescription = "Analytics",
                                            tint = GeoPrimary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScoreGauge(
    score: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            progress = { score / 100f },
            modifier = Modifier.fillMaxSize(),
            color = color,
            strokeWidth = 6.dp,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = score.toString(),
                fontWeight = FontWeight.Black,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "/100",
                fontWeight = FontWeight.Medium,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun ErrorScreen(
    message: String,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = "Error",
            tint = Rose500,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Comparison Failed",
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onDismiss,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text("Back & Try Again", fontWeight = FontWeight.Bold)
        }
    }
}

data class QuickIdea(
    val itemA: String,
    val itemB: String,
    val context: String
)

data class ComparisonTemplate(
    val title: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val defaultItemA: String,
    val defaultItemB: String,
    val defaultContext: String,
    val suggestedCriteria: List<String>
)

private fun toggleCriterion(current: String, criterion: String): String {
    val list = current.split(",")
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .toMutableList()
    
    val index = list.indexOfFirst { it.equals(criterion, ignoreCase = true) }
    if (index != -1) {
        list.removeAt(index)
    } else {
        list.add(criterion)
    }
    return list.joinToString(", ")
}
