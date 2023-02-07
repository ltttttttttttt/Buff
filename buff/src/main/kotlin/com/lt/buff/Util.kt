package com.lt.buff

import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.Nullability
import com.lt.buff.options.KSTypeInfo
import com.lt.buff.options.KspOptions
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
internal fun getKSTypeInfo(ks: KSTypeReference, options: KspOptions): KSTypeInfo {
    //type对象
    val ksType = ks.resolve()
    //类是否有Buff注解
    val isBuffBean =
        ksType.declaration.annotations.toList()
            .find { it.shortName.getShortName() == buffName } != null
    //泛型 // TODO by lt 2023/2/7 23:00 处理buff问题,将需要转state的list转为statelist
    var typeString = ksType.arguments.filter { it.type != null }.joinToString {
        getKSTypeInfo(it.type!!, options).finallyTypeName
    }
    if (!typeString.isEmpty()) {
        typeString = "<$typeString>"
    }
    //完整type字符串
    val typeName =
        "${ksType.declaration.packageName.asString()}.${ksType.declaration.simpleName.asString()}"
    //是否可空
    val nullable = if (ksType.nullability == Nullability.NULLABLE) "?" else ""
    //最后确定下来的type名字
    val finallyTypeName =
        if (isBuffBean) "$typeName${options.suffix}$typeString$nullable" else "$typeName$typeString$nullable"
    return KSTypeInfo(
        ksType,
        isBuffBean,
        typeName,
        nullable,
        finallyTypeName,
    )
}