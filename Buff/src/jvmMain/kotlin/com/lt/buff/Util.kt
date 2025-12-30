package com.lt.buff

import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSTypeReference
import com.lt.buff.options.BuffKSTypeInfo
import com.lt.buff.options.KspOptions
import com.lt.ksp.getKSTypeInfo
import com.lt.ksp.isList

/**
 * creator: lt  2022/10/21  lt.dygzs@qq.com
 * effect : 工具类
 * warning:
 */

internal val buffName = Buff::class.simpleName

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
 * [ks] KSTypeReference信息
 * [isFirstFloor] 是否是最外层,用于判断泛型
 */
internal fun getBuffKSTypeInfo(ks: KSTypeReference, thisClass: KSClassDeclaration, isFirstFloor: Boolean = true): BuffKSTypeInfo {
    //type对象
    val ksType = ks.resolve()
    val ksTypeInfo = getKSTypeInfo(ksType, null, thisClass)
    //类是否有Buff注解
    val isBuffBean =
        ksType.declaration.annotations.toList()
            .find { it.shortName.getShortName() == buffName } != null
    //泛型中是否包含Buff注解
    var typeHaveBuff = false
    //完整type字符串
    val typeName = ksTypeInfo.thisTypeName.toString()
    //是否可空
    val nullable = if (ksTypeInfo.nullable) "?" else ""
    //是否是List<T>,后续在需要的地方会将List<T>转换为mutableStateListOf()
    val isList = ksType.isList()
    var isMutableList = false
    if (isList) {
        val declaration = ksType.declaration
        val packageName = declaration.packageName.asString()
        val className = declaration.simpleName.asString()
        if (packageName != "kotlin.collections" || className != "List")
            isMutableList = true
    }
    //泛型(只支持List<T>)
    val typeString =
        if (
            isFirstFloor
            && isList
            && ksType.arguments.size == 1
            && ksType.arguments.first().type != null
        ) {
            //处理List<T>,支持转state,且自动加Buff
            val info = getBuffKSTypeInfo(ksType.arguments.first().type!!, thisClass, false)
            typeHaveBuff = info.isBuffBean
            val finallyTypeName = info.finallyTypeName
            if (finallyTypeName.isNotEmpty())
                "<$finallyTypeName>"
            else
                finallyTypeName
        } else if (ksType.arguments.isEmpty())
            ""//无泛型
        else {
            //其他泛型,原样输出
            ksTypeInfo.childType.joinToString(prefix = "<", postfix = ">")
        }
    //最后确定下来的type名字
    val finallyTypeName =
        if (isBuffBean) "$typeName${KspOptions.suffix}$typeString$nullable" else "$typeName$typeString$nullable"
    return BuffKSTypeInfo(
        ksType,
        //自身或泛型包含Buff注解
        isBuffBean || typeHaveBuff,
        typeName,
        nullable,
        finallyTypeName,
        typeString,
        isList,
        isMutableList,
    )
}

/**
 * 获取注解的全类名
 */
internal fun getAnnotationFullClassName(ksa: KSAnnotation): String {
    val ksType = ksa.annotationType.resolve()
    //完整type字符串
    return ksType.declaration.let {
        val name = it.qualifiedName?.asString()
        if (name != null)
            return@let name
        val packageName = it.packageName.asString()
        return@let if (packageName.isEmpty())
            ksa.shortName.asString()
        else
            "$packageName.${it.simpleName.asString()}"
    }
}