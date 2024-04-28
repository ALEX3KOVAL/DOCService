package ru.alex3koval.docService.generation

import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xwpf.usermodel.XWPFParagraph
import org.apache.xmlbeans.XmlCursor

/**
 * Контекст для генерации списка. Для каждого списка в документе создается свой контекст
 * @param document - документ в котором нужно отрисовать список
 * @param name - название поля куда будет вставляться список.
 * @param data - список с данными для отображения. Дженерик позволяет получить хорошую поддержку типов в
 * функциях отвечающих за отображение блоков таблицы
 */
internal class DocxListGenerationContext<T>(val name: String, val data: List<T>, val document: XWPFDocument) {
    /**
     * Для того чтобы отрисовывать элементы последовательно в нужной точке документа, пришлось добавить курсор
     * ЗА последний элемент последнего отрисованного элемента в документе. Из-за этой особенности нужно использовать
     * метод paragraph для создания новых параграфов (при необходимости это операции в лямбде отвечающей за
     * отрисовку одиночного элемента списка)
     */
    private lateinit var currentCursor: XmlCursor

    // функция отрисовки конкретного элемента
    var itemRenderer: DocxListGenerationContext<T>.(T, XWPFParagraph) -> Unit = { _, _ -> }

    /**
     * Регистрация функции отрисовки элемента списка
     */
    fun item(render: DocxListGenerationContext<T>.(T, XWPFParagraph) -> Unit) {
        itemRenderer = render
    }

    /**
     * Метод добавляет очередной параграф, выполняет на нем переданную лямбду и смещает курсор
     * вывода ЗА него
     */
    fun paragraph(builder: XWPFParagraph.() -> Unit): XWPFParagraph {
        val p = document.insertNewParagraph(currentCursor)
        p.builder()
        currentCursor = p.ctp.newCursor()
        currentCursor.toNextSibling()

        return p
    }

    /**
     * Отрисовывает список в документе. Для каждого элемента списка создается параграф включающий этот элемент
     */
    fun render() {
        // Ищем параграф в который нужно вывести список
        val startParagraph = document.paragraphs.fold(null) { acc: XWPFParagraph?, p: XWPFParagraph ->
            if (p.text.contains("\${$name}")) p else acc
        } ?: throw RuntimeException("Paragraph [$name] not found in template")

        // Удаляем из найденного параграфа все содержимое
        startParagraph.runs.forEach {
            it.setText("", 0)
        }

        // перемещаем внутренний курсор вывода за найденный параграф
        currentCursor = startParagraph.ctp.newCursor().apply { toNextSibling() }

        var currentP: XWPFParagraph? = null

        data.forEach {
            currentP = if (currentP == null) startParagraph else paragraph { }
            itemRenderer(it, currentP!!)
        }
    }
}
