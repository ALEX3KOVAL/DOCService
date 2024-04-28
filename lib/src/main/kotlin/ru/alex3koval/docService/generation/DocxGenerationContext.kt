package ru.alex3koval.docService.generation

import org.apache.poi.common.usermodel.PictureType
import org.apache.poi.util.Units
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xwpf.usermodel.XWPFRun
import ru.alex3koval.docService.GeneratedDoc
import ru.alex3koval.docService.vo.TypeImageStaff
import java.awt.Dimension
import java.io.InputStream
import kotlin.reflect.KProperty1
import kotlin.reflect.full.createType
import kotlin.reflect.full.memberProperties

/**
 * DocxGenerationContext выполняет основную работу по генерации документа docx, вызывает внутри себя другие
 * контексты для отображения специфичных блоков вроде таблиц и списков.
 */
internal class DocxGenerationContext<D : Any>(template: InputStream, val dto: D) {
    // карта с именами и значениями простых текстовых полей для подстановки
    private val plainField = mutableMapOf<String, String>()

    /**
     * Список с контекстами генерации таблиц, так как контекст сам отвечает за поиск и отрисовку
     * и хранит в себе имя элемента для подстановки, то используется просто список, а не карта
     */
    private val tables = mutableListOf<DocxTableGenerationContext<*>>()

    // Список с контекстами генерации списков, работает по аналогии со списком таблиц
    private val list = mutableListOf<DocxListGenerationContext<*>>()

    /**
     * Карта изображений сотрудника (печать, подпись, печать и подпись) используемых в документе
     *
     * Ключ: тип изображения (печать, подпись, печать и подпись)
     * Значение: путь до ресурса (/templates/images/staffDocs/XXX/sign.png)
     */
    private val staffImages = mutableMapOf<String, InputStream>()

    // документ с которым идет работа
    private val document: XWPFDocument = XWPFDocument(template)

    /**
     * Производит связывание параметров, в данном методе обрабатываются
     * плоские типы - строковые и изображения, подстановка списков происходит в другом методе
     *
     * Метод проходит по всем полям data класса, и выбирает только поля нужного типа,
     * в этом плане он является универсальным
     */
    private fun bind() {
        dto::class.members.filterIsInstance<KProperty1<*, *>>().forEach {
            when (it.returnType) {
                String::class.createType() -> {
                    val p = it as KProperty1<D, String>
                    plainField[it.name] = it.getValue(dto, p)
                }

                InputStream::class.createType() -> {
                    val p = it as KProperty1<D, InputStream>
                    staffImages[it.name] =  it.getValue(dto, p)
                }
            }
        }
    }

    /**
     * Метод генерирует docx документ и возвращает результат.
     *
     * Внимание: в конце метода происходит валидация документа - если остались неподставленные параметры
     * вида ${fieldName} то будет возбуждено исключение типа RuntimeException. Сделано специально чтобы
     * конечному пользователю не выгрузился некорректно созданный документ
     */
    fun generate(): XWPFDocument {
        // связываем плоские поля
        bind()

        // заменяем все вхождения строковых типов внутри параграфов
        document.scan { it.setText(it.text().putData(), 0) }

        // Отрисовываем все таблицы в документе
        tables.forEach { it.render() }
        // Отрисовываем все списки в документе
        list.forEach { it.render() }

        // подставляем все печати внутри документа
        document.putStamps()

        // Валидируем результат и ломаемся если остались неподставленные данные
        document.validate()

        return document
    }

    /**
     * Добавляет в документ новый контекст для отображения таблицы
     * @param tableName имя элемента, в котором будет отрисована таблица:
     *      - в шаблоне ищется таблица в первой ячейке первой строки которой стоит поле ${tableName}
     *      - в данном методе имя передается без символов $,{,}
     * @param data данные для отображения, за счет использования обобщения внутри лямбды builder будут
     *        доступны типизированные данные
     * @param builder лямбда для отрисовки внешнего вида таблицы
     */
    fun <T> table(tableName: String, data: List<T>, builder: DocxTableGenerationContext<T>.() -> Unit) {
        tables.add(
            DocxTableGenerationContext(document, tableName, data).apply {
                builder()
            }
        )
    }

    /**
     * Добавляет в документ новый контекст для отображения списка. Особенности отрисовки списков смотрите в
     * документации к ListGenerationContext
     * @param listName имя элемента в котором будет отрисована таблица:
     *      - в данном методе имя передается без символов $,{,}
     * @param data данные для отображения, за счет использования обобщения внутри лямбды builder будут
     *        доступны типизированные данные
     * @param builder лямбда для отрисовки внешнего вида списка
     */
    fun <T> list(listName: String, data: List<T>, builder: DocxListGenerationContext<T>.() -> Unit) {
        list += DocxListGenerationContext(listName, data, document).apply { builder() }
    }

    /**
     * Функция для замены всех вхождений полей вида ${fieldName} на конкретные данные
     */
    private fun String.putData(): String {
        var result = this
        plainField.forEach { (key, value) ->
            result = result.replace("\${$key}", value)
        }

        return result
    }

    /**
     * Подставляет в нужные места документа печати
     */
    private fun XWPFDocument.putStamps() {
        val needStamp = dto::class.memberProperties.firstOrNull { it.returnType == StampNecessity::class.createType() }?.getter?.call(dto) as StampNecessity?
        this.scan { r ->
            staffImages.filterNot { it.key == "template" }.forEach { (name, resource) ->
                if (r.text().contains("\${$name}")) {
                    r.setText("", 0)

                    /** Если для генерации документа не требуются подпись или подпись и печать, то не подставляем  */
                    if (needStamp == null || needStamp.isNeed()) {
                        val dimension = when (TypeImageStaff.fromString(name)) {
                            TypeImageStaff.SIGN -> Dimension(100, 100)
                            else -> Dimension(150, 150)
                        }

                        r.addPicture(
                            resource,
                            PictureType.PNG,
                            "stamp",
                            Units.toEMU(dimension.width.toDouble()),
                            Units.toEMU(dimension.height.toDouble())
                        )
                    }
                }
            }
        }
    }
}

internal fun <T : GeneratedDoc> docx(template: InputStream, data: T, modifier: DocumentModifier<T>? = null): XWPFDocument {
    return DocxGenerationContext(template, data).run {
        modifier?.modify(data, this)
        generate()
    }
}

/**
 * Проходит по всем run-ам в документе и применяет к ним переданную лямбду
 */
private fun XWPFDocument.scan(processor: (XWPFRun) -> Unit) {
    // Сканируем параграфы, которые находятся внутри таблиц,
    // например футер, где подпись, печать и сотрудника ФИО
    this.tables.forEach { t ->
        t.rows.forEach { r ->
            r.tableCells.forEach { c ->
                c.paragraphs.forEach { pg ->
                    pg.runs.forEach { pgr -> processor(pgr) }
                }
            }
        }
    }

    this.footerList.forEach { footer ->
        footer.paragraphs.forEach { p ->
            p.runs.forEach { pr -> processor(pr) }
        }
    }

    this.paragraphs.forEach { p ->
        p.runs.forEach { r -> processor(r) }
    }
}
