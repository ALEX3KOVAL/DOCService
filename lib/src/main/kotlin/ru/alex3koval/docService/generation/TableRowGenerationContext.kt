package ru.alex3koval.docService.generation

import org.apache.poi.xwpf.usermodel.XWPFTableCell
import org.apache.poi.xwpf.usermodel.XWPFTableRow

/**
 * TableRowGenerationContext помогает выводить ячейки
 * в строке таблице при этом не следить за нумерацией,
 * потому что делает это автоматически
 */
internal class TableRowGenerationContext<T>(private val row: XWPFTableRow) {
    private var cellCount = -1
    fun cell(text: String): XWPFTableCell {
        cellCount++

        val cell = if (cellCount < row.tableCells.size) row.getCell(cellCount) else row.createCell()
        cell.text = text

        return cell
    }
}
