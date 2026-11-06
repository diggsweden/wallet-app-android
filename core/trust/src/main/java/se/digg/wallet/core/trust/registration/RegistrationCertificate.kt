// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.trust.registration

import eu.europa.ec.eudi.openid4vp.dcql.ClaimPath
import java.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RegistrationCertificate(
    val name: String? = null,
    val sub: String,
    val country: String? = null,
    val iat: Long? = null,
    val exp: Long? = null,
    @SerialName("registry_uri")
    val registryUri: String? = null,
    @SerialName("srv_description")
    val serviceDescription: List<LocalizedText>? = null,
    val entitlements: List<String>? = null,
    val purpose: List<LocalizedText>? = null,
    val credentials: List<Credential>? = null,
    @SerialName("service_identifier")
    val serviceIdentifier: String? = null,
    @SerialName("intended_use_id")
    val intendedUseId: String? = null,
    @SerialName("privacy_policy")
    val privacyPolicy: String? = null,
    @SerialName("info_uri")
    val infoUri: String? = null,
    @SerialName("support_uri")
    val supportUri: String? = null,
    @SerialName("supervisory_authority")
    val supervisoryAuthority: SupervisoryAuthority? = null,
    @SerialName("policy_id")
    val policyId: List<String>? = null,
    @SerialName("certificate_policy")
    val certificatePolicy: String? = null,
    val status: Status? = null,
) {
    fun isExpired(at: Instant): Boolean = exp != null && !at.isBefore(Instant.ofEpochSecond(exp))

    @Serializable
    data class Credential(val format: String, val meta: Meta, val claim: List<Claim>? = null)

    @Serializable
    data class Claim(val path: ClaimPath)

    @Serializable
    data class Meta(
        @SerialName("vct_values")
        val vctValues: List<String>? = null,
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
    data class SupervisoryAuthority(
        val name: String? = null,
        val country: String? = null,
        val email: String? = null,
    )
}
