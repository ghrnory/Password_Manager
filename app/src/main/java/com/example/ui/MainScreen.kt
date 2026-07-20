package com.example.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.CustomField
import com.example.data.Item
import com.example.data.JsonUtils
import com.example.data.Owner
import com.example.ui.theme.PolishPrimary

@Composable
fun getCardColor(type: String, isDark: Boolean): Color {
    return if (isDark) {
        when (type) {
            "email" -> Color(0xFF28253D)
            "social" -> Color(0xFF3B2323)
            "bank" -> Color(0xFF1D3023)
            "custom" -> Color(0xFF332B18)
            else -> Color(0xFF1B283A)
        }
    } else {
        when (type) {
            "email" -> Color(0xFFF2F0FF)   // Light Lavender
            "social" -> Color(0xFFFFF0F0)  // Light Coral
            "bank" -> Color(0xFFF0FFF4)    // Light Sage
            "custom" -> Color(0xFFFFF7E0)  // Light Amber
            else -> Color(0xFFEBF8FF)      // Light Teal-Blue
        }
    }
}

@Composable
fun getCardBorderColor(type: String, isDark: Boolean): Color {
    return if (isDark) {
        when (type) {
            "email" -> Color(0xFF464263)
            "social" -> Color(0xFF5C3C3C)
            "bank" -> Color(0xFF2F4C38)
            "custom" -> Color(0xFF4D4229)
            else -> Color(0xFF2C3E55)
        }
    } else {
        when (type) {
            "email" -> Color(0xFFE1DFFF)
            "social" -> Color(0xFFFFDADA)
            "bank" -> Color(0xFFD1E9D6)
            "custom" -> Color(0xFFFBE09C)
            else -> Color(0xFFCBEBFC)
        }
    }
}

@Composable
fun getBadgeColors(type: String, isDark: Boolean): Pair<Color, Color> { // Pair(Bg, Text)
    return if (isDark) {
        when (type) {
            "email" -> Pair(Color(0xFF383355), Color(0xFFD6D3FF))
            "social" -> Pair(Color(0xFF4F2727), Color(0xFFFFD4D4))
            "bank" -> Pair(Color(0xFF213A28), Color(0xFFCFF6D8))
            "custom" -> Pair(Color(0xFF3D331D), Color(0xFFFFEAB8))
            else -> Pair(Color(0xFF1F3247), Color(0xFFD1EBFF))
        }
    } else {
        when (type) {
            "email" -> Pair(Color(0xFFE1DFFF), Color(0xFF1B1733))
            "social" -> Pair(Color(0xFFFFDADA), Color(0xFF410002))
            "bank" -> Pair(Color(0xFFD1E9D6), Color(0xFF06210C))
            "custom" -> Pair(Color(0xFFFBE09C), Color(0xFF251A00))
            else -> Pair(Color(0xFFCBEBFC), Color(0xFF002D52))
        }
    }
}

