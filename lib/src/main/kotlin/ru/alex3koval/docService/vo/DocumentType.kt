package ru.alex3koval.docService.vo

import java.io.File

/**
 * Тип документа
 */
enum class DocumentType(val value: String) {
    DOCX("docx"),
    PDF("pdf"),
    TXT("txt"),
    ZIP("zip"),
    XLSX("xlsx"),
    XLS("xls"),
    NONE("");

    companion object {
        operator fun invoke(rawExtension: String): Result<DocumentType> = runCatching {
            entries.firstOrNull { it.name.equals(rawExtension, ignoreCase = true) }
                ?: throw RuntimeException("Неизвестное расширение: $rawExtension")
        }

        operator fun invoke(file: File): Result<DocumentType> = invoke(file.extension)
    }
}
