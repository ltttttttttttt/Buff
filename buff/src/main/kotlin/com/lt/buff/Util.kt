package com.lt.buff

import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.Nullability
import java.io.OutputStream

/**
 * creator: lt  2022/10/21  lt.dygzs@qq.com
 * effect : 工具类
 * warning:
 */

internal val buffName = Buff::class.simpleName

/**
 * 向os中写入文字
 */
internal fun OutputStream.appendText(str: String) {
    this.write(str.toByteArray())
}

/**
 * 如果字符串为空或长度为0,就使用lambda中的字符串
 */
internal inline fun String?.ifNullOfEmpty(defaultValue: () -> String): String =
    if (this.isNullOrEmpty()) defaultValue() else this

/**
 * 打印日志
 */
internal fun String?.w(environment: SymbolProcessorEnvironment) {
    environment.logger.warn("lllttt buff: ${this ?: "空字符串"}")
}

/**
 * 获取ksType的信息
 */
internal fun getKSTypeInfo(ks: KSTypeReference): KSTypeInfo {
    val ksType = ks.resolve()
    val isBuffBean =
        ksType.declaration.annotations.toList()
            .find { it.shortName.getShortName() == buffName } != null
    val typeName =
        "${ksType.declaration.packageName.asString()}.${ksType.declaration.simpleName.asString()}"
    val nullable = if (ksType.nullability == Nullability.NULLABLE) "?" else ""
    return KSTypeInfo(
        ksType,
        isBuffBean,
        typeName,
        nullable,
    )
}

internal data class KSTypeInfo(
    val ksType: KSType,
    val isBuffBean: Boolean,
    val typeName: String,
    val nullable: String,
)