package ru.alex3koval.docService.vo

import kotlinx.serialization.Serializable
import ru.alex3koval.docService.DocTypeSerializer
import ru.alex3koval.docService.DocumentServiceException

/**
 * Тип документа
 */
@Serializable(DocTypeSerializer::class)
enum class DocType(val value: String) {
    COURT_ORDER("court_order");

    companion object {
        operator fun invoke(value: String): Result<DocType> = runCatching {
            val trimmedValue = value.trim()

            DocType.entries
                .firstOrNull { it.value.equals(trimmedValue, ignoreCase = true) }
                ?: throw DocumentServiceException("Не найден тип документа: $trimmedValue")
        }
    }
}
