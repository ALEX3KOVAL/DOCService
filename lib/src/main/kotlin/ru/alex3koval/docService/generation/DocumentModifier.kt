package ru.alex3koval.docService.generation

import ru.alex3koval.docService.GeneratedDoc

internal interface DocumentModifier<T : GeneratedDoc> {
    fun modify(dto: T, context: DocxGenerationContext<T>)
}
