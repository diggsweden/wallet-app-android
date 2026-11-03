// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

@file:UseSerializers(OffsetDateTimeSerializer::class)

package se.digg.wallet.core.trust

import java.time.OffsetDateTime
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable
data class Trust(
    @SerialName("LoTE")
    val loTE: LoTE,
)

@Serializable
data class LoTE(
    @SerialName("ListAndSchemeInformation")
    val listAndSchemeInformation: ListAndSchemeInformation,

    @SerialName("TrustedEntitiesList")
    val trustedEntitiesList: List<TrustedEntitiesList>,
)

@Serializable
data class ListAndSchemeInformation(
    @SerialName("LoTEVersionIdentifier")
    val loTEVersionIdentifier: Long,

    @SerialName("LoTESequenceNumber")
    val loTESequenceNumber: Long,

    @SerialName("LoTEType")
    val loTEType: String,

    @SerialName("SchemeOperatorName")
    val schemeOperatorName: List<Name>,

    @SerialName("SchemeName")
    val schemeName: List<Name>,

    @SerialName("SchemeTerritory")
    val schemeTerritory: String,

    @SerialName("ListIssueDateTime")
    val listIssueDateTime: OffsetDateTime,

    @SerialName("NextUpdate")
    val nextUpdate: OffsetDateTime,
)

@Serializable
data class Name(val lang: String, val value: String)

@Serializable
data class TrustedEntitiesList(
    @SerialName("TrustedEntityInformation")
    val trustedEntityInformation: TrustedEntityInformation,

    @SerialName("TrustedEntityServices")
    val trustedEntityServices: List<TrustedEntityService>,
)

@Serializable
data class TrustedEntityInformation(
    @SerialName("TEName")
    val teName: List<Name>,

    @SerialName("TEAddress")
    val teAddress: TEAddress,

    @SerialName("TEInformationURI")
    val teInformationURI: List<Te>,
)

@Serializable
data class TEAddress(
    @SerialName("TEPostalAddress")
    val tePostalAddress: List<TEPostalAddress>,

    @SerialName("TEElectronicAddress")
    val teElectronicAddress: List<Te>,
)

@Serializable
data class Te(val lang: String, val uriValue: String)

@Serializable
data class TEPostalAddress(
    val lang: String,

    @SerialName("StreetAddress")
    val streetAddress: String,

    @SerialName("Country")
    val country: String,
)

@Serializable
data class TrustedEntityService(
    @SerialName("ServiceInformation")
    val serviceInformation: ServiceInformation,
)

@Serializable
data class ServiceInformation(
    @SerialName("ServiceName")
    val serviceName: List<Name>,

    @SerialName("ServiceDigitalIdentity")
    val serviceDigitalIdentity: ServiceDigitalIdentity,

    @SerialName("ServiceTypeIdentifier")
    val serviceTypeIdentifier: String,

    @SerialName("ServiceStatus")
    val serviceStatus: String? = null,

    @SerialName("StatusStartingTime")
    val statusStartingTime: OffsetDateTime? = null,
)

@Serializable
data class ServiceDigitalIdentity(
    @SerialName("X509Certificates")
    val x509CertificateBase64s: List<X509CertificateBase64>,
)

@Serializable
data class X509CertificateBase64(
    @SerialName("val")
    val x509CertificateVal: String,
)

object OffsetDateTimeSerializer : KSerializer<OffsetDateTime> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("OffsetDateTime", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): OffsetDateTime =
        OffsetDateTime.parse(decoder.decodeString())

    override fun serialize(encoder: Encoder, value: OffsetDateTime) {
        encoder.encodeString(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(value))
    }
}
