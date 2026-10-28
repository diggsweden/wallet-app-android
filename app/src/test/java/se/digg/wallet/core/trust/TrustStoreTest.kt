// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.trust

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.JWSObject
import com.nimbusds.jose.Payload
import com.nimbusds.jose.crypto.ECDSASigner
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.gen.ECKeyGenerator
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import java.security.cert.X509Certificate
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

private const val PROVIDERS_URL = "https://gateway.test/trust/v0/lote-providers"
private const val LOTE_A_URL = "https://lote-a.test/lote"
private const val LOTE_B_URL = "https://lote-b.test/lote"

private val WRPAC = ServiceType.Access.uri
private val WRPRC = ServiceType.Registration.uri

private typealias Route = MockRequestHandleScope.() -> HttpResponseData

class TrustStoreTest {

    private val wrpacCa = TestCa.root("CN=WRPAC CA")
    private val otherWrpacCa = TestCa.root("CN=Other WRPAC CA")
    private val wrprcCa = TestCa.root("CN=WRPRC CA")
    private val signingKey = ECKeyGenerator(Curve.P_256).generate()

    private val requests = ConcurrentLinkedQueue<String>()
    private val routes = ConcurrentHashMap<String, Route>()

    private val baseHttpClient = HttpClient(
        MockEngine { request ->
            val url = request.url.toString()
            requests += url
            val route = routes[url] ?: return@MockEngine respondError(HttpStatusCode.NotFound)
            route()
        },
    ) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    private val gatewayHttpClient = baseHttpClient.config {
        defaultRequest {
            url("https://gateway.test/")
        }
    }

    private fun trustStore(applicationScope: CoroutineScope = CoroutineScope(Job())) = TrustStore(
        gatewayHttpClient = gatewayHttpClient,
        baseHttpClient = baseHttpClient,
        applicationScope = applicationScope,
    )

    private fun requestCount(url: String): Int = requests.count { it == url }

