package com.benki.lumen

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.google.protobuf.InvalidProtocolBufferException
import java.io.InputStream
import java.io.OutputStream

object SheetEntryListSerializer : Serializer<SheetEntryList> {
    override val defaultValue: SheetEntryList = SheetEntryList.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): SheetEntryList {
        try {
            return SheetEntryList.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", exception)
        }
    }

    override suspend fun writeTo(
        t: SheetEntryList,
        output: OutputStream
    ) = t.writeTo(output)
} 