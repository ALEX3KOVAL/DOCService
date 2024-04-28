package ru.alex3koval.docService.generation

import kotlinx.coroutines.runBlocking
import org.apache.pdfbox.io.MemoryUsageSetting
import org.apache.pdfbox.multipdf.PDFMergerUtility
import org.apache.poi.xwpf.usermodel.XWPFDocument
import ru.alex3koval.docService.DocumentServiceException
import ru.alex3koval.docService.GeneratedDoc
import ru.alex3koval.docService.TxtDocument
import ru.alex3koval.docService.TxtDocumentRender
import ru.alex3koval.docService.TxtRender
import ru.alex3koval.docService.WithoutTemplateDoc
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.findAnnotation

class FileGenerator(private val unoconv: Unoconv) {
    fun generateDocx(dto: GeneratedDoc): ByteArray {
        val template = dto.template
        val modifier = dto.findModifier()

        return docx(template, dto, modifier).use { it.toByteArray() }
    }

    fun generatePdf(dto: GeneratedDoc): ByteArray = generateDocx(dto)
        .let { bytes -> runBlocking { unoconv.convertDocToPdf(bytes) } }

    fun mergePdf(parts: Iterable<InputStream>): ByteArray {
        val merger = PDFMergerUtility()

        parts.forEach(merger::addSource)

        val mergedOutput = ByteArrayOutputStream()
        merger.destinationStream = mergedOutput
        merger.mergeDocuments(MemoryUsageSetting.setupMainMemoryOnly())

        return mergedOutput.toByteArray()
    }

    fun <T : WithoutTemplateDoc> generateTxt(dto: T): TxtDocument {
        return dto::class.findAnnotation<TxtRender>()
            ?.clazz
            ?.createInstance()
            ?.let { it as TxtDocumentRender<T> }
            ?.render(dto)
            ?: throw DocumentServiceException("Не удалось найти генератор для документа ${dto::class.simpleName}")
    }
}

private fun <T : GeneratedDoc> T.findModifier(): DocumentModifier<T>? {
    return this::class.findAnnotation<Modifier>()
        ?.clazz
        ?.createInstance()
        ?.let { it as DocumentModifier<T>? }
}

internal fun XWPFDocument.toByteArray(): ByteArray {
    val output = ByteArrayOutputStream()
    return write(output).let { output.toByteArray() }
}
