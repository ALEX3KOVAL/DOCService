package ru.alex3koval.docService

import ru.alex3koval.docService.vo.DocFormat
import java.io.InputStream

/**
 * Интерфейс подсчета страниц документа
 */
interface DocPageCount {
    /**
     * Получить количество страниц в документе
     *
     * Работает с форматами: PDF, DOCX, XLS
     */
    fun countPages(format: DocFormat, content: InputStream): Result<Int>
}