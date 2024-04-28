package ru.alex3koval.docService.generation

import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Repeatable
internal annotation class Modifier(val clazz: KClass<*>)
