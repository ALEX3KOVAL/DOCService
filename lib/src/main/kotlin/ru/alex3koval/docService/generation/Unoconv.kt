package ru.alex3koval.docService.generation

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.*
import ru.alex3koval.docService.DocumentServiceException
import java.util.*

private data class Url(val url: String, var countUsages: Int = 0)

/**
 * Класс для конвертации заданного файла в PDF
 */
class Unoconv(unoconvBaseUrls: List<String>) {
    private val urls = unoconvBaseUrls.map { Url(it) }
    private val maxRecursionDepth = 5

    @OptIn(InternalAPI::class)
    internal suspend fun convertDocToPdf(docx: ByteArray, depth: Int = 0): ByteArray {
        val baseUrl = urls.minBy { it.countUsages }
        baseUrl.countUsages++
        val httpClient = HttpClient()

        repeat(10) {
            runCatching {
                httpClient.post("${baseUrl.url}/pdf") {
                    body = MultiPartFormDataContent(
                        formData {
                            append(
                                "file",
                                docx,
                                headersOf(HttpHeaders.ContentDisposition, "filename=${UUID.randomUUID()}")
                            )
                        }
                    )
                }.apply {
                    if (!status.isSuccess()) {
                        return@repeat
                    }

                    return readBytes()
                }
            }.onFailure {
                if (depth == maxRecursionDepth) throw it
                return convertDocToPdf(docx = docx, depth = depth + 1)
            }
        }


        throw DocumentServiceException("Не получилось сконвертировать документ за 10 попыток")
    }
}
