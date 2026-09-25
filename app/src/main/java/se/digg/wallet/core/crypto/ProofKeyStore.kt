package se.digg.wallet.core.crypto

interface ProofKeyStore {
    suspend fun createKey(): ProofKey
    suspend fun deleteKey(keyId: ProofKeyId)
    suspend fun authenticate()
}
