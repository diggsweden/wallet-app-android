// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.presentation

import java.time.Instant
import se.digg.wallet.core.data.model.VerifierUiModel
import se.digg.wallet.core.model.ClaimUiModel
import se.digg.wallet.core.model.ClaimValue

private fun previewCredential(id: String, name: String, vararg claims: Pair<String, String>) =
    CredentialUiModel(
        id = id,
        name = name,
        issuerName = null,
        logoUrl = null,
        claims = claims.map { (label, value) ->
            ClaimUiModel(id = label, displayName = label, value = ClaimValue.TextValue(value))
        },
    )

internal val previewRequest = PresentationUiState.PresentClaims(
    verifier = VerifierUiModel(
        name = "Nordbil Hyrbilar AB",
        country = "SE",
        descriptions = emptyMap(),
        purpose = mapOf(
            "sv" to "Identitets- och körkortskontroll vid biluthyrning. Valfritt: studentrabatt.",
        ),
    ),
    certificate = CertificateDetails(
        subject = "Nordbil Hyrbilar AB, 559123-4567",
        issuer = "Exempel Registrerings-CA",
        validFrom = Instant.parse("2026-01-15T00:00:00Z"),
        validTo = Instant.parse("2027-01-15T00:00:00Z"),
        serialNumber = "4F:2A:91:0C:7E:33",
        sha256Fingerprint = "3B:7C:A1:52:09:4D:E8:11:6F:A0:2C:9B:77:D3:41:9E:E1",
    ),
    requiredCredentials = listOf(
        previewCredential(
            "pid",
            "ID-uppgifter",
            "Förnamn" to "Elin",
            "Efternamn" to "Andersson",
            "Födelsedatum" to "18 mars 1994",
            "Personnummer" to "19940318-1234",
            "Födelseort" to "Stockholm",
            "Födelseland" to "Sverige",
            "Kön" to "Kvinna",
            "Över 18 år" to "Ja",
            "Utfärdat" to "2 januari 2026",
            "Giltigt till" to "2 januari 2031",
        ),
        previewCredential(
            "mdl",
            "Mobilt körkort",
            "Porträtt" to "Foto",
            "Behörigheter" to "B",
            "Giltigt till" to "3 maj 2033",
        ),
    ),
    optionalCredentials = listOf(
        previewCredential("address", "ID-uppgifter", "Adress" to "Storgatan 12, 113 51 Stockholm")
            .copy(isChecked = true),
        previewCredential("nationality", "ID-uppgifter", "Medborgarskap" to "Svenskt"),
        previewCredential("mdl_issued", "Mobilt körkort", "Utfärdat" to "3 maj 2013"),
        previewCredential(
            "student",
            "Studentintyg",
            "Lärosäte" to "Stockholms universitet",
            "Studietakt" to "Heltidsstudent",
            "Giltigt till" to "30 juni 2027",
        ).copy(isChecked = true),
    ),
    isEncrypted = true,
)

internal val previewDone = PresentationUiState.ShareSuccess(
    verifierName = previewRequest.verifier.name,
    sharedCredentials = previewRequest.sharedCredentials,
)
