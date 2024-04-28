package ru.alex3koval.docService.generation

import ru.alex3koval.docService.vo.DocFormat
import ru.alex3koval.docService.vo.DocType
import java.io.InputStream

interface DocumentGenerator {
    fun <T> get(type: DocType, data: T, format: DocFormat = DocFormat.PDF): InputStream
}