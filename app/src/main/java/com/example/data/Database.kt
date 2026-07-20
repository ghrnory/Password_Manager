package com.example.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface OwnerDao {
    @Query("SELECT * FROM owners")
    fun getAllOwners(): Flow<List<Owner>>

    @Query("SELECT * FROM owners WHERE id = :id")
    suspend fun getOwnerById(id: Int): Owner?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOwner(owner: Owner): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOwners(owners: List<Owner>)

    @Delete
    suspend fun deleteOwner(owner: Owner)

    @Query("DELETE FROM owners")
    suspend fun deleteAllOwners()
}

@Dao
interface ItemDao {
    @Query("SELECT * FROM items")
    fun getAllItems(): Flow<List<Item>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: Item): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<Item>)

    @Delete
    suspend fun deleteItem(item: Item)

    @Query("DELETE FROM items")
    suspend fun deleteAllItems()
}

@Database(entities = [Owner::class, Item::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun ownerDao(): OwnerDao
    abstract fun itemDao(): ItemDao
}
