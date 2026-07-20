package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val database = DatabaseProvider.getDatabase(application)
    private val repository = PasswordRepository(database.ownerDao(), database.itemDao())

    // UI state
    val owners: StateFlow<List<Owner>> = repository.allOwners
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val items: StateFlow<List<Item>> = repository.allItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Search and filters
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _filterType = MutableStateFlow<String?>(null) // Level 1 (email, social, bank, website, custom)
    val filterType = _filterType.asStateFlow()

    private val _filterSubType = MutableStateFlow<String?>(null) // Level 2 (gmail, outlook... or facebook, instagram...)
    val filterSubType = _filterSubType.asStateFlow()

    private val _sortBy = MutableStateFlow("Title") // "Title" or "RecentlyAdded"
    val sortBy = _sortBy.asStateFlow()

    private val _selectedTab = MutableStateFlow(0) // 0 = Me, 1 = Others, 2 = Settings
    val selectedTab = _selectedTab.asStateFlow()

    // Add / Edit state
    private val _editingItem = MutableStateFlow<Item?>(null)
    val editingItem = _editingItem.asStateFlow()

    private val _addingItemType = MutableStateFlow<String?>(null) // "email", "social", "bank", "website", "custom"
    val addingItemType = _addingItemType.asStateFlow()

    init {
        // Initialize default owners and sample items if empty
        viewModelScope.launch {
            owners.first { true } // Wait for flow initialization
            if (owners.value.isEmpty()) {
                val meId = repository.insertOwner(Owner(name = "My Account", isMe = true)).toInt()
                val otherId = repository.insertOwner(Owner(name = "Others / Family", isMe = false)).toInt()
                
                // Let's add standard sample items to guide the user on first launch
                repository.insertItem(
                    Item(
                        ownerId = meId,
                        type = "email",
                        subType = "gmail",
                        title = "Personal Google Mail",
                        username = "alex.doe@gmail.com",
                        password = "secureP@ssword123",
                        url = "https://mail.google.com",
                        notes = "Primary email account used for personal and recovery services."
                    )
                )
                repository.insertItem(
                    Item(
                        ownerId = meId,
                        type = "social",
                        subType = "instagram",
                        title = "Instagram Profile",
                        username = "alex_doe_creative",
                        password = "instaSecureKey987!",
                        url = "https://instagram.com",
                        notes = "Social channel for design projects."
                    )
                )
                repository.insertItem(
                    Item(
                        ownerId = otherId,
                        type = "bank",
                        subType = null,
                        title = "Family Savings Card",
                        username = "alex_doe_savings",
                        password = "cardPIN_8890",
                        url = "https://www.chase.com",
                        bankName = "Chase Bank",
                        cardholderName = "Alex Doe",
                        cardNumber = "4111 2222 3333 4444",
                        expiryDate = "12/28",
                        cvv = "123",
                        notes = "Primary card for mutual home bills and online shopping."
                    )
                )
            }
        }
    }

    // Actions
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilterType(type: String?) {
        _filterType.value = type
        _filterSubType.value = null // reset subtype on parent type change
    }

    fun setFilterSubType(subType: String?) {
        _filterSubType.value = subType
    }

    fun setSortBy(sort: String) {
        _sortBy.value = sort
    }

    fun setSelectedTab(tab: Int) {
        _selectedTab.value = tab
    }

    fun startEditingItem(item: Item) {
        _editingItem.value = item
        _addingItemType.value = null
    }

    fun stopEditingItem() {
        _editingItem.value = null
    }

    fun startAddingItem(type: String) {
        _addingItemType.value = type
        _editingItem.value = null
    }

    fun stopAddingItem() {
        _addingItemType.value = null
    }

    fun saveItem(
        ownerId: Int,
        type: String,
        subType: String?,
        title: String,
        username: String?,
        password: String?,
        url: String?,
        notes: String?,
        bankName: String? = null,
        cardholderName: String? = null,
        cardNumber: String? = null,
        expiryDate: String? = null,
        cvv: String? = null,
        customFields: List<CustomField> = emptyList()
    ) {
        viewModelScope.launch {
            val customJson = if (type == "custom") JsonUtils.toJson(customFields) else null
            val itemToSave = Item(
                id = _editingItem.value?.id ?: 0, // id is auto-generated if 0
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
                customFieldsJson = customJson
            )
            repository.insertItem(itemToSave)
            stopEditingItem()
            stopAddingItem()
        }
    }

    fun deleteItem(item: Item) {
        viewModelScope.launch {
            repository.deleteItem(item)
        }
    }

    // Owner management
    fun addOwner(name: String, isMe: Boolean) {
        viewModelScope.launch {
            if (name.isNotBlank()) {
                repository.insertOwner(Owner(name = name, isMe = isMe))
            }
        }
    }

    fun deleteOwner(owner: Owner) {
        viewModelScope.launch {
            repository.deleteOwner(owner)
        }
    }

    // Export/Import backup operations
    fun getBackupJson(): String {
        val currentOwners = owners.value
        val currentItems = items.value
        val backup = BackupData(owners = currentOwners, items = currentItems)
        return JsonUtils.backupToJson(backup)
    }

    fun importBackupJson(json: String, merge: Boolean, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val backup = JsonUtils.backupFromJson(json)
            if (backup == null) {
                onError("Invalid backup file format.")
                return@launch
            }
            try {
                repository.restoreBackup(backup.owners, backup.items, merge)
                onSuccess()
            } catch (e: Exception) {
                onError("Error importing data: ${e.message}")
            }
        }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return MainViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
