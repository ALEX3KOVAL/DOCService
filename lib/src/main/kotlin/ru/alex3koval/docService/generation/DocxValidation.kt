package ru.alex3koval.docService.generation

import org.apache.poi.xwpf.extractor.XWPFWordExtractor
import org.apache.poi.xwpf.usermodel.XWPFDocument
import ru.alex3koval.docService.DocumentServiceException

/**
 * Валидирует результат генерации. Работает просто - получает
 * полный тест документа без тэгов и ищет
 * регуляркой все вхождения вида ${fieldName}
 * @throws RuntimeException
 */
internal fun XWPFDocument.validate() {
    val pattern = "[$][{][A-Za-z0-9]+[}]".toRegex()
    val extractor = XWPFWordExtractor(this)
    val matches = pattern.findAll(extractor.text)

    val brokenList = matches.fold("") { acc, matchResult ->
        if (acc.isNotEmpty()) "$acc, ${matchResult.value}" else matchResult.value
    }

    if (brokenList.isNotEmpty()) {
        throw DocumentServiceException("При генерации шаблона остались незаполненные поля: $brokenList")
    }
}
