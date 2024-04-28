package ru.alex3koval.docService

import java.io.InputStream

/**
 * Интерфейс генерируемого документа
 */
abstract class GeneratedDoc {
    abstract val template: InputStream
}

/**
 * Интерфейс генерируемого документа без шаблона
 */
interface WithoutTemplateDoc
