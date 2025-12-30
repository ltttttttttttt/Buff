package com.lt.buff

/**
 * creator: lt  2022/10/20  lt.dygzs@qq.com
 * effect : 表示给bean加buff
 * warning:
 * [scope]范围,要给bean的哪些属性增加buff
 * [variability]要处理哪些可变性的属性
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Buff(
    val scope: BuffScope = BuffScope.All,
    val variability: BuffVariability = BuffVariability.All
)