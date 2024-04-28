package ru.alex3koval.docService.generation

/** Нужно ли вставлять в документ печать или подпись */
enum class StampNecessity {
    NO,
    YES;

    fun isNeed() = this == YES
}
