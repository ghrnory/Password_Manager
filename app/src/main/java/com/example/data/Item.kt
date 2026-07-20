package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

@Entity(tableName = "items")
data class Item(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val ownerId: Int,
    val type: String, // "email", "social", "bank", "website", "custom"
    val subType: String?, // e.g. "gmail", "facebook" etc
    val title: String,
    val username: String?,
    val password: String?,
    val url: String?,
    val notes: String?,
    
    // For "bank" type
    val bankName: String? = null,
    val cardholderName: String? = null,
    val cardNumber: String? = null,
    val expiryDate: String? = null,
    val cvv: String? = null,
    
    // For "custom" type (JSON string of list of custom fields)
    val customFieldsJson: String? = null
)

data class CustomField(
    val id: String, // unique ID
    val label: String,
    val type: String, // "text", "multiline", "number", "password", "email", "phone", "url", "date", "boolean"
    val value: String // stored as string
)

data class BackupData(
    val version: Int = 1,
    val owners: List<Owner>,
    val items: List<Item>
)

object JsonUtils {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val customFieldsType = Types.newParameterizedType(List::class.java, CustomField::class.java)
    private val customFieldsAdapter = moshi.adapter<List<CustomField>>(customFieldsType)
    private val backupAdapter = moshi.adapter(BackupData::class.java)

    fun toJson(fields: List<CustomField>): String {
        return customFieldsAdapter.toJson(fields)
    }

    fun fromJson(json: String?): List<CustomField> {
        if (json.isNullOrEmpty()) return emptyList()
        return try {
            customFieldsAdapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun backupToJson(backup: BackupData): String {
        return backupAdapter.toJson(backup)
    }

    fun backupFromJson(json: String): BackupData? {
        return try {
            backupAdapter.fromJson(json)
        } catch (e: Exception) {
            null
        }
    }
}
