package ru.alex3koval.docService

import ru.alex3koval.docService.vo.DocumentType
import ru.alex3koval.docService.vo.throwIfDoesNotExists
import java.io.File
import java.io.InputStream

/**
 * Билдер для zip-архива
 */
interface ZipBuilder {
    /**
     * Добавить docx, сгенерированный из шаблона
     *
     * @param dto объект с данными для подстановки в шаблон документа
     * @param pageCountCalculation необходимость просчёта количества страниц в документе
     * @param fileNamingStrategy объект, содержащий правила наименования файла в архиве
     * */
    fun docx(
        dto: GeneratedDoc,
        pageCountCalculation: Boolean = false,
        fileNamingStrategy: FileNamingStrategy<Document>
    )

    /**
     * Добавить pdf, сгенерированный из шаблона
     *
     * @param dto объект с данными для подстановки в шаблон документа
     * @param pageCountCalculation необходимость просчёта количества страниц в документе
     * @param fileNamingStrategy объект, содержащий правила наименования файла в архиве
     * */
    fun pdf(
        dto: GeneratedDoc,
        pageCountCalculation: Boolean = false,
        fileNamingStrategy: FileNamingStrategy<Document>
    )

    /**
     * Добавить pdf из внешнего источника
     *
     * @param stream поток байтов документа из внешнего источника
     * @param pageCountCalculation необходимость просчёта количества страниц в документе
     * @param fileNamingStrategy объект, содержащий правила наименования файла в архиве
     * */
    fun pdf(
        stream: InputStream,
        pageCountCalculation: Boolean = false,
        fileNamingStrategy: FileNamingStrategy<Document>
    )

    /**
     * Добавить zip-архив сформированный ранее
     * @param zip Архив(сформированный ранее) для добавления в архив
     * @param fileNamingStrategy объект, содержащий правила наименования файла в архиве
     */
    fun zip(
        zip:ZipArchive,
        fileNamingStrategy: FileNamingStrategy<ZipArchive>
    )

    /**
     * Добавить объединённый pdf
     *
     * @param pageCountCalculation необходимость просчёта количества страниц в документе
     * @param fileNamingStrategy объект, содержащий правила наименования файла в архиве
     * @param builderAction конфигуратор объединённого документа
     * */
    fun mergedPdf(
        pageCountCalculation: Boolean = false,
        fileNamingStrategy: FileNamingStrategy<Document>,
        builderAction: MergedPdfBuilder.() -> Unit
    )

    /**
     * Добавить файл из внешнего хранилища
     *
     * @param stream поток байтов файла из внешнего источника
     * @param type тип содержимого
     * @param fileNamingStrategy объект, содержащий правила наименования файла в архиве
     * */
    fun external(
        stream: InputStream,
        type: DocumentType,
        fileNamingStrategy: FileNamingStrategy<Content>
    )

    /**
     * Добавить файл из внешнего хранилища
     *
     * @param byteArray массив байтов файла из внешнего хранилища
     * @param type тип содержимого
     * @param fileNamingStrategy объект, содержащий правила наименования файла в архиве
     * */
    fun external(
        byteArray: ByteArray,
        type: DocumentType,
        fileNamingStrategy: FileNamingStrategy<Content>
    ) = external(byteArray.inputStream(), type, fileNamingStrategy)

    /**
     * Добавить файл из файловой системы
     *
     * @param file файл из файловой системы
     * @param type тип содержимого
     * @param fileNamingStrategy объект, содержащий правила наименования файла в архиве. Если не указан,
     * файл в архиве будет назван точно также, как и в файловой системе
     * */
    fun external(
        file: File,
        type: DocumentType,
        fileNamingStrategy: FileNamingStrategy<Content>? = null
    ) = external(
        stream = file.throwIfDoesNotExists().inputStream(),
        type = type,
        fileNamingStrategy = fileNamingStrategy ?: FileBasedFileNamingStrategy(file)
    )
}
