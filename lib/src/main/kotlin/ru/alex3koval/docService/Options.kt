package ru.alex3koval.docService

data class Options(
    val needCombine: Boolean = false,
    val needSplit: Boolean = false,
    val pagesForSplit: List<Int> = listOf()
)