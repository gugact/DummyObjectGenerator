package com.gugact.dummyobjectgenerator

import kotlin.reflect.KClass
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.typeOf

object Generator {

    private val registry = mutableMapOf<KClass<*>, Any>()

    init {
        reset()
    }

    fun reset() {
        registry.clear()
        register(0)
        register(0.toByte())
        register(0.toChar())
        register(0.toDouble())
        register(0.toFloat())
        register(0.toShort())
        register(0.toLong())
        register(false)
        register("0")
    }

    inline fun <reified T : Any> register(value: T) {
        register(T::class, value)
    }

    fun <T : Any> register(kClass: KClass<T>, value: T) {
        if (kClass.typeParameters.isNotEmpty()) {
            throw IllegalArgumentException("Registering generic classes is not supported")
        }
        registry[kClass] = value
    }


    inline fun <reified T : Any> default(): T {
        return default(T::class, typeOf<T>().arguments) as T
    }

    fun default(kClass: KClass<*>?, argumentsList: List<KTypeProjection>?): Any {
        return registry[kClass] ?: run {
            when {
                kClass?.isData == true -> defaultDataClass(kClass)
                kClass == List::class -> listOf(default(argumentsList?.getClassFromIndex(0), argumentsList?.getArgumentsFromIndex(0)))
                kClass == MutableList::class -> mutableListOf(default(argumentsList?.getClassFromIndex(0), argumentsList?.getArgumentsFromIndex(0)))
                kClass == Map::class -> mapOf(default(argumentsList?.getClassFromIndex(0), argumentsList?.getArgumentsFromIndex(0)) to default(argumentsList?.getClassFromIndex(1), argumentsList?.getArgumentsFromIndex(1)))
                kClass == MutableMap::class -> mutableMapOf(default(argumentsList?.getClassFromIndex(0), argumentsList?.getArgumentsFromIndex(0)) to default(argumentsList?.getClassFromIndex(1), argumentsList?.getArgumentsFromIndex(1)))
                kClass == Set::class -> setOf(default(argumentsList?.getClassFromIndex(0), argumentsList?.getArgumentsFromIndex(0)))
                kClass == MutableSet::class -> mutableSetOf(default(argumentsList?.getClassFromIndex(0), argumentsList?.getArgumentsFromIndex(0)))
                else -> throw IllegalStateException("Class $kClass is not registered")
            }
        }
    }

    private fun defaultDataClass(kClass: KClass<*>): Any {
        val constructor = kClass.primaryConstructor
            ?: throw IllegalArgumentException("No primary constructor found for data class $kClass")
        val arguments = constructor.parameters
            .associate { param ->
                when (param.type.classifier) {
                    is KClass<*> -> param to default(param.type.classifier as KClass<*>, param.type.arguments)
                    else -> throw IllegalArgumentException("Unsupported type ${param.type}")
                }
            }
        return constructor.callBy(arguments)
    }

    private fun List<KTypeProjection>?.getArgumentsFromIndex(index: Int) = this?.get(index)?.type?.arguments
    private fun List<KTypeProjection>?.getClassFromIndex(index: Int) = this?.get(index)?.type?.classifier as? KClass<*>

    inline fun <reified T : Any> T.forEachMember(action: (Any?) -> Unit) {
        for (prop in T::class.memberProperties) {
            action(prop.call(*(prop.parameters.map { param ->
                default(param.type.classifier as KClass<*>, param.type.arguments)
            }.toTypedArray())))
        }
    }
}