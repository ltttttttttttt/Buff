package com.lt.buff.provider

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.validate
import com.lt.buff.Buff

/**
 * creator: lt  2022/10/20  lt.dygzs@qq.com
 * effect : ksp处理程序
 * warning:
 */
internal class BuffSymbolProcessor(private val environment: SymbolProcessorEnvironment) :
    SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val ret = mutableListOf<KSAnnotated>()
        resolver.getSymbolsWithAnnotation(Buff::class.qualifiedName!!)
            .toList()
            .forEach {
                if (it is KSClassDeclaration && it.classKind.type == "class") {
                    if (!it.validate()) ret.add(it)
                    else it.accept(BuffVisitor(environment), Unit)//处理符号
                }
            }
        //返回无法处理的符号
        return ret
    }
}