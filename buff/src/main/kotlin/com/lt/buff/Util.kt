package com.lt.buff

import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import java.io.OutputStream

/**
 * creator: lt  2022/10/21  lt.dygzs@qq.com
 * effect : 工具类
 * warning:
 */

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