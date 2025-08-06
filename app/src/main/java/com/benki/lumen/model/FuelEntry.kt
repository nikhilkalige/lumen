package com.benki.lumen.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.LocalDate
import java.util.UUID

object LocalDateSerializer : KSerializer<LocalDate> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LocalDate", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: LocalDate) = encoder.encodeString(value.toString())
    override fun deserialize(decoder: Decoder): LocalDate = LocalDate.parse(decoder.decodeString())
}

/**
 * Domain model representing a single fuel log entry.
 */
@Serializable
data class FuelEntry(
    val id: String = UUID.randomUUID().toString(),
    @Serializable(with = LocalDateSerializer::class)
    val date: LocalDate,
    val gallons: Double,
    val miles: Double,
    val cost: Double,
    // Google Sheets specific fields
    val sheetRowId: String? = null,
    val status: Status = Status.PENDING,
    val errorMessage: String? = null
) {
    @Serializable
    enum class Status { PENDING, SYNCED, ERROR }
} 