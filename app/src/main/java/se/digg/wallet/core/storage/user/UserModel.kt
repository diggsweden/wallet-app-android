// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.storage.user

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import java.util.UUID

@Entity(tableName = "user")
data class User(
    @PrimaryKey val id: Int = 0,
    val pin: String?,
    val email: String?,
    val uuid: UUID?,
    val wua: String?
)


class DbConverters {
    @TypeConverter
    fun uuidFromString(value: String?): UUID? =
        value?.let { runCatching { UUID.fromString(it) }.getOrNull() }

    @TypeConverter fun uuidToString(uuid: UUID?): String? = uuid?.toString()
}