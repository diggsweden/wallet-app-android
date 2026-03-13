// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.storage.user

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import java.util.UUID
import kotlinx.serialization.json.Json
import se.digg.wallet.data.SavedCredential

@Entity(tableName = "user")
data class User(
    @PrimaryKey val id: Int = 0,
    val pin: String?,
    val email: String?,
    val phone: String?,
    val uuid: UUID?,
    val accountId: String?,
    val wua: String?,
    val credential: SavedCredential?,
)

class DbConverters {
    @TypeConverter
    fun uuidFromString(value: String?): UUID? =
        value?.let { runCatching { UUID.fromString(it) }.getOrNull() }

    @TypeConverter
    fun uuidToString(uuid: UUID?): String? = uuid?.toString()

    @TypeConverter
    fun credentialFromString(value: String?): SavedCredential? =
        value?.let { runCatching { Json.decodeFromString<SavedCredential>(it) }.getOrNull() }

    @TypeConverter
    fun credentialToString(credential: SavedCredential?): String? =
        credential?.let { Json.encodeToString(SavedCredential.serializer(), it) }
}
