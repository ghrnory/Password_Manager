package com.example.data

import kotlinx.coroutines.flow.Flow

class PasswordRepository(
    private val ownerDao: OwnerDao,
    private val itemDao: ItemDao
) {
    val allOwners: Flow<List<Owner>> = ownerDao.getAllOwners()
    val allItems: Flow<List<Item>> = itemDao.getAllItems()

    suspend fun insertOwner(owner: Owner): Long {
        return ownerDao.insertOwner(owner)
    }

    suspend fun deleteOwner(owner: Owner) {
        ownerDao.deleteOwner(owner)
    }

    suspend fun insertItem(item: Item): Long {
        return itemDao.insertItem(item)
    }

    suspend fun deleteItem(item: Item) {
        itemDao.deleteItem(item)
    }

    suspend fun clearAllData() {
        itemDao.deleteAllItems()
        ownerDao.deleteAllOwners()
    }

    suspend fun restoreBackup(owners: List<Owner>, items: List<Item>, merge: Boolean) {
        if (!merge) {
            clearAllData()
        }
        // Insert owners and items. Because we use REPLACE conflict strategy,
        // it automatically overwrites (gives priority to newly imported data) for items with the same ID.
        ownerDao.insertOwners(owners)
        itemDao.insertItems(items)
    }
}
