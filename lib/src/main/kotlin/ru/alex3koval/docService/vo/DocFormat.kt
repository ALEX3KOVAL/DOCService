package ru.alex3koval.docService.vo

import ru.alex3koval.docService.DocumentServiceException

/**
 * Формат документа
 */
enum class DocFormat(val value: String) {
    DOCX("docx"),
    XLSX("xlsx"),
    XLS("xls"),
    PDF("pdf"),
    JPEG("jpeg"),
    TXT("txt"),
    PNG("png"),
    NONE(""),
    XML("xml"),
    ZIP("zip"),
    CSV("csv");

    /** true если формат PDF */
    fun isPdf() = this == PDF

    /** true если формат XLS */
    fun isXls() = this == XLS

    /** true если формат DOCX */
    fun isDocx() = this == DOCX

    override fun toString(): String = value

    companion object {
        operator fun invoke(value: String): Result<DocFormat> = runCatching {
            val trimmedValue = value.trim()
            entries
                .firstOrNull { it.value.equals(trimmedValue, ignoreCase = true) }
                ?: throw DocumentServiceException("Отсутствует формат документа со значением: $value")
        }
    }
}