// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.storage.user

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import java.util.UUID
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import se.digg.wallet.data.SavedCredential

@Entity(tableName = "user")
data class User(
    @PrimaryKey val id: Int = 0,
    val uuid: UUID?,
    val accountId: String?,
    val credentials: List<SavedCredential>,
    val pid: SavedCredential?,
    val opaqueSession: OpaqueSession? = null,
)

@Serializable
data class OpaqueSession(
    val serverPublicKeyJwk: String,
    val opaqueServerId: String,
    val stateId: String,
    val opaqueContext: String,
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

    @TypeConverter
    fun savedCredentialListToString(list: List<SavedCredential>): String? =
        runCatching { Json.encodeToString(list) }.getOrNull()

    @TypeConverter
    fun stringToSavedCredentialList(credentialString: String?): List<SavedCredential> =
        credentialString?.let {
            runCatching {
                Json.decodeFromString<List<SavedCredential>>(
                    credentialString,
                )
            }.getOrNull()
        } ?: emptyList()

    @TypeConverter
    fun opaqueSessionToString(session: OpaqueSession?): String? =
        session?.let { Json.encodeToString(it) }

    @TypeConverter
    fun stringToOpaqueSession(value: String?): OpaqueSession? =
        value?.let { runCatching { Json.decodeFromString<OpaqueSession>(it) }.getOrNull() }
}
