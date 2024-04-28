package ru.alex3koval.docService.vo

import ru.alex3koval.docService.DocumentServiceException
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream

internal fun File.throwIfDoesNotExists(
    exception: () -> Throwable = { DocumentServiceException("Файл $absolutePath отсутствует в файловой системе") }
): File = if (exists()) this else throw exception.invoke()

internal fun ByteArrayOutputStream.inputStream(): InputStream = toByteArray().inputStream()
