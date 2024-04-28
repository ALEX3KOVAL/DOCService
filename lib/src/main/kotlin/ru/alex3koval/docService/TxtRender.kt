package ru.alex3koval.docService

import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
internal annotation class TxtRender(val clazz: KClass<*>)