    private fun serveProviders(vararg urls: String) {
        val items = urls.mapIndexed { index, url ->
            """{"id": "provider-$index", "url": "$url"}"""
        }
        routes[PROVIDERS_URL] = {
            respond(
                content = """{"items": [${items.joinToString()}]}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
    }

    private fun serveLote(url: String, body: String, contentType: String = "application/json") {
        routes[url] = {
            respond(
                content = body,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, contentType),
            )
        }
    }

    private fun failWith(url: String, status: HttpStatusCode) {
        routes[url] = {
            respondError(status)
        }
    }

    private fun signed(payload: String): String {
        val jws = JWSObject(JWSHeader(JWSAlgorithm.ES256), Payload(payload))
        jws.sign(ECDSASigner(signingKey))
        return jws.serialize()
    }

    @Test
    fun `anchors of the requested service type are returned from a json list`() = runTest {
        serveProviders(LOTE_A_URL)
        serveLote(
            LOTE_A_URL,
            lote(
                entity(
                    service(WRPAC, wrpacCa.certificate),
                    service(WRPRC, wrprcCa.certificate),
                ),
            ),
        )

        val anchors = trustStore().getTrustAnchors(WRPAC)

        assertEquals(listOf(wrpacCa.certificate), anchors)
    }

    @Test
    fun `a jose list is decoded from its signed payload`() = runTest {
        serveProviders(LOTE_A_URL)
        serveLote(
            url = LOTE_A_URL,
            body = signed(lote(entity(service(WRPAC, wrpacCa.certificate)))),
            contentType = "application/jose",
        )

        val anchors = trustStore().getTrustAnchors(WRPAC)

        assertEquals(listOf(wrpacCa.certificate), anchors)
    }

    @Test
    fun `a jwt list is decoded from its signed payload`() = runTest {
        serveProviders(LOTE_A_URL)
        serveLote(
            url = LOTE_A_URL,
            body = signed(lote(entity(service(WRPAC, wrpacCa.certificate)))),
            contentType = "application/jwt",
        )

        val anchors = trustStore().getTrustAnchors(WRPAC)

        assertEquals(listOf(wrpacCa.certificate), anchors)
    }

    @Test
    fun `anchors of a service type are merged across lists, entities and services`() = runTest {
        val thirdWrpacCa = TestCa.root("CN=Third WRPAC CA")
        serveProviders(LOTE_A_URL, LOTE_B_URL)
        serveLote(
            LOTE_A_URL,
            lote(
                entity(service(WRPAC, wrpacCa.certificate)),
                entity(service(WRPAC, otherWrpacCa.certificate)),
            ),
        )
        serveLote(
            LOTE_B_URL,
            lote(
                entity(
                    service(WRPRC, wrprcCa.certificate),
                    service(WRPAC, thirdWrpacCa.certificate),
                ),
            ),
        )

        val anchors = trustStore().getTrustAnchors(WRPAC)

        assertEquals(
            setOf(wrpacCa.certificate, otherWrpacCa.certificate, thirdWrpacCa.certificate),
            anchors.toSet(),
        )
        assertEquals(3, anchors.size)
    }

    @Test
    fun `all certificates of a single service are returned`() = runTest {
        serveProviders(LOTE_A_URL)
        serveLote(
            LOTE_A_URL,
            lote(entity(service(WRPAC, wrpacCa.certificate, otherWrpacCa.certificate))),
        )

        val anchors = trustStore().getTrustAnchors(WRPAC)

        assertEquals(listOf(wrpacCa.certificate, otherWrpacCa.certificate), anchors)
    }

    @Test
    fun `line wrapped base64 certificates are decoded`() = runTest {
        val wrapped = Base64.getMimeEncoder().encodeToString(wrpacCa.certificate.encoded)
        assertTrue(wrapped.contains("\r\n"))
        serveProviders(LOTE_A_URL)
        serveLote(
            LOTE_A_URL,
            lote(entity(service(WRPAC, base64Certificates = listOf(wrapped)))),
        )

        val anchors = trustStore().getTrustAnchors(WRPAC)

        assertEquals(listOf(wrpacCa.certificate), anchors)
    }

    @Test
    fun `a service type missing from every list throws`() {
        serveProviders(LOTE_A_URL)
        serveLote(LOTE_A_URL, lote(entity(service(WRPRC, wrprcCa.certificate))))

        val trustStore = trustStore()

        assertThrows(IllegalStateException::class.java) {
            runBlocking {
                trustStore.getTrustAnchors(WRPAC)
            }
        }
    }

    @Test
    fun `a failing provider lookup throws`() {
        failWith(PROVIDERS_URL, HttpStatusCode.InternalServerError)

        val trustStore = trustStore()

        assertThrows(IllegalStateException::class.java) {
            runBlocking {
                trustStore.getTrustAnchors(WRPAC)
            }
        }
    }

    @Test
    fun `lists that fail to load are skipped and the rest are used`() = runTest {
        val unreachableUrl = "https://unreachable.test/lote"
        val unsupportedUrl = "https://unsupported.test/lote"
        val malformedUrl = "https://malformed.test/lote"
        serveProviders(unreachableUrl, unsupportedUrl, malformedUrl, LOTE_A_URL)
        failWith(unreachableUrl, HttpStatusCode.ServiceUnavailable)
        serveLote(
            url = unsupportedUrl,
            body = lote(entity(service(WRPAC, otherWrpacCa.certificate))),
            contentType = "text/html",
        )
        serveLote(malformedUrl, """{"LoTE": {}}""")
        serveLote(LOTE_A_URL, lote(entity(service(WRPAC, wrpacCa.certificate))))

        val anchors = trustStore().getTrustAnchors(WRPAC)

        assertEquals(listOf(wrpacCa.certificate), anchors)
        assertEquals(1, requestCount(unreachableUrl))
        assertEquals(1, requestCount(unsupportedUrl))
        assertEquals(1, requestCount(malformedUrl))
    }

    @Test
    fun `lists are fetched once and then served from the cache`() = runTest {
        serveProviders(LOTE_A_URL)
        serveLote(
            LOTE_A_URL,
            lote(entity(service(WRPAC, wrpacCa.certificate), service(WRPRC, wrprcCa.certificate))),
        )
        val trustStore = trustStore()

        trustStore.getTrustAnchors(WRPAC)
        trustStore.getTrustAnchors(WRPAC)
        val wrprcAnchors = trustStore.getTrustAnchors(WRPRC)

        assertEquals(listOf(wrprcCa.certificate), wrprcAnchors)
        assertEquals(1, requestCount(PROVIDERS_URL))
        assertEquals(1, requestCount(LOTE_A_URL))
    }

    @Test
    fun `concurrent lookups fetch the lists only once`() = runTest {
        serveProviders(LOTE_A_URL)
        serveLote(LOTE_A_URL, lote(entity(service(WRPAC, wrpacCa.certificate))))
        val trustStore = trustStore()

        val results = List(10) {
            async {
                trustStore.getTrustAnchors(WRPAC)
            }
        }.awaitAll()

        assertTrue(results.all { it == listOf(wrpacCa.certificate) })
        assertEquals(1, requestCount(PROVIDERS_URL))
        assertEquals(1, requestCount(LOTE_A_URL))
    }

    @Test
    fun `a failed provider lookup is retried on the next lookup`() = runTest {
        failWith(PROVIDERS_URL, HttpStatusCode.InternalServerError)
        serveLote(LOTE_A_URL, lote(entity(service(WRPAC, wrpacCa.certificate))))
        val trustStore = trustStore()

        runCatching {
            trustStore.getTrustAnchors(WRPAC)
        }
        serveProviders(LOTE_A_URL)
        val anchors = trustStore.getTrustAnchors(WRPAC)

        assertEquals(listOf(wrpacCa.certificate), anchors)
        assertEquals(2, requestCount(PROVIDERS_URL))
    }

    @Test
    fun `a load where every list failed is not cached`() = runTest {
        serveProviders(LOTE_A_URL)
        failWith(LOTE_A_URL, HttpStatusCode.ServiceUnavailable)
        val trustStore = trustStore()

        runCatching {
            trustStore.getTrustAnchors(WRPAC)
        }
        serveLote(LOTE_A_URL, lote(entity(service(WRPAC, wrpacCa.certificate))))
        val anchors = trustStore.getTrustAnchors(WRPAC)

        assertEquals(listOf(wrpacCa.certificate), anchors)
        assertEquals(2, requestCount(LOTE_A_URL))
    }

    @Test
    fun `a chain issued by an anchor of the service type is trusted`() = runTest {
        serveProviders(LOTE_A_URL)
        serveLote(LOTE_A_URL, lote(entity(service(WRPAC, wrpacCa.certificate))))
        val leaf = wrpacCa.issueLeaf("CN=Issuer")

        assertTrue(trustStore().isTrusted(listOf(leaf), ServiceType.Access))
    }

    @Test
    fun `a chain issued by an anchor of another service type is not trusted`() = runTest {
        serveProviders(LOTE_A_URL)
        serveLote(
            LOTE_A_URL,
            lote(entity(service(WRPAC, wrpacCa.certificate), service(WRPRC, wrprcCa.certificate))),
        )
        val leaf = wrprcCa.issueLeaf("CN=Issuer")

        assertFalse(trustStore().isTrusted(listOf(leaf), ServiceType.Access))
    }

    @Test
    fun `a chain is not trusted when the anchors cannot be loaded`() = runTest {
        failWith(PROVIDERS_URL, HttpStatusCode.InternalServerError)
        val leaf = wrpacCa.issueLeaf("CN=Issuer")

        assertFalse(trustStore().isTrusted(listOf(leaf), ServiceType.Access))
    }

    @Test
    fun `prefetching warms the cache`() = runTest {
        serveProviders(LOTE_A_URL)
        serveLote(LOTE_A_URL, lote(entity(service(WRPAC, wrpacCa.certificate))))
        val applicationJob = Job()
        val trustStore = trustStore(CoroutineScope(applicationJob))

        trustStore.prefetchTrustAnchors()
        applicationJob.children.toList().joinAll()
        val anchors = trustStore.getTrustAnchors(WRPAC)

        assertEquals(listOf(wrpacCa.certificate), anchors)
        assertEquals(1, requestCount(PROVIDERS_URL))
        assertEquals(1, requestCount(LOTE_A_URL))
    }

    @Test
    fun `a failing prefetch leaves the application scope intact and is retried later`() = runTest {
        failWith(PROVIDERS_URL, HttpStatusCode.InternalServerError)
        serveLote(LOTE_A_URL, lote(entity(service(WRPAC, wrpacCa.certificate))))
        val applicationJob = Job()
        val trustStore = trustStore(CoroutineScope(applicationJob))

        trustStore.prefetchTrustAnchors()
        applicationJob.children.toList().joinAll()
        serveProviders(LOTE_A_URL)
        val anchors = trustStore.getTrustAnchors(WRPAC)

        assertTrue(applicationJob.isActive)
        assertEquals(listOf(wrpacCa.certificate), anchors)
        assertEquals(2, requestCount(PROVIDERS_URL))
    }
}

private fun lote(vararg entities: String): String = """
{
  "LoTE": {
    "ListAndSchemeInformation": {
      "LoTEVersionIdentifier": 1,
      "LoTESequenceNumber": 1,
      "LoTEType": "http://uri.etsi.org/19602/LoTEType/EUWRPACProvidersList",
      "SchemeOperatorName": [{"lang": "en", "value": "Test Operator"}],
      "SchemeName": [{"lang": "en", "value": "Test Scheme"}],
      "SchemeTerritory": "SE",
      "ListIssueDateTime": "2026-01-01T00:00:00Z",
      "NextUpdate": "2026-07-01T00:00:00Z",
      "UnknownField": "ignored"
    },
    "TrustedEntitiesList": [${entities.joinToString()}]
  }
}
"""

private fun entity(vararg services: String): String = """
{
  "TrustedEntityInformation": {
    "TEName": [{"lang": "en", "value": "Test Entity"}],
    "TEAddress": {
      "TEPostalAddress": [{"lang": "en", "StreetAddress": "Street 1", "Country": "SE"}],
      "TEElectronicAddress": [{"lang": "en", "uriValue": "mailto:test@example.com"}]
    },
    "TEInformationURI": [{"lang": "en", "uriValue": "https://example.com"}]
  },
  "TrustedEntityServices": [${services.joinToString()}]
}
"""

private fun service(type: String, vararg certificates: X509Certificate): String = service(
    type = type,
    base64Certificates = certificates.map {
        Base64.getEncoder().encodeToString(it.encoded)
    },
)

private fun service(type: String, base64Certificates: List<String>): String {
    val certificates = base64Certificates.joinToString {
        """{"val": "${it.replace("\r\n", "\\r\\n")}"}"""
    }
    return """
    {
      "ServiceInformation": {
        "ServiceName": [{"lang": "en", "value": "Test Service"}],
        "ServiceDigitalIdentity": {
          "X509Certificates": [$certificates]
        },
        "ServiceTypeIdentifier": "$type",
        "ServiceStatus": "http://uri.etsi.org/19602/SvcStatus/granted",
        "StatusStartingTime": "2026-01-01T00:00:00Z"
      }
    }
    """
}
