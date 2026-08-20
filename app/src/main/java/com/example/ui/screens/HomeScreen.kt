package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.IdeaMemo
import com.example.data.model.PredefinedCategories
import com.example.ui.components.IdeaCard
import com.example.ui.viewmodel.IdeaTab
import com.example.ui.viewmodel.IdeaViewModel
import com.example.ui.viewmodel.SortOption

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: IdeaViewModel,
    modifier: Modifier = Modifier
) {
    val filteredIdeas by viewModel.filteredIdeas.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedTags by viewModel.selectedTags.collectAsState()
    val activeTab by viewModel.activeTab.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()
    val isGridView by viewModel.isGridView.collectAsState()
    val allTags by viewModel.allTags.collectAsState()
    val selectedImportance by viewModel.selectedImportance.collectAsState()

    var isSearchExpanded by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }

    val pinnedIdeas = remember(filteredIdeas) { filteredIdeas.filter { it.isPinned } }
    val unpinnedIdeas = remember(filteredIdeas) { filteredIdeas.filter { !it.isPinned } }

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("home_screen"),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFF6366F1), Color(0xFFF59E0B))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lightbulb,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "아이디어노트",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    actions = {
                        // Search Button Toggle
                        IconButton(
                            onClick = {
                                isSearchExpanded = !isSearchExpanded
                                if (!isSearchExpanded) viewModel.setSearchQuery("")
                            },
                            modifier = Modifier.testTag("toggle_search_button")
                        ) {
                            Icon(
                                imageVector = if (isSearchExpanded) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = "검색"
                            )
                        }

                        // Tag Management Button
                        IconButton(
                            onClick = { viewModel.openTagManageDialog() },
                            modifier = Modifier.testTag("open_tag_manage_button")
                        ) {
                            if (selectedTags.isNotEmpty()) {
                                BadgedBox(
                                    badge = {
                                        Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                            Text("${selectedTags.size}")
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.LocalOffer, contentDescription = "태그 관리")
                                }
                            } else {
                                Icon(Icons.Default.LocalOffer, contentDescription = "태그 관리")
                            }
                        }

                        // Grid / List view toggle
                        IconButton(
                            onClick = { viewModel.toggleViewMode() },
                            modifier = Modifier.testTag("toggle_view_mode_button")
                        ) {
                            Icon(
                                imageVector = if (isGridView) Icons.Default.ViewAgenda else Icons.Default.GridView,
                                contentDescription = if (isGridView) "리스트 보기" else "그리드 보기"
                            )
                        }

                        // Sort Menu
                        Box {
                            IconButton(
                                onClick = { showSortMenu = true },
                                modifier = Modifier.testTag("sort_menu_button")
                            ) {
                                Icon(Icons.Default.Sort, contentDescription = "정렬")
                            }

                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false }
                            ) {
                                SortOption.values().forEach { option ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = option.label,
                                                fontWeight = if (sortOption == option) FontWeight.Bold else FontWeight.Normal,
                                                color = if (sortOption == option) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                        },
                                        onClick = {
                                            viewModel.setSortOption(option)
                                            showSortMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )

                // Search Bar Expandable
                AnimatedVisibility(
                    visible = isSearchExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = { Text("아이디어, 내용, 태그 검색…") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                        Icon(Icons.Default.Clear, contentDescription = "검색어 지우기")
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("search_text_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            )
                        )
                    }
                }

                // Tabs (전체, 즐겨찾기, 고정됨)
                PrimaryTabRow(
                    selectedTabIndex = activeTab.ordinal,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    divider = {}
                ) {
                    IdeaTab.values().forEach { tab ->
                        Tab(
                            selected = activeTab == tab,
                            onClick = { viewModel.setActiveTab(tab) },
                            text = {
                                Text(
                                    text = tab.label,
                                    fontWeight = if (activeTab == tab) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            modifier = Modifier.testTag("tab_${tab.name.lowercase()}")
                        )
                    }
                }

                // Category Filter Scroll Row
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(PredefinedCategories.ALL) { category ->
                        val isSelected = selectedCategory == category
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.selectCategory(category) },
                            label = { Text(category) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("category_chip_$category")
                        )
                    }
                }

                // Importance (별 1~5개) Filter Scroll Row
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item {
                        FilterChip(
                            selected = selectedImportance == null,
                            onClick = { viewModel.clearImportanceFilter() },
                            label = { Text("중요도 전체") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("importance_chip_all")
                        )
                    }

                    items(listOf(5, 4, 3, 2, 1)) { starCount ->
                        val isSelected = selectedImportance == starCount
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setImportanceFilter(starCount) },
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("★".repeat(starCount), color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("${starCount}점")
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFFEF3C7),
                                selectedLabelColor = Color(0xFF92400E)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("importance_chip_$starCount")
                        )
                    }
                }

                // Active Tag Filters Strip
                if (selectedTags.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "태그 필터:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(selectedTags.toList()) { tag ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.clickable { viewModel.toggleTagFilter(tag) }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = "#$tag",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "필터 해제",
                                            tint = Color.White,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                        TextButton(
                            onClick = { viewModel.clearTagFilters() },
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Text("초기화", fontSize = 12.sp)
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Quick Voice Record FAB
                ExtendedFloatingActionButton(
                    onClick = { viewModel.openVoiceDialog() },
                    icon = { Icon(Icons.Default.Mic, contentDescription = null) },
                    text = { Text("음성 녹음") },
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("voice_fab")
                )

                // New Idea Note FAB
                FloatingActionButton(
                    onClick = { viewModel.openNewIdeaSheet() },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = CircleShape,
                    modifier = Modifier.testTag("new_note_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "새 아이디어 작성", modifier = Modifier.size(28.dp))
                }
            }
        }
    ) { innerPadding ->
        if (filteredIdeas.isEmpty()) {
            // Empty State
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = if (searchQuery.isNotBlank() || selectedTags.isNotEmpty() || selectedCategory != "전체") {
                                "조건에 맞는 아이디어가 없습니다"
                            } else {
                                "번뜩이는 아이디어를 기록해보세요!"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = if (searchQuery.isNotBlank() || selectedTags.isNotEmpty()) {
                                "검색어나 필터를 변경해 보세요."
                            } else {
                                "하단의 마이크 버튼으로 말하거나\n새 메모 버튼을 눌러 생각을 체계화해보세요."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = { viewModel.openVoiceDialog() },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("음성으로 말하기")
                            }

                            Button(
                                onClick = { viewModel.openNewIdeaSheet() },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("새 아이디어")
                            }
                        }
                    }
                }
            }
        } else {
            // Ideas List / Grid
            val columns = if (isGridView) GridCells.Fixed(2) else GridCells.Fixed(1)

            LazyVerticalGrid(
                columns = columns,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .testTag("ideas_grid"),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Section Header: Pinned Ideas (if not already filtered to pinned only)
                if (pinnedIdeas.isNotEmpty() && activeTab != IdeaTab.PINNED) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = "📌 고정된 아이디어 (${pinnedIdeas.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    items(pinnedIdeas, key = { "pinned_${it.id}" }) { idea ->
                        IdeaCard(
                            idea = idea,
                            searchQuery = searchQuery,
                            onCardClick = { viewModel.openEditIdeaSheet(idea) },
                            onTogglePin = { viewModel.togglePin(idea) },
                            onToggleFavorite = { viewModel.toggleFavorite(idea) },
                            onImportanceChanged = { newImp -> viewModel.updateImportance(idea, newImp) },
                            onDelete = { viewModel.deleteIdea(idea) },
                            onDuplicate = { viewModel.duplicateIdea(idea) },
                            onTagClick = { tag -> viewModel.toggleTagFilter(tag) }
                        )
                    }

                    if (unpinnedIdeas.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Text(
                                    text = "💡 모든 아이디어 (${unpinnedIdeas.size})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Unpinned items (or all items if in PINNED tab)
                val regularItems = if (activeTab == IdeaTab.PINNED) pinnedIdeas else unpinnedIdeas
                items(regularItems, key = { "item_${it.id}" }) { idea ->
                    IdeaCard(
                        idea = idea,
                        searchQuery = searchQuery,
                        onCardClick = { viewModel.openEditIdeaSheet(idea) },
                        onTogglePin = { viewModel.togglePin(idea) },
                        onToggleFavorite = { viewModel.toggleFavorite(idea) },
                        onImportanceChanged = { newImp -> viewModel.updateImportance(idea, newImp) },
                        onDelete = { viewModel.deleteIdea(idea) },
                        onDuplicate = { viewModel.duplicateIdea(idea) },
                        onTagClick = { tag -> viewModel.toggleTagFilter(tag) }
                    )
                }

                // Bottom spacer for FAB
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}