@Composable
fun getTypeIcon(type: String): ImageVector {
    return when (type) {
        "email" -> Icons.Default.Email
        "social" -> Icons.Default.People
        "bank" -> Icons.Default.CreditCard
        "website" -> Icons.Default.Language
        else -> Icons.Default.Extension
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val owners by viewModel.owners.collectAsState()
    val items by viewModel.items.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val filterType by viewModel.filterType.collectAsState()
    val filterSubType by viewModel.filterSubType.collectAsState()
    val sortBy by viewModel.sortBy.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val editingItem by viewModel.editingItem.collectAsState()
    val addingItemType by viewModel.addingItemType.collectAsState()

    val context = LocalContext.current
    var showAddTypeMenu by remember { mutableStateOf(false) }

    // Backup & Restore Uri Launchers
    var pendingBackupJson by remember { mutableStateOf("") }
    val exportFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.openOutputStream(it)?.use { stream ->
                    stream.write(pendingBackupJson.toByteArray())
                }
                Toast.makeText(context, "Backup file exported successfully!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    var pendingImportJson by remember { mutableStateOf("") }
    var showImportChoiceDialog by remember { mutableStateOf(false) }

    val importFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { stream ->
                    val json = stream.bufferedReader().use { r -> r.readText() }
                    pendingImportJson = json
                    showImportChoiceDialog = true
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Reading backup file failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Filtered & Sorted items
    val currentItems = remember(items, searchQuery, filterType, filterSubType, sortBy, selectedTab, owners) {
        val tabIsMe = selectedTab == 0
        items.filter { item ->
            // Filter by Tab (Owner.isMe)
            val owner = owners.find { o -> o.id == item.ownerId }
            val matchesTab = if (tabIsMe) {
                owner?.isMe == true
            } else {
                owner?.isMe == false
            }

            // Filter by Search Query
            val matchesSearch = if (searchQuery.isBlank()) {
                true
            } else {
                item.title.contains(searchQuery, ignoreCase = true) ||
                        (item.username ?: "").contains(searchQuery, ignoreCase = true) ||
                        (item.notes ?: "").contains(searchQuery, ignoreCase = true)
            }

            // Level 1 filter (type)
            val matchesType = if (filterType.isNullOrEmpty()) {
                true
            } else {
                item.type.equals(filterType, ignoreCase = true)
            }

            // Level 2 filter (subtype)
            val matchesSubType = if (filterSubType.isNullOrEmpty()) {
                true
            } else {
                item.subType.equals(filterSubType, ignoreCase = true)
            }

            matchesTab && matchesSearch && matchesType && matchesSubType
        }.sortedWith { a, b ->
            if (sortBy == "Title") {
                a.title.compareTo(b.title, ignoreCase = true)
            } else {
                b.id.compareTo(a.id) // Recently added (higher ID is more recent)
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            // Adaptive Bottom Nav: Shown on Compact (Mobile) screens
            BoxWithConstraints {
                if (maxWidth < 600.dp) {
                    NavigationBar(
                        modifier = Modifier.testTag("bottom_nav"),
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        NavigationBarItem(
                            selected = selectedTab == 0,
                            onClick = { viewModel.setSelectedTab(0) },
                            icon = { Icon(Icons.Default.Person, contentDescription = "Me") },
                            label = { Text("Me") }
                        )
                        NavigationBarItem(
                            selected = selectedTab == 1,
                            onClick = { viewModel.setSelectedTab(1) },
                            icon = { Icon(Icons.Default.People, contentDescription = "Others") },
                            label = { Text("Others") }
                        )
                        NavigationBarItem(
                            selected = selectedTab == 2,
                            onClick = { viewModel.setSelectedTab(2) },
                            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                            label = { Text("Settings") }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (selectedTab != 2) {
                Box {
                    FloatingActionButton(
                        onClick = { showAddTypeMenu = true },
                        modifier = Modifier.testTag("add_item_fab"),
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Password")
                    }

                    DropdownMenu(
                        expanded = showAddTypeMenu,
                        onDismissRequest = { showAddTypeMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Email Account") },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                            onClick = {
                                showAddTypeMenu = false
                                viewModel.startAddingItem("email")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Social Media") },
                            leadingIcon = { Icon(Icons.Default.People, contentDescription = null) },
                            onClick = {
                                showAddTypeMenu = false
                                viewModel.startAddingItem("social")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Bank Login / Card") },
                            leadingIcon = { Icon(Icons.Default.CreditCard, contentDescription = null) },
                            onClick = {
                                showAddTypeMenu = false
                                viewModel.startAddingItem("bank")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Website Credential") },
                            leadingIcon = { Icon(Icons.Default.Language, contentDescription = null) },
                            onClick = {
                                showAddTypeMenu = false
                                viewModel.startAddingItem("website")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Custom / Dynamic") },
                            leadingIcon = { Icon(Icons.Default.Extension, contentDescription = null) },
                            onClick = {
                                showAddTypeMenu = false
                                viewModel.startAddingItem("custom")
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        BoxWithConstraints(modifier = Modifier.padding(innerPadding)) {
            val isDesktop = maxWidth >= 600.dp

            Row(modifier = Modifier.fillMaxSize()) {
                // Adaptive Sidebar: Shown on Expanded (Desktop) screens
                if (isDesktop) {
                    NavigationRail(
                        modifier = Modifier.testTag("side_rail"),
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "App Logo",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        NavigationRailItem(
                            selected = selectedTab == 0,
                            onClick = { viewModel.setSelectedTab(0) },
                            icon = { Icon(Icons.Default.Person, contentDescription = "Me") },
                            label = { Text("Me") }
                        )
                        NavigationRailItem(
                            selected = selectedTab == 1,
                            onClick = { viewModel.setSelectedTab(1) },
                            icon = { Icon(Icons.Default.People, contentDescription = "Others") },
                            label = { Text("Others") }
                        )
                        NavigationRailItem(
                            selected = selectedTab == 2,
                            onClick = { viewModel.setSelectedTab(2) },
                            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                            label = { Text("Settings") }
                        )
                    }
                }

                // Main Section Container
                Box(modifier = Modifier.fillMaxSize()) {
                    if (selectedTab == 2) {
                        SettingsTab(
                            viewModel = viewModel,
                            owners = owners,
                            onExportFile = {
                                pendingBackupJson = viewModel.getBackupJson()
                                exportFileLauncher.launch("password_backup.json")
                            },
                            onImportFile = {
                                importFileLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                            }
                        )
                    } else {
                        MainPasswordsTab(
                            items = currentItems,
                            owners = owners,
                            searchQuery = searchQuery,
                            filterType = filterType,
                            filterSubType = filterSubType,
                            sortBy = sortBy,
                            viewModel = viewModel,
                            isDesktop = isDesktop
                        )
                    }
                }
            }
        }
    }

    // Form Overlay for Adding/Editing Items
    if (addingItemType != null || editingItem != null) {
        val initialItem = editingItem
        val type = addingItemType ?: initialItem?.type ?: "website"

        AddEditItemDialog(
            type = type,
            initialItem = initialItem,
            owners = owners,
            onDismiss = {
                viewModel.stopAddingItem()
                viewModel.stopEditingItem()
            },
            onSave = { ownerId, subType, title, username, password, url, notes, bankName, cardholderName, cardNumber, expiryDate, cvv, customFields ->
                viewModel.saveItem(
                    ownerId = ownerId,
                    type = type,
                    subType = subType,
                    title = title,
                    username = username,
                    password = password,
                    url = url,
                    notes = notes,
                    bankName = bankName,
                    cardholderName = cardholderName,
                    cardNumber = cardNumber,
                    expiryDate = expiryDate,
                    cvv = cvv,
                    customFields = customFields
                )
            },
            onAddOwnerInline = { name, isMe ->
                viewModel.addOwner(name, isMe)
            }
        )
    }

    // Import confirmation dialog (Merge vs. Overwrite)
    if (showImportChoiceDialog) {
        AlertDialog(
            onDismissRequest = { showImportChoiceDialog = false },
            title = { Text("Import Data Options") },
            text = { Text("How would you like to restore this backup file?\n\n• Overwrite: Deletes all previous passwords and owners, replacing them with the backup.\n• Merge: Merges the backup into your existing list. Backed-up items with conflict IDs will replace local items.") },
            confirmButton = {
                Button(
                    onClick = {
                        showImportChoiceDialog = false
                        viewModel.importBackupJson(
                            json = pendingImportJson,
                            merge = true,
                            onSuccess = {
                                Toast.makeText(context, "Data merged successfully!", Toast.LENGTH_SHORT).show()
                            },
                            onError = { err ->
                                Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                            }
                        )
                    }
                ) {
                    Text("Merge (Keep Both)")
                }
            },
            dismissButton = {
                TextButton(
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        showImportChoiceDialog = false
                        viewModel.importBackupJson(
                            json = pendingImportJson,
                            merge = false,
                            onSuccess = {
                                Toast.makeText(context, "Full reset & import complete!", Toast.LENGTH_SHORT).show()
                            },
                            onError = { err ->
                                Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                            }
                        )
                    }
                ) {
                    Text("Overwrite (Reset All)")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainPasswordsTab(
    items: List<Item>,
    owners: List<Owner>,
    searchQuery: String,
    filterType: String?,
    filterSubType: String?,
    sortBy: String,
    viewModel: MainViewModel,
    isDesktop: Boolean
) {
    val context = LocalContext.current
    var showSortMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Professional Polish Styled Search Pill Bar
        TextField(
            value = searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            placeholder = { Text("Search your passwords...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Icon") },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear Search")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(28.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_bar")
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Filters and Sort Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Filter Level 1 Button
                var showTypeMenu by remember { mutableStateOf(false) }
                Box {
                    FilterChip(
                        selected = filterType != null,
                        onClick = { showTypeMenu = true },
                        label = { Text(if (filterType == null) "All Types" else filterType.replaceFirstChar { it.uppercase() }) },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) }
                    )
                    DropdownMenu(
                        expanded = showTypeMenu,
                        onDismissRequest = { showTypeMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("All Types") },
                            onClick = {
                                viewModel.setFilterType(null)
                                showTypeMenu = false
                            }
                        )
                        listOf("email", "social", "bank", "website", "custom").forEach { t ->
                            DropdownMenuItem(
                                text = { Text(t.replaceFirstChar { it.uppercase() }) },
                                onClick = {
                                    viewModel.setFilterType(t)
                                    showTypeMenu = false
                                }
                            )
                        }
                    }
                }

                // Filter Level 2 (Sub-type) - Visible only for email and social
                if (filterType == "email" || filterType == "social") {
                    val subTypes = if (filterType == "email") {
                        listOf("gmail", "outlook", "hotmail", "yahoo", "other")
                    } else {
                        listOf("facebook", "instagram", "x", "whatsapp", "telegram", "snapchat", "other")
                    }

                    var showSubTypeMenu by remember { mutableStateOf(false) }
                    Box {
                        FilterChip(
                            selected = filterSubType != null,
                            onClick = { showSubTypeMenu = true },
                            label = { Text(if (filterSubType == null) "All Sub-Types" else filterSubType.replaceFirstChar { it.uppercase() }) },
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) }
                        )
                        DropdownMenu(
                            expanded = showSubTypeMenu,
                            onDismissRequest = { showSubTypeMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("All Sub-Types") },
                                onClick = {
                                    viewModel.setFilterSubType(null)
                                    showSubTypeMenu = false
                                }
                            )
                            subTypes.forEach { st ->
                                DropdownMenuItem(
                                    text = { Text(st.replaceFirstChar { it.uppercase() }) },
                                    onClick = {
                                        viewModel.setFilterSubType(st)
                                        showSubTypeMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Sort Selector
            Box {
                IconButton(onClick = { showSortMenu = true }) {
                    Icon(Icons.Default.Sort, contentDescription = "Sort Options")
                }
                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Sort: Title (A-Z)") },
                        leadingIcon = { if (sortBy == "Title") Icon(Icons.Default.Check, contentDescription = null) },
                        onClick = {
                            viewModel.setSortBy("Title")
                            showSortMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Sort: Recently Added") },
                        leadingIcon = { if (sortBy == "RecentlyAdded") Icon(Icons.Default.Check, contentDescription = null) },
                        onClick = {
                            viewModel.setSortBy("RecentlyAdded")
                            showSortMenu = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Empty State Check
        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.VpnKey,
                        contentDescription = "Empty",
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No credentials saved yet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Click the '+' button at the bottom to add your first password offline.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else if (items.isNotEmpty() && items.filter { item ->
                val tabIsMe = viewModel.selectedTab.value == 0
                val owner = owners.find { o -> o.id == item.ownerId }
                if (tabIsMe) owner?.isMe == true else owner?.isMe == false
            }.isEmpty()) {
            // Tab is empty because no credentials are added to Me/Others specifically
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.PersonOutline,
                        contentDescription = "Empty Category",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No items for this profile tab",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        if (viewModel.selectedTab.value == 0) "Create passwords and set owner to 'Me' profile." else "Create passwords and set owner to family or other profiles.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else if (items.isNotEmpty() && items.isNotEmpty() && items.isEmpty()) {
            // Filters have zeroed out search results
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.SearchOff,
                        contentDescription = "No results",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No matching results found",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Try relaxing your search terms or filters.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            // Keep Style Grid/List layout
            if (isDesktop) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 260.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(items, key = { it.id }) { item ->
                        PasswordCard(
                            item = item,
                            owners = owners,
                            onEdit = { viewModel.startEditingItem(item) },
                            onDelete = { viewModel.deleteItem(item) }
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(items, key = { it.id }) { item ->
                        PasswordCard(
                            item = item,
                            owners = owners,
                            onEdit = { viewModel.startEditingItem(item) },
                            onDelete = { viewModel.deleteItem(item) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PasswordCard(
    item: Item,
    owners: List<Owner>,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val uriHandler = LocalUriHandler.current

    val owner = owners.find { o -> o.id == item.ownerId }
    val isDark = isSystemInDarkTheme()
    val cardColor = getCardColor(item.type, isDark)
    val borderColor = getCardBorderColor(item.type, isDark)
    val (badgeBg, badgeText) = getBadgeColors(item.type, isDark)

    var expandedCardMenu by remember { mutableStateOf(false) }
    var revealPassword by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() }
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header Row (Matches HTML: logo square on left, badges + menu button on right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // White rounded square logo container (HTML: class="w-8 h-8 bg-white rounded-lg flex items-center justify-center shadow-sm")
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            if (isDark) Color(0xFF1F1B2C) else Color.White,
                            RoundedCornerShape(8.dp)
                        )
                        .padding(2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val firstChar = item.title.firstOrNull()?.uppercaseChar()?.toString() ?: "P"
                    Text(
                        text = firstChar,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = if (isDark) badgeText else PolishPrimary
                    )
                }

                // Row of badges and drop menu button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Profile/Owner Badge (like 'Me' or 'Work')
                    if (owner != null) {
                        Box(
                            modifier = Modifier
                                .background(
                                    if (isDark) Color(0xFF2D3039) else Color(0xFFEEF1F6),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = owner.name,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFFC3C7D0) else Color(0xFF44474E)
                            )
                        }
                    }

                    // Category/Type Badge
                    Box(
                        modifier = Modifier
                            .background(badgeBg, RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (!item.subType.isNullOrEmpty()) {
                                item.subType.replaceFirstChar { it.uppercase() }
                            } else {
                                item.type.replaceFirstChar { it.uppercase() }
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeText
                        )
                    }

                    // Action dropdown trigger
                    Box {
                        IconButton(
                            onClick = { expandedCardMenu = true },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "Actions",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = expandedCardMenu,
                            onDismissRequest = { expandedCardMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit / View details") },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                onClick = {
                                    expandedCardMenu = false
                                    onEdit()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete Password", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    expandedCardMenu = false
                                    onDelete()
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Title + Subtitle section
            Column {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!item.username.isNullOrEmpty()) {
                    Text(
                        text = item.username,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Type-Specific Fields Visualizer
            when (item.type) {
                "bank" -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        if (!item.bankName.isNullOrEmpty()) {
                            Text("Bank: ${item.bankName}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                        if (!item.cardholderName.isNullOrEmpty()) {
                            Text("Cardholder: ${item.cardholderName}", fontSize = 12.sp)
                        }
                        if (!item.cardNumber.isNullOrEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Card: ${item.cardNumber}", fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(item.cardNumber))
                                        Toast.makeText(context, "Card number copied", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy Card Number", modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                        if (!item.expiryDate.isNullOrEmpty() || !item.cvv.isNullOrEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Expiry: ${item.expiryDate ?: "N/A"}", fontSize = 12.sp, modifier = Modifier.weight(1f))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("CVV: ${item.cvv ?: "N/A"}", fontSize = 12.sp)
                                    if (!item.cvv.isNullOrEmpty()) {
                                        IconButton(
                                            onClick = {
                                                clipboardManager.setText(AnnotatedString(item.cvv))
                                                Toast.makeText(context, "CVV copied", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy CVV", modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                "custom" -> {
                    val fields = remember(item.customFieldsJson) { JsonUtils.fromJson(item.customFieldsJson) }
                    if (fields.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            fields.take(3).forEach { field ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(field.label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            if (field.type == "password" && !revealPassword) "••••••••" else field.value,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Row {
                                        if (field.type == "password") {
                                            IconButton(
                                                onClick = { revealPassword = !revealPassword },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    if (revealPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                    contentDescription = "Toggle password",
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                        IconButton(
                                            onClick = {
                                                clipboardManager.setText(AnnotatedString(field.value))
                                                Toast.makeText(context, "${field.label} copied", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            }
                            if (fields.size > 3) {
                                Text("+ ${fields.size - 3} more dynamic fields", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
                else -> {
                    // Standard Fields (username, password)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        if (!item.password.isNullOrEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Password", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        if (revealPassword) item.password else "••••••••",
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Row {
                                    IconButton(
                                        onClick = { revealPassword = !revealPassword },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            if (revealPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = "Reveal Password",
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(item.password))
                                            Toast.makeText(context, "Password copied", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy Password", modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Url or Notes indicator
            if (!item.url.isNullOrEmpty() || !item.notes.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (!item.url.isNullOrEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    try {
                                        var finalUrl = item.url
                                        if (!finalUrl.startsWith("http://") && !finalUrl.startsWith("https://")) {
                                            finalUrl = "https://$finalUrl"
                                        }
                                        uriHandler.openUri(finalUrl)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Cannot open link", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .padding(4.dp)
                        ) {
                            Icon(Icons.Default.OpenInNew, contentDescription = "Open Website", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Open Website",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    if (!item.notes.isNullOrEmpty()) {
                        Icon(
                            imageVector = Icons.Default.Note,
                            contentDescription = "Has Notes",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsTab(
    viewModel: MainViewModel,
    owners: List<Owner>,
    onExportFile: () -> Unit,
    onImportFile: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var textToImportByClipboard by remember { mutableStateOf("") }
    var newOwnerName by remember { mutableStateOf("") }
    var newOwnerIsMe by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text("Settings & Backup", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        // Profiles / Owners Manager Section
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Profile Owners", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Manage multiple user accounts or profiles offline to organize your credentials.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                
                Spacer(modifier = Modifier.height(12.dp))

                // Owners Form
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newOwnerName,
                        onValueChange = { newOwnerName = it },
                        label = { Text("Profile Name") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { newOwnerIsMe = !newOwnerIsMe }
                    ) {
                        Checkbox(checked = newOwnerIsMe, onCheckedChange = { newOwnerIsMe = it })
                        Text("Is Me", fontSize = 12.sp)
                    }
                    Button(
                        onClick = {
                            if (newOwnerName.isNotBlank()) {
                                viewModel.addOwner(newOwnerName.trim(), newOwnerIsMe)
                                newOwnerName = ""
                                newOwnerIsMe = false
                                Toast.makeText(context, "Profile added", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Text("Add")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Owners list
                owners.forEach { owner ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (owner.isMe) Icons.Default.Person else Icons.Default.PersonOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(owner.name, fontWeight = FontWeight.SemiBold)
                            if (owner.isMe) {
                                Spacer(modifier = Modifier.width(6.dp))
                                SuggestionChip(
                                    onClick = { },
                                    label = { Text("Me Tab", fontSize = 10.sp) }
                                )
                            } else {
                                Spacer(modifier = Modifier.width(6.dp))
                                SuggestionChip(
                                    onClick = { },
                                    label = { Text("Others Tab", fontSize = 10.sp) }
                                )
                            }
                        }

                        // Prevent deleting the last Owner
                        if (owners.size > 1) {
                            IconButton(onClick = { viewModel.deleteOwner(owner) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Profile", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                }
            }
        }

        // Backup and Restore Section
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Backup & Export (100% Local / Offline)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("No internet required! Save your offline data securely to a file or your clipboard.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onExportFile,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export Backup File")
                    }

                    OutlinedButton(
                        onClick = {
                            val backup = viewModel.getBackupJson()
                            clipboardManager.setText(AnnotatedString(backup))
                            Toast.makeText(context, "Backup copied to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Assignment, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Copy to Clipboard")
                    }
                }
            }
        }

        // Import Section
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Restore & Import Data", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Restore passwords from a previously exported JSON backup file or clipboard string.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Button(
                    onClick = onImportFile,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Select Backup JSON File")
                }

                HorizontalDivider()

                Text("Or Paste JSON String directly:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = textToImportByClipboard,
                    onValueChange = { textToImportByClipboard = it },
                    placeholder = { Text("Paste JSON backup data here...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        enabled = textToImportByClipboard.isNotBlank(),
                        onClick = {
                            viewModel.importBackupJson(
                                json = textToImportByClipboard,
                                merge = true,
                                onSuccess = {
                                    textToImportByClipboard = ""
                                    Toast.makeText(context, "Clipboard backup merged!", Toast.LENGTH_SHORT).show()
                                },
                                onError = { err ->
                                    Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                }
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Merge Paste")
                    }

                    OutlinedButton(
                        enabled = textToImportByClipboard.isNotBlank(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        onClick = {
                            viewModel.importBackupJson(
                                json = textToImportByClipboard,
                                merge = false,
                                onSuccess = {
                                    textToImportByClipboard = ""
                                    Toast.makeText(context, "Full reset complete!", Toast.LENGTH_SHORT).show()
                                },
                                onError = { err ->
                                    Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                }
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Overwrite Paste")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditItemDialog(
    type: String,
    initialItem: Item?,
    owners: List<Owner>,
    onDismiss: () -> Unit,
    onSave: (
        ownerId: Int,
        subType: String?,
        title: String,
        username: String?,
        password: String?,
        url: String?,
        notes: String?,
        bankName: String?,
        cardholderName: String?,
        cardNumber: String?,
        expiryDate: String?,
        cvv: String?,
        customFields: List<CustomField>
    ) -> Unit,
    onAddOwnerInline: (String, Boolean) -> Unit
) {
    var title by remember { mutableStateOf(initialItem?.title ?: "") }
    var selectedOwnerId by remember { mutableStateOf(initialItem?.ownerId ?: owners.firstOrNull()?.id ?: 1) }
    var subType by remember { mutableStateOf(initialItem?.subType ?: "") }
    var username by remember { mutableStateOf(initialItem?.username ?: "") }
    var password by remember { mutableStateOf(initialItem?.password ?: "") }
    var url by remember { mutableStateOf(initialItem?.url ?: "") }
    var notes by remember { mutableStateOf(initialItem?.notes ?: "") }

    // Bank specific fields
    var bankName by remember { mutableStateOf(initialItem?.bankName ?: "") }
    var cardholderName by remember { mutableStateOf(initialItem?.cardholderName ?: "") }
    var cardNumber by remember { mutableStateOf(initialItem?.cardNumber ?: "") }
    var expiryDate by remember { mutableStateOf(initialItem?.expiryDate ?: "") }
    var cvv by remember { mutableStateOf(initialItem?.cvv ?: "") }

    // Custom fields list
    val initialFields = remember(initialItem?.customFieldsJson) {
        JsonUtils.fromJson(initialItem?.customFieldsJson)
    }
    val customFields = remember { mutableStateListOf<CustomField>().apply { addAll(initialFields) } }

    var showOwnerDropdown by remember { mutableStateOf(false) }
    var showInlineOwnerDialog by remember { mutableStateOf(false) }
    var inlineOwnerName by remember { mutableStateOf("") }
    var inlineOwnerIsMe by remember { mutableStateOf(true) }

    var revealPassword by remember { mutableStateOf(false) }

    // Pre-populate Sub-Types lists
    val subTypesList = if (type == "email") {
        listOf("gmail", "outlook", "hotmail", "yahoo", "other")
    } else {
        listOf("facebook", "instagram", "x", "whatsapp", "telegram", "snapchat", "other")
    }

    // Set default subtype if empty
    LaunchedEffect(type) {
        if (subType.isEmpty() && subTypesList.isNotEmpty()) {
            subType = subTypesList.first()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (initialItem == null) "Add ${type.replaceFirstChar { it.uppercase() }}" else "Edit Credential",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable fields form
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Title Field
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title (e.g. My Personal Mail)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("form_title_input")
                    )

                    // Owner selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            val activeOwner = owners.find { o -> o.id == selectedOwnerId } ?: owners.firstOrNull()
                            OutlinedTextField(
                                value = activeOwner?.name ?: "No Profile",
                                onValueChange = {},
                                label = { Text("Owner Profile") },
                                readOnly = true,
                                trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showOwnerDropdown = true }
                            )
                            DropdownMenu(
                                expanded = showOwnerDropdown,
                                onDismissRequest = { showOwnerDropdown = false }
                            ) {
                                owners.forEach { o ->
                                    DropdownMenuItem(
                                        text = { Text(o.name + if (o.isMe) " (Me)" else " (Others)") },
                                        onClick = {
                                            selectedOwnerId = o.id
                                            showOwnerDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = { showInlineOwnerDialog = true },
                            modifier = Modifier.offset(y = 4.dp)
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = "Add profile inline")
                        }
                    }

                    // Level 2 Sub-type selector
                    if (type == "email" || type == "social") {
                        var showSubTypeDropdown by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = subType.replaceFirstChar { it.uppercase() },
                                onValueChange = {},
                                label = { Text("Provider/Sub-Type") },
                                readOnly = true,
                                trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showSubTypeDropdown = true }
                            )
                            DropdownMenu(
                                expanded = showSubTypeDropdown,
                                onDismissRequest = { showSubTypeDropdown = false }
                            ) {
                                subTypesList.forEach { st ->
                                    DropdownMenuItem(
                                        text = { Text(st.replaceFirstChar { it.uppercase() }) },
                                        onClick = {
                                            subType = st
                                            showSubTypeDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // conditional layouts
                    if (type == "bank") {
                        // BANK FIELDS
                        OutlinedTextField(
                            value = bankName,
                            onValueChange = { bankName = it },
                            label = { Text("Bank Name *") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = cardholderName,
                            onValueChange = { cardholderName = it },
                            label = { Text("Cardholder Name *") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = cardNumber,
                            onValueChange = { cardNumber = it },
                            label = { Text("Card Number *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = expiryDate,
                                onValueChange = { expiryDate = it },
                                label = { Text("Expiry (MM/YY) *") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = cvv,
                                onValueChange = { cvv = it },
                                label = { Text("CVV *") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Required Bank Login credential components
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text("Online Banking Username *") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Online Banking Password *") },
                            singleLine = true,
                            visualTransformation = if (revealPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { revealPassword = !revealPassword }) {
                                    Icon(
                                        if (revealPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = null
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else if (type == "custom") {
                        // CUSTOM DYNAMIC FORM BUILDER
                        Text("Dynamic Custom Fields", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text("Add user-defined fields on the fly.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        customFields.forEachIndexed { index, field ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Field #${index + 1}", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                        IconButton(
                                            onClick = { customFields.removeAt(index) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete custom field", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Label input
                                        OutlinedTextField(
                                            value = field.label,
                                            onValueChange = { newLabel ->
                                                customFields[index] = field.copy(label = newLabel)
                                            },
                                            label = { Text("Field Label") },
                                            singleLine = true,
                                            modifier = Modifier.weight(1.2f)
                                        )

                                        // Type selection
                                        var showTypeDrop by remember { mutableStateOf(false) }
                                        Box(modifier = Modifier.weight(1f)) {
                                            OutlinedTextField(
                                                value = field.type.replaceFirstChar { it.uppercase() },
                                                onValueChange = {},
                                                label = { Text("Type") },
                                                readOnly = true,
                                                trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { showTypeDrop = true }
                                            )
                                            DropdownMenu(
                                                expanded = showTypeDrop,
                                                onDismissRequest = { showTypeDrop = false }
                                            ) {
                                                listOf("text", "multiline", "number", "password", "email", "phone", "url", "date", "boolean").forEach { t ->
                                                    DropdownMenuItem(
                                                        text = { Text(t.replaceFirstChar { it.uppercase() }) },
                                                        onClick = {
                                                            customFields[index] = field.copy(type = t)
                                                            showTypeDrop = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Dynamic Value Form based on Type selection
                                    if (field.type == "boolean") {
                                        val isChecked = field.value == "true"
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Value Switch: ", fontSize = 13.sp)
                                            Switch(
                                                checked = isChecked,
                                                onCheckedChange = { checked ->
                                                    customFields[index] = field.copy(value = checked.toString())
                                                }
                                            )
                                        }
                                    } else {
                                        var fieldReveal by remember { mutableStateOf(false) }
                                        OutlinedTextField(
                                            value = field.value,
                                            onValueChange = { newVal ->
                                                customFields[index] = field.copy(value = newVal)
                                            },
                                            label = { Text("Value") },
                                            singleLine = field.type != "multiline",
                                            visualTransformation = if (field.type == "password" && !fieldReveal) PasswordVisualTransformation() else VisualTransformation.None,
                                            keyboardOptions = KeyboardOptions(
                                                keyboardType = when (field.type) {
                                                    "number" -> KeyboardType.Number
                                                    "email" -> KeyboardType.Email
                                                    "phone" -> KeyboardType.Phone
                                                    "url" -> KeyboardType.Uri
                                                    else -> KeyboardType.Text
                                                }
                                            ),
                                            trailingIcon = {
                                                if (field.type == "password") {
                                                    IconButton(onClick = { fieldReveal = !fieldReveal }) {
                                                        Icon(if (fieldReveal) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null)
                                                    }
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = {
                                customFields.add(
                                    CustomField(
                                        id = System.currentTimeMillis().toString(),
                                        label = "Field Label",
                                        type = "text",
                                        value = ""
                                    )
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.AddCircleOutline, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add Dynamic Field")
                        }
                    } else {
                        // WEBSITE, EMAIL, SOCIAL standard credential fields
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text("Username / Account Identifier") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password") },
                            singleLine = true,
                            visualTransformation = if (revealPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { revealPassword = !revealPassword }) {
                                    Icon(
                                        if (revealPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = null
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Optional URL launcher field
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text("Website Link URL (Optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Description Note Area
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes / Descriptions") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Footer Save buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (title.isBlank()) {
                                title = "Untitled Password"
                            }
                            onSave(
                                selectedOwnerId,
                                if (type == "email" || type == "social") subType else null,
                                title.trim(),
                                username.trim(),
                                password,
                                url.trim(),
                                notes.trim(),
                                if (type == "bank") bankName.trim() else null,
                                if (type == "bank") cardholderName.trim() else null,
                                if (type == "bank") cardNumber.trim() else null,
                                if (type == "bank") expiryDate.trim() else null,
                                if (type == "bank") cvv.trim() else null,
                                if (type == "custom") customFields.toList() else emptyList()
                            )
                        },
                        modifier = Modifier.testTag("save_item_button")
                    ) {
                        Text("Save Offline")
                    }
                }
            }
        }
    }

    // Inline profiles builder
    if (showInlineOwnerDialog) {
        AlertDialog(
            onDismissRequest = { showInlineOwnerDialog = false },
            title = { Text("Add Profile Owner") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Create a new local profile directly.", fontSize = 12.sp)
                    OutlinedTextField(
                        value = inlineOwnerName,
                        onValueChange = { inlineOwnerName = it },
                        label = { Text("Profile Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { inlineOwnerIsMe = !inlineOwnerIsMe }
                    ) {
                        Checkbox(checked = inlineOwnerIsMe, onCheckedChange = { inlineOwnerIsMe = it })
                        Text("Add to 'Me' profile (otherwise 'Others' profile)")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (inlineOwnerName.isNotBlank()) {
                            onAddOwnerInline(inlineOwnerName.trim(), inlineOwnerIsMe)
                            inlineOwnerName = ""
                            showInlineOwnerDialog = false
                        }
                    }
                ) {
                    Text("Add Profile")
                }
            },
            dismissButton = {
                TextButton(onClick = { showInlineOwnerDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
