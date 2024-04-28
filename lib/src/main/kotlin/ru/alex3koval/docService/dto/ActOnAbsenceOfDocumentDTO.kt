package ru.alex3koval.docService.dto

import ru.alex3koval.docService.Options
import java.io.InputStream

data class ActOnAbsenceOfDocumentDTO(
    val options: Options,
    val stream: InputStream
)
