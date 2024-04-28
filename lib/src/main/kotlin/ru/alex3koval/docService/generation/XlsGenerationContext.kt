package ru.alex3koval.docService.generation

import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.InputStream
import kotlin.reflect.KProperty1
import kotlin.reflect.full.createType

/**
 * DocxGenerationContext выполняет основную работу по генерации документа docx, вызывает внутри себя другие
 * контексты для отображения специфичных блоков вроде таблиц и списков.
 */
class XlsGenerationContext<D : Any>(template: InputStream, val dto: D) {
    // документ с которым идет работа
    private val document: XSSFWorkbook = XSSFWorkbook(template)
    private val plainFields = buildMap {
        dto::class.members.filterIsInstance<KProperty1<*, *>>().forEach {
            when (it.returnType) {
                String::class.createType() -> {
                    val p = it as KProperty1<D, String>
                    this[it.name] = it.getValue(dto, p)
                }
            }
        }
    }

    fun generate(): XSSFWorkbook {
        document.getSheetAt(0).forEach rows@{ row ->
            row.filter { it.cellType == CellType.STRING }.forEach cells@{ cell ->
                val cellValue = cell.stringCellValue.replace("[{$}]".toRegex(), "")
                if (plainFields[cellValue] == null) return@cells
                cell.setCellValue(plainFields[cellValue])
            }
        }

        document.getSheetAt(0).forEach { row ->
            row.filter { it.cellType == CellType.STRING }.forEach { cell ->
                if ("[$][{]\\w+[}]".toRegex().findAll(cell.stringCellValue).any()) {
                    throw RuntimeException("Остались незаполненные поля")
                }
            }
        }

        return document
    }
}

