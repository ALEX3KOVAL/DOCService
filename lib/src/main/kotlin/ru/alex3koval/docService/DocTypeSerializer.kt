package ru.alex3koval.docService

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import ru.alex3koval.docService.vo.DocType

/**
 * Сериализатор типа документов
 */
object DocTypeSerializer : KSerializer<DocType> {
    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor("DocType", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): DocType {
        return decoder.decodeString().let(DocType.Companion::invoke).getOrThrow()
    }

    override fun serialize(encoder: Encoder, value: DocType) {
        return value.value.let { encoder.encodeString(it) }
    }
}