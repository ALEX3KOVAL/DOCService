package ru.alex3koval.docService

import kotlinx.coroutines.runBlocking
import org.apache.pdfbox.multipdf.LayerUtility
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.util.Matrix
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.xwpf.usermodel.XWPFDocument
import ru.alex3koval.docService.dto.ActOnAbsenceOfDocumentDTO
import ru.alex3koval.docService.generation.DocumentGenerator
import ru.alex3koval.docService.generation.FileGenerator
import ru.alex3koval.docService.generation.Unoconv
import ru.alex3koval.docService.vo.DocFormat
import ru.alex3koval.docService.vo.DocType
import ru.alex3koval.docService.vo.DocumentType
import ru.alex3koval.docService.vo.inputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.charset.Charset
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.math.min

/**
 * Сервис для генерации документов из шаблонов, сборки архива из документов и склеивания документов
 */
class DocumentService(
    private val unoconv: Unoconv,
    private val fileGenerator: FileGenerator
) : DocumentGenerator, DocPageCount {
    /**
     * Сгенерировать DOCX-документ
     *
     * @param dto объект с данными для подстановки в шаблон документа
     * @param pageCountCalculation необходимость просчёта количества страниц в документе
     *
     * @return сгенерированный документ
     */
    fun docx(dto: GeneratedDoc, pageCountCalculation: Boolean = false): Result<Document> = runBlocking {
        runCatching {
            val bytes = fileGenerator.generateDocx(dto)
            val pageCount =
                if (pageCountCalculation) PDDocument.load(unoconv.convertDocToPdf(bytes)).numberOfPages else null

            Document(
                stream = ByteArrayInputStream(bytes),
                type = DocumentType.DOCX,
                pageCount = pageCount
            )
        }
    }

    suspend fun convertToPdf(doc: ByteArray) = unoconv.convertDocToPdf(doc)

    /**
     * Ориентация
     */
    enum class Orientation(val value: String) {
        /**
         * Альбомная
         */
        LANDSCAPE("landscape"),

        /**
         * Книжная
         */
        PORTRAIT("portrait");

        fun isLandscape(): Boolean = equals(LANDSCAPE)
    }

    /**
     * Преобразовывает документ к формату
     * 4 страницы на 1 листе (Если ориентация книжная)
     * 2 страницы на 1 листе (Если ориентация альбомная)
     */
    fun combine(stream: InputStream, orientation: Orientation = Orientation.PORTRAIT): Document {
        val document = PDDocument.load(stream)
        val countOfPages = if (orientation.isLandscape()) 2 else 4
        val outPdf = PDDocument()
        val chunkRectangle: MutableList<List<PDPage>> = mutableListOf()

        /* Разбиваем массив страниц на группы по 4 штуки */
        generateSequence(0..<countOfPages) {
            if (it.last >= document.pages.count - 1) null
            else it.last + 1..min(it.last + countOfPages, document.pages.count - 1)
        }.forEach { range ->
            val neededRange =
                if ((document.pages.count - 1) <= range.last) range.first..<document.pages.count
                else range
            chunkRectangle.add(document.pages.toList().slice(neededRange))
        }

        /* проходим по группам */
        chunkRectangle.mapIndexed { indexMap, groupPages ->
            val rectangle = if (orientation.isLandscape()) PDRectangle(
                PDRectangle.A4.height,
                PDRectangle.A4.width
            ) else PDRectangle.A4

            /* добавление страницы */
            val npage = PDPage(rectangle)
            outPdf.addPage(npage)
            val layerUtility = LayerUtility(outPdf)
            val contents = PDPageContentStream(document, npage)

            /* добавляем контент на новую страницу */
            List(groupPages.size) { index ->
                val form = layerUtility.importPageAsForm(document, index + indexMap * countOfPages)

                contents.saveGraphicsState()

                /**
                 * a - изменение по ширине
                 * d - изменение по высоте
                 * e - отступ слева
                 * f - отступ снизу
                 * b,c - поворот
                 */
                val (dHeight, dWidth) = when (orientation) {
                    Orientation.PORTRAIT -> Pair(0.5f, 0.5f)
                    Orientation.LANDSCAPE -> Pair(
                        PDRectangle.A4.width / PDRectangle.A4.height,
                        PDRectangle.A4.height / (2 * PDRectangle.A4.width)
                    )
                }

                val (stepLeft, stepDown) = when (orientation) {
                    Orientation.PORTRAIT -> when (index) {
                        0 -> Pair(0f, npage.mediaBox.height / 2)
                        1 -> Pair(npage.mediaBox.width / 2, npage.mediaBox.height / 2)
                        2 -> Pair(0f, 0f)
                        3 -> Pair(npage.mediaBox.width / 2, 0f)
                        else -> throw Exception()
                    }

                    Orientation.LANDSCAPE -> when (index) {
                        0 -> Pair(0f, 0f)
                        1 -> Pair(npage.mediaBox.width / 2, 0f)
                        else -> throw Exception()
                    }
                }

                val matrix = Matrix(dWidth, 0f, 0f, dHeight, stepLeft, stepDown)

                contents.transform(matrix)
                contents.drawForm(form)
                contents.restoreGraphicsState()
            }

            contents.close()
        }
        val out = ByteArrayOutputStream()
        outPdf.save(out)

        val nDocument = Document(
            ByteArrayInputStream(out.toByteArray()),
            DocumentType.PDF,
            outPdf.pages.count
        )

        document.close()
        outPdf.close()

        return nDocument
    }

    /**
     * Метод создает новый PDF-файл на основании исходного PDF-файла из страниц указанных в диапазоне
     *
     * На вход принимает [InputStream PDF документа] [stream] и диапазон [pages] необходимых страниц.
     * Возвращает новый [PDF-документ][Document]
     */
    fun splitPdf(stream: InputStream, pages: IntRange): Document {
        val sourceDocument = PDDocument.load(stream)
        val document = PDDocument()
        val outputStream = ByteArrayOutputStream()

        pages.forEach { pageNumber -> document.addPage(sourceDocument.getPage(pageNumber - 1)) }

        document.save(outputStream)

        val resultDoc = Document(
            ByteArrayInputStream(outputStream.toByteArray()),
            DocumentType.PDF,
            document.pages.count
        )

        sourceDocument.close()
        document.close()

        return resultDoc
    }

    /**
     * Получить количество страниц в файле
     */
    override fun countPages(format: DocFormat, content: InputStream) = runCatching {
        when (format) {
            DocFormat.PDF -> {
                PDDocument.load(content).use { it.numberOfPages }
            }

            DocFormat.DOCX -> {
                XWPFDocument(content).properties.extendedProperties.pages
            }

            DocFormat.XLS -> {
                HSSFWorkbook(content).getSheetAt(0).physicalNumberOfRows
            }

            else -> throw DocumentServiceException("Для формата ${format.value} еще не реализован подсчет страниц")
        }
    }

    /**
     * Сгенерировать PDF-документ
     *
     * @param dto объект с данными для подстановки в шаблон документа
     * @param pageCountCalculation необходимость просчёта количества страниц в документе
     *
     * @return сгенерированный документ
     */
    fun pdf(dto: GeneratedDoc, pageCountCalculation: Boolean = false): Result<Document> = runBlocking {
        runCatching {
            val bytes = fileGenerator.generatePdf(dto)
            val pageCount = if (pageCountCalculation) PDDocument.load(bytes).numberOfPages else null

            Document(
                stream = ByteArrayInputStream(bytes),
                type = DocumentType.PDF,
                pageCount = pageCount
            )
        }
    }

    /**
     * Сгенерировать TXT-документ
     *
     * @param dto объект с данными для подстановки в шаблон документа
     */
    fun txt(dto: WithoutTemplateDoc): Result<TxtDocument> = runCatching { fileGenerator.generateTxt(dto) }

    /**
     * Сгенерировать архив с документами
     *
     * @param builderAction конфигуратор архива
     *
     * @return сгенерированный архив
     */
    fun zip(builderAction: ZipBuilder.() -> Unit): Result<ZipArchive> = runBlocking {
        runCatching {
            val builder = ZipBuilderImpl().also(builderAction::invoke)

            // Специфичная кодировка для кириллицы в архиве
            val innerOutputStream = ByteArrayOutputStream()
            val outputStream = ZipOutputStream(innerOutputStream, Charset.forName("CP866"))

            val zipDocuments = builder.build()
                .map { it.invoke() }
                .onEach { element ->
                    val fileName = element.filename()
                    val fileBytes = element.content.stream.readAllBytes()

                    outputStream.putNextEntry(ZipEntry(fileName))
                    outputStream.write(fileBytes)
                }
                .map(ZipElement<*>::content)

            outputStream.closeEntry()
            outputStream.close()

            ZipArchive(
                stream = innerOutputStream.inputStream(),
                elements = zipDocuments
            )
        }
    }

    /**
     * Сгенерировать объединённый документ (pdf)
     *
     * @param pageCountCalculation необходимость просчёта количества страниц после генерации
     * @param builderAction конфигуратор объединённого документа
     *
     * @return сгенерировать документ
     */
    fun mergedPdf(
        pageCountCalculation: Boolean = false,
        builderAction: MergedPdfBuilder.() -> Unit
    ): Result<Document> = runBlocking {
        runCatching {
            val builder = MergedPdfBuilderImpl().also(builderAction::invoke)

            val mergedBytes = builder.build()
                .asSequence()
                .map { it.invoke() }
                .map(Document::stream)
                .asIterable()
                .let(fileGenerator::mergePdf)

            Document(
                stream = mergedBytes.inputStream(),
                type = DocumentType.PDF,
                pageCount = if (pageCountCalculation) PDDocument.load(mergedBytes).numberOfPages else null
            )
        }
    }

    private inner class ZipBuilderImpl : ZipBuilder {
        private val elementSuppliers = mutableListOf<() -> ZipElement<*>>()

        override fun docx(
            dto: GeneratedDoc,
            pageCountCalculation: Boolean,
            fileNamingStrategy: FileNamingStrategy<Document>
        ) {
            elementSuppliers.add {
                ZipElement(
                    content = this@DocumentService.docx(dto, pageCountCalculation).getOrThrow(),
                    fileNamingStrategy = fileNamingStrategy
                )
            }
        }

        override fun pdf(
            dto: GeneratedDoc,
            pageCountCalculation: Boolean,
            fileNamingStrategy: FileNamingStrategy<Document>
        ) {
            elementSuppliers.add {
                ZipElement(
                    content = this@DocumentService.pdf(dto, pageCountCalculation).getOrThrow(),
                    fileNamingStrategy = fileNamingStrategy
                )
            }
        }

        override fun pdf(
            stream: InputStream,
            pageCountCalculation: Boolean,
            fileNamingStrategy: FileNamingStrategy<Document>
        ) {
            val bytes = stream.readAllBytes()
            elementSuppliers.add {
                ZipElement(
                    content = Document(
                        stream = bytes.inputStream(),
                        type = DocumentType.PDF,
                        pageCount = PDDocument.load(bytes).use { it.numberOfPages }
                    ),
                    fileNamingStrategy = fileNamingStrategy,
                    pageCountCalculation = pageCountCalculation
                )
            }
        }

        override fun zip(zip: ZipArchive, fileNamingStrategy: FileNamingStrategy<ZipArchive>) {
            elementSuppliers.add {
                ZipElement(
                    content = zip,
                    fileNamingStrategy = fileNamingStrategy
                )
            }
        }

        override fun mergedPdf(
            pageCountCalculation: Boolean,
            fileNamingStrategy: FileNamingStrategy<Document>,
            builderAction: MergedPdfBuilder.() -> Unit
        ) {
            elementSuppliers.add {
                ZipElement(
                    content = this@DocumentService.mergedPdf(pageCountCalculation, builderAction).getOrThrow(),
                    fileNamingStrategy = fileNamingStrategy
                )
            }
        }

        override fun external(
            stream: InputStream,
            type: DocumentType,
            fileNamingStrategy: FileNamingStrategy<Content>
        ) {
            elementSuppliers.add {
                ZipElement(
                    content = object : Content {
                        override val type = type
                        override val stream: InputStream = stream
                    },
                    fileNamingStrategy = fileNamingStrategy
                )
            }
        }

        fun build(): List<() -> ZipElement<*>> = elementSuppliers.toList()
    }

    private data class ZipElement<T : Content>(
        val content: T,
        val fileNamingStrategy: FileNamingStrategy<T>,
        val pageCountCalculation: Boolean = false
    ) {
        fun filename() = fileNamingStrategy.name(content)
    }

    private inner class MergedPdfBuilderImpl : MergedPdfBuilder {
        private val elements = mutableListOf<() -> Document>()

        override fun pdf(dto: GeneratedDoc) {
            elements.add { this@DocumentService.pdf(dto).getOrThrow() }
        }

        override fun pdf(stream: InputStream) {
            elements.add {
                Document(
                    stream = stream,
                    type = DocumentType.PDF
                )
            }
        }

        fun build(): List<() -> Document> = elements.toList()
    }

    override fun <T> get(type: DocType, data: T, format: DocFormat): InputStream {
        return when (type) {
            DocType.COURT_ORDER -> {
                data as ActOnAbsenceOfDocumentDTO
                when (data.options.needCombine) {
                    true -> {
                        val ba = data.stream.readAllBytes()
                        val numberOfPages = countPages(DocFormat.PDF, ba.inputStream()).getOrThrow()

                        //Свидетельство
                        val certificate = splitPdf(ba.inputStream(), 1..<2)

                        // Выписка
                        val egrul = splitPdf(ba.inputStream(), 2..numberOfPages).stream
                        val combinedEgrul = combine(egrul)

                        mergedPdf {
                            pdf(certificate.stream)
                            pdf(combinedEgrul.stream)
                        }.getOrThrow().stream
                    }
                    false -> data.stream
                }
            }

            else -> TODO("Not yet implemented")
        }

    }
}
