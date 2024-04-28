package ru.alex3koval.docService

import java.io.File

/**
 * Правило наименования файла в архиве
 */
interface FileNamingStrategy<in T : Content> {
    /**
     * Сгенерировать название файла
     *
     * @param content контент, который будет содержать файл
     *
     * @return название файла
     */
    fun name(content: T): String
}

/**
 * Наименование файла без параметризации
 *
 * @property nameWithoutExtension имя файла без расширения
 */
data class ConstantNamingStrategy<in T : Content>(val nameWithoutExtension: String) : FileNamingStrategy<T> {
    override fun name(content: T): String = "$nameWithoutExtension.${content.type.value}"
}

/**
 * Наименование файла, содержащее в себе количество страниц
 *
 * @property pageCountToFileNameWithoutExtension функция, принимающая количество страниц и возвращающая наименование файла без расширения
 */
data class PageCountNamingStrategy(val pageCountToFileNameWithoutExtension: (Int) -> String) :
    FileNamingStrategy<Document> {
    override fun name(content: Document): String {
        val pageCount = content.pageCount
            ?: throw DocumentServiceException("Для названия документа требуется просчёт количества страниц")

        return buildString {
            append(pageCountToFileNameWithoutExtension.invoke(pageCount))
            append('.')
            append(content.type.name.lowercase())
        }
    }
}

/**
 * Наименование файла, полностью продублированное из файла файловой системы
 *
 * @param file файл из файловой системы
 */
data class FileBasedFileNamingStrategy(val file: File) : FileNamingStrategy<Content> {
    override fun name(content: Content): String = file.name
}
