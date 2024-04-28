package ru.alex3koval.docService.generation

import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xwpf.usermodel.XWPFTable
import org.apache.poi.xwpf.usermodel.XWPFTableRow
import java.lang.RuntimeException

/**
 * Контекст для генерации таблицы. Для каждой таблицы в документе создается свой контекст
 * @param document - документ в котором нужно отрисовать таблицу
 * @param tableName - название поля куда будет вставляться таблица. ВНИМАНИЕ: для генерации любой таблицы применяется
 * одна и также методика - в шаблоне в нужном месте создается таблица с одним столбцом и одной ячейкой и в нее
 * помещается название таблицы в виде ${tableName}
 * @param data - список с данными для отображения. Дженерик позволяет получить хорошую поддержку типов в
 * функциях отвечающих за отображение блоков таблицы
 */
internal class DocxTableGenerationContext<T>(val document: XWPFDocument, val tableName: String, val data: List<T>) {
    private var headerRenderer: DocxTableGenerationContext<T>.() -> Unit = {}
    private var rowRenderer: TableRowGenerationContext<T>.(T) -> Unit = {}
    private var footerRenderer: DocxTableGenerationContext<T>.() -> Unit = {}

    fun header(render: DocxTableGenerationContext<T>.() -> Unit) {
        TODO()
        // headerRenderer = render
    }

    /**
     * Регистрирует функцию для отрисовки одной строки в таблице
     * @param render - лямбда будет выполняться на объекте типа TableRowGenerationContext
     */
    fun row(render: TableRowGenerationContext<T>.(T) -> Unit) {
        rowRenderer = render
    }

    fun footer(render: DocxTableGenerationContext<T>.() -> Unit) {
        TODO()
        // footerRenderer = render
    }

    /**
     * Занимается отрисовкой таблицы внутри документа. Сам ищет место куда должна быть вставлена и таблица
     * и производит все необходимые действия
     */
    fun render() {
        this.run {
            // ищем таблицу с нужным именем в документе
            val table = document.tables.fold(null) { acc: XWPFTable?, t: XWPFTable ->
                if (t.getRow(0).getCell(0).text.trim().contains("\${$tableName}")) t else acc
            } ?: throw RuntimeException("Table [$tableName] not found in template")

            // для удаления шаблона из первой ячейки таблицы
            table.getRow(0).getCell(0).removeParagraph(0)
            var row: XWPFTableRow? = null

            // отрисовка таблицы
            data.forEach {
                row = if (row == null) table.getRow(0) else table.createRow()
                val rowContext = TableRowGenerationContext<T>(row!!)
                rowContext.run {
                    rowRenderer(it)
                }
            }
        }
    }
}
