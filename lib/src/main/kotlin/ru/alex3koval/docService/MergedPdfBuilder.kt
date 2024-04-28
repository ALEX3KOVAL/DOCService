package ru.alex3koval.docService

import ru.alex3koval.docService.vo.throwIfDoesNotExists
import java.io.File
import java.io.InputStream

/**
 * Билдер для объединённого pdf-документа
 */
interface MergedPdfBuilder {
    /**
     * Добавить pdf, сгенерированный из шаблона
     *
     * @param dto объект с данными для подстановки в шаблон документа
     */
    fun pdf(dto: GeneratedDoc)

    /**
     * Добавить pdf из внешнего источника
     *
     * @param stream поток байтов файла из внешнего источника
     */
    fun pdf(stream: InputStream)

    /**
     * Добавить pdf из внешнего источника
     *
     * @param bytes массив байтов файла из внешнего источника
     */
    fun pdf(bytes: ByteArray) = pdf(bytes.inputStream())

    /**
     * Добавить pdf из файловой системы
     *
     * @param file файл из файловой системы
     */
    fun pdf(file: File) = pdf(file.throwIfDoesNotExists().inputStream())
}
