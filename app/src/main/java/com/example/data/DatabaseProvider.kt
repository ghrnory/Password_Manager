package com.example.data

import android.content.Context
import androidx.room.Room

object DatabaseProvider {
    private var instance: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase {
        return instance ?: synchronized(this) {
            val db = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "password_manager.db"
            )
            .fallbackToDestructiveMigration()
            .build()
            instance = db
            db
        }
    }
}
