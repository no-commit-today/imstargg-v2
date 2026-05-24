package com.imstargg.core.config

import com.fasterxml.jackson.annotation.JsonTypeInfo
import org.springframework.core.KotlinDetector
import org.springframework.util.ClassUtils
import tools.jackson.databind.DefaultTyping
import tools.jackson.databind.JavaType
import tools.jackson.databind.jsontype.PolymorphicTypeValidator
import tools.jackson.databind.jsontype.impl.DefaultTypeResolverBuilder

class StatisticsTypeResolverBuilder(
    validator: PolymorphicTypeValidator,
) : DefaultTypeResolverBuilder(
        validator,
        DefaultTyping.NON_FINAL,
        JsonTypeInfo.As.PROPERTY,
        JsonTypeInfo.Id.CLASS,
        "@class",
    ) {
    override fun withDefaultImpl(defaultImpl: Class<*>?): DefaultTypeResolverBuilder = this

    override fun useForType(javaType: JavaType): Boolean {
        if (javaType.isJavaLangObject) return true
        val resolved = resolveArrayOrWrapper(javaType)
        val raw: Class<*> = resolved.rawClass
        if (resolved.isEnumType) return false
        if (ClassUtils.isPrimitiveOrWrapper(raw)) return false
        if (raw.kotlin.isValue) return false
        if (resolved.isFinal &&
            !KotlinDetector.isKotlinType(raw) &&
            raw.packageName.startsWith("java")
        ) {
            return false
        }
        return true
    }

    private fun resolveArrayOrWrapper(type: JavaType): JavaType {
        var current = type
        while (current.isArrayType) {
            current = current.contentType
        }
        while (current.isReferenceType) {
            current = current.referencedType
        }
        return current
    }
}
