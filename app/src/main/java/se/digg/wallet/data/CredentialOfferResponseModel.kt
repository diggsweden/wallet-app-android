// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.data

import com.squareup.moshi.Json
import eu.europa.ec.eudi.openid4vci.Claim
import eu.europa.ec.eudi.openid4vci.Display
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.net.URI
import java.util.Date
import java.util.Locale

data class CredentialOfferResponseModel(
    @Json(name = "credential_issuer")
    val credentialIssuer: String,
    @Json(name = "credential_configuration_ids")
    val credentialConfigurationIds: List<String>,
    @Json(name = "grants")
    val grants: Map<String, Grant>
)

data class Grant(
    @Json(name = "pre-authorized_code")
    val preAuthorizedCode: String,
    @Json(name = "authorization_server")
    val authorizationServer: String,
    @Json(name = "tx_code")
    val txCode: TxCodeInputMode
)

data class TxCodeInputMode(
    @Json(name = "input_mode")
    val inputMode: String,
    @Json(name = "length")
    val length: Int,
    @Json(name = "description")
    val description: String,
)

data class CredentialRequestModel(
    val format: String,
    val credential_configuration_id: String,
    val proofs: Proof
)

data class OldCredentialRequestModel(
    val format: String,
    val credential_configuration_id: String,
    val proof: OldProof
)

data class Proof(
    val jwt: List<String>
)

data class OldProof(
    val jwt: String,
    val proof_type: String
)

data class CredentialResponseModel(
    val credentials: List<Credential>
)

data class Credential(
    val credential: String
)

data class TokenModel(
    val accessToken: String,
    val tokenType: String,
    val expiresIn: String
)

data class GrantModel(
    val salt: String,
    val parameter: String,
    val value: String
)

data class FetchedCredential(
    val issuer: Display?,
    val sdJwt: String,
    val disclosures: Map<String, Disclosure>,
    val issuedAt: Date = Date()
)

data class Disclosure(
    val base64: String,
    val claim: Claim,
    val value: String
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
