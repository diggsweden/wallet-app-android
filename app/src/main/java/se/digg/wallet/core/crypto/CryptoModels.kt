package se.digg.wallet.core.crypto

import com.nimbusds.jose.EncryptionMethod
import com.nimbusds.jose.jwk.JWK
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

@Serializable
data class DefaultJwtClaims(
    val iat: Int,
    val nbf: Int,
    val exp: Int,
)

@Serializable
data class JwtClaims<T>(
    val defaults: DefaultJwtClaims,
    val payload: T,
)

data class CryptoSpec(
    val jwk: JWK,
    val encryptionMethod: EncryptionMethod
)

class JwtClaimsSerializer<T>(
    private val payloadSerializer: KSerializer<T>,
) : KSerializer<JwtClaims<T>> {

    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("JwtClaims")

    override fun serialize(
        encoder: Encoder,
        value: JwtClaims<T>,
    ) {
        require(encoder is JsonEncoder)

        val defaultsJson = encoder.json.encodeToJsonElement(
            DefaultJwtClaims.serializer(),
            value.defaults
        ).jsonObject

        val payloadJson = encoder.json.encodeToJsonElement(
            payloadSerializer,
            value.payload
        ).jsonObject

        val merged = JsonObject(defaultsJson + payloadJson)
        encoder.encodeJsonElement(merged)
    }

    override fun deserialize(
        decoder: Decoder,
    ): JwtClaims<T> {
        require(decoder is JsonDecoder)

        val obj = decoder.decodeJsonElement().jsonObject

        val defaults = decoder.json.decodeFromJsonElement(
            DefaultJwtClaims.serializer(),
            obj
        )

        val payload = decoder.json.decodeFromJsonElement(
            payloadSerializer,
            obj
        )

        return JwtClaims(
            defaults = defaults,
            payload = payload
        )
    }
}

fun <T> jwtClaimsSerializer(
    payloadSerializer: KSerializer<T>
): KSerializer<JwtClaims<T>> =
    JwtClaimsSerializer(payloadSerializer)