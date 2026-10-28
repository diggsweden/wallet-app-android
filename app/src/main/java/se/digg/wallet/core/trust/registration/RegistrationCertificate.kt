// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.trust.registration

import eu.europa.ec.eudi.openid4vp.dcql.ClaimPath
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The payload of a relying party registration certificate (WRPRC), describing the verifier
 * and which credentials and claims it is registered to request.
 */
@Serializable
data class RegistrationCertificate(
    val name: String,
    val sub: String,
    val country: String,
    val iat: Long,
    val exp: Long,
    @SerialName("registry_uri")
    val registryUri: String,
    @SerialName("srv_description")
    val serviceDescription: List<LocalizedText>,
    val entitlements: List<String>,
    val purpose: List<LocalizedText>,
    val credentials: List<Credential>,
    @SerialName("service_identifier")
    val serviceIdentifier: String,
    @SerialName("intended_use_id")
    val intendedUseId: String,
    @SerialName("privacy_policy")
    val privacyPolicy: String,
    @SerialName("info_uri")
    val infoUri: String,
    @SerialName("support_uri")
    val supportUri: String,
    @SerialName("supervisory_authority")
    val supervisoryAuthority: SupervisoryAuthority,
    @SerialName("policy_id")
    val policyId: List<String>,
    @SerialName("certificate_policy")
    val certificatePolicy: String,
    val status: Status,
) {
    @Serializable
    data class Credential(val format: String, val meta: Meta, val claim: List<Claim>)

    @Serializable
    data class Claim(val path: ClaimPath)

    @Serializable
    data class Meta(
        @SerialName("vct_values")
        val vctValues: List<String>,
    )

    @Serializable
    data class LocalizedText(val lang: String, val value: String)

    @Serializable
    data class Status(
        @SerialName("status_list")
        val statusList: StatusList,
    )

    @Serializable
    data class StatusList(val uri: String, val idx: Int)

    @Serializable
    data class SupervisoryAuthority(val name: String, val country: String, val email: String)
}
