package ru.alex3koval.docService

interface TxtDocumentRender<T : WithoutTemplateDoc> {
    fun render(dto: T): TxtDocument
}
