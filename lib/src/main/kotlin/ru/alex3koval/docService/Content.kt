package ru.alex3koval.docService

import ru.alex3koval.docService.vo.DocumentType
import java.io.Closeable
import java.io.InputStream
import java.nio.charset.Charset

/**
 * Контент - это то, что можно превратить в файл
 *
 * @property stream поток байтов содержимого
 * @property type тип содержимого
 */
interface Content : Closeable {
    val stream: InputStream
    val type: DocumentType
    override fun close() = stream.close()
}

/**
 * Обычный документ, для которого
 * возможно посчитать количество страниц
 * (docx, pdf)
 *
 * @property stream поток байтов содержимого
 * @property type тип содержимого
 * @property pageCount количество страниц (опционально)
 */
data class Document internal constructor(
    override val stream: InputStream,
    override val type: DocumentType,
    val pageCount: Int? = null
) : Content

/**
 * Текстовый документ
 *
 * @property stream поток байтов содержимого
 * @property type тип содержимого
 * @property charset кодировка содержимого
 */
data class TxtDocument internal constructor(
    override val stream: InputStream,
    override val type: DocumentType,
    val charset: Charset
) : Content

/**
 * Архив, содержащий в себе другие элементы контента
 *
 * @property stream поток байтов содержимого архива
 * @property elements список, содержащий информацию о составных элементах архива
 * @property type тип содержимого
 *
 * Важно: вы не сможете прочитать поток байтов каждого элемента по-отдельности.
 * Только с помощью результирующего потока
 */
data class ZipArchive internal constructor(
    override val stream: InputStream,
    val elements: List<Content>
) : Content {
    override val type: DocumentType = DocumentType.ZIP
}
