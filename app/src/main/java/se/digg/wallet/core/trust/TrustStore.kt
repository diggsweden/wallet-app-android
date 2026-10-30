// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.trust

import com.nimbusds.jose.JWSObject
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.contentType
import java.io.ByteArrayInputStream
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import se.digg.wallet.core.common.di.ApplicationScope
import se.digg.wallet.core.di.BaseHttpClient
import se.digg.wallet.core.di.GatewayHttpClient
import se.wallet.client.gateway.client.NetworkResult
import se.wallet.client.gateway.client.TrustV0LoteProvidersClient
import timber.log.Timber

private val loteJson = Json { ignoreUnknownKeys = true }

@Singleton
class TrustStore @Inject constructor(
    @param:GatewayHttpClient gatewayHttpClient: HttpClient,
    @param:BaseHttpClient private val baseHttpClient: HttpClient,
    @param:ApplicationScope private val applicationScope: CoroutineScope,
) {
    private val loteProviderClient = TrustV0LoteProvidersClient(gatewayHttpClient)
    private val certificateFactory = CertificateFactory.getInstance("X.509")
    private val cacheMutex = Mutex()
    private var serviceTrustAnchorsCache: Map<String, List<X509Certificate>> = emptyMap()

    fun prefetchTrustAnchors() {
        applicationScope.launch {
            runCatching {
                loadTrustAnchors()
            }
        }
    }

    suspend fun getTrustAnchors(serviceType: String): List<X509Certificate> {
        val trustAnchors = loadTrustAnchors()[serviceType].orEmpty()
        check(trustAnchors.isNotEmpty()) {
            "No trust anchors found for service type $serviceType, " +
                "available service types: ${serviceTrustAnchorsCache.keys}"
        }
        return trustAnchors
    }

    private suspend fun loadTrustAnchors(): Map<String, List<X509Certificate>> =
        cacheMutex.withLock {
            if (serviceTrustAnchorsCache.isEmpty()) {
                serviceTrustAnchorsCache = fetchTrustedLists()
            }
            serviceTrustAnchorsCache
        }

    private suspend fun fetchTrustedLists(): Map<String, List<X509Certificate>> {
        val urls = when (val response = loteProviderClient.getLoteProviders()) {
            is NetworkResult.Failure -> {
                throw IllegalStateException(response.error.toString())
            }

            is NetworkResult.Success -> {
                response.data.items.map { it.url }
            }
        }
        val lotes = coroutineScope {
            urls.map { url ->
                async {
                    fetchLoteOrNull(url)
                }
            }.awaitAll()
        }.filterNotNull()
        val services = lotes.flatMap { lote ->
            lote.trustedEntitiesList.flatMap { it.trustedEntityServices }
        }
        return groupTrustAnchorsBy(services)
    }

    private suspend fun fetchLoteOrNull(url: String): LoTE? = try {
        fetchLote(url).loTE
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Timber.w(e, "Could not get trusted list: $url")
        null
    }

    private suspend fun fetchLote(url: String): Trust {
        val response = baseHttpClient.get(urlString = url)
        val subType = checkNotNull(response.contentType()?.contentSubtype)
        val loteContentType = checkNotNull(LoteContentType.findFromSubType(subType))
        val body: String = response.body()
        val json = when (loteContentType) {
            LoteContentType.Json -> body

            LoteContentType.Jose, LoteContentType.Jwt -> JWSObject.parse(body)
                .payload
                .toString()
        }
        return loteJson.decodeFromString<Trust>(json)
    }

    private fun groupTrustAnchorsBy(
        services: List<TrustedEntityService>,
    ): Map<String, List<X509Certificate>> = services.groupBy(
        keySelector = { it.serviceInformation.serviceTypeIdentifier },
        valueTransform = { it.serviceInformation.serviceDigitalIdentity.x509CertificateBase64s },
    ).mapValues { (_, certificates) ->
        certificates.flatten().mapNotNull { parseCertificateOrNull(it.x509CertificateVal) }
    }

    private fun parseCertificateOrNull(base64: String): X509Certificate? = try {
        val bytes = Base64.getMimeDecoder().decode(base64)
        certificateFactory.generateCertificate(ByteArrayInputStream(bytes)) as X509Certificate
    } catch (e: Exception) {
        Timber.w(e, "Skipping malformed trust anchor certificate")
        null
    }
}
