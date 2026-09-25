package se.digg.wallet.core.crypto

import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.ECKey

@JvmInline
value class ProofKeyId(val value: String) {
    init {
        require(value.isNotBlank()) { "Missing binding key ID" }
    }
}

data class ProofKey(val id: ProofKeyId, val publicKey: ECKey) {
    init {
        require(publicKey.curve == Curve.P_256)
        require(!publicKey.isPrivate)
        require(publicKey.keyID == id.value)
        publicKey.toECPublicKey()
    }
}

@JvmInline
value class Pin(val value: String) {
    override fun toString(): String = "Pin(***)"
}
