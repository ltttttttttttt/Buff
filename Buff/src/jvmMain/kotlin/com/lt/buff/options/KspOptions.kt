package com.lt.buff.options

import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.lt.buff.ifNullOfEmpty

/**
 * creator: lt  2022/10/23  lt.dygzs@qq.com
 * effect : buff的配置
 * warning:
 */
internal class KspOptions(environment: SymbolProcessorEnvironment) {
    companion object {
        const val suffix = "WithBuff"//后缀
    }
    private val options = environment.options
    private val customInClass = "customInClass$suffix"
    private val customInFile = "customInFile$suffix"

    /**
     * 获取类中自定义的代码
     */
    fun getCustomInClass(info: CustomOptionsInfo): String {
        return handlerCustomCode(options[customInClass].ifNullOfEmpty { return "" }, info)
    }

    /**
     * 获取文件中自定义的代码
     */
    fun getCustomInFile(info: CustomOptionsInfo): String {
        return handlerCustomCode(options[customInFile].ifNullOfEmpty { return "" }, info)
    }

    /**
     * 处理自定义代码,将特殊字段替换为真实数据
     */
    private fun handlerCustomCode(code: String, info: CustomOptionsInfo): String {
        return code.replace("#originalClassName#", info.originalClassName)
            .replace("#className#", info.className)
    }
}
