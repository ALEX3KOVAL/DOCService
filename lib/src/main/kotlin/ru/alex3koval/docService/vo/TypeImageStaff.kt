package ru.alex3koval.docService.vo

/**
 * Список печатей, подписей и печатей с подписью сотрудников используемых при генерации документов
 */
enum class TypeImageStaff(val id: String) {
    STAMP("stamp"),
    SIGN("sign"),

    /** "staffImage": "SIGN_WITH_STAMP" - ключ для JSON поля, signStamp - переменная внутри шаблона */
    SIGN_WITH_STAMP("signStamp");

    fun path(): String {
        return when (this) {
            STAMP -> "stamp.png"
            SIGN -> "sign.png"
            SIGN_WITH_STAMP -> "signStamp.png"
        }
    }

    companion object {
        fun fromString(value: String) = entries.first { it.id == value }
    }
}
