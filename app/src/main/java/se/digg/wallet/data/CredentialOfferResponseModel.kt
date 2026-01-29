// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.data

import eu.europa.ec.eudi.openid4vci.Claim
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.net.URI
import java.util.Date
import java.util.Locale

@Serializable
data class CredentialRequestModel(
    @SerialName("credential_configuration_id")
    val credentialConfigurationId: String,
    val proofs: Proof,
    @SerialName("credential_response_encryption")
    val credentialResponseEncryption: CredentialResponseEncryptionModel? = null
)

@Serializable
data class CredentialResponseEncryptionModel(
    val jwk: JwkModel,
    val enc: String
)

@Serializable
data class Proof(
    val jwt: List<String>
)

@Serializable
data class CredentialResponseModel(
    val credentials: List<Credential>
)

@Serializable
data class Credential(
    val credential: String
)

@Serializable
data class CredentialLocal(
    val issuer: DisplayLocal?,
    val sdJwt: String,
    val disclosures: Map<String, DisclosureLocal>,
    @Serializable(with = DateAsLongSerializer::class)
    val issuedAt: Date = Date()
)

@Serializable
data class DisplayLocal(
    val name: String,
    @Serializable(with = LocaleSerializer::class)
    val locale: Locale? = null,
    val logo: Logo? = null,
    val description: String? = null,
    @Serializable(with = JavaUriSerializer::class)
    val backgroundImage: URI? = null,
) {
    @Serializable
    data class Logo(
        @Serializable(with = JavaUriSerializer::class)
        val uri: URI? = null,
        val alternativeText: String? = null,
    )
}

@Serializable
data class DisclosureLocal(
    val base64: String,
    val claim: Claim,
    val value: String
)

object LocaleSerializer : KSerializer<Locale> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("Locale", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Locale) {
        // Save as IETF BCP 47 language tag, e.g. "en-US"
        encoder.encodeString(value.toLanguageTag())
    }

    override fun deserialize(decoder: Decoder): Locale {
        val tag = decoder.decodeString()
        return Locale.forLanguageTag(tag)
    }
}

object JavaUriSerializer : KSerializer<URI> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("JavaURI", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: URI) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): URI {
        return URI.create(decoder.decodeString())
    }
}

object DateAsLongSerializer : KSerializer<Date> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("DateAsLong", PrimitiveKind.LONG)

    override fun serialize(encoder: Encoder, value: Date) {
        encoder.encodeLong(value.time)
    }

    override fun deserialize(decoder: Decoder): Date {
        return Date(decoder.decodeLong())
    }
}

data class CredentialData(val jwt: String)
