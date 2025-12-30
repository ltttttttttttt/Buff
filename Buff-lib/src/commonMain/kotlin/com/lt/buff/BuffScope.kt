package com.lt.buff

/**
 * creator: lt  2025/12/24  lt.dygzs@qq.com
 * effect : 范围,要给bean的哪些属性增加buff
 * warning:
 */
enum class BuffScope(
    //包含构造函数属性
    val hasConstructorProperties: Boolean,
    //包含类内属性
    val hasClassProperties: Boolean
) {
    /**
     * 构造函数属性 + 类内属性
     */
    All(true, true),

    /**
     * 构造函数属性
     */
    ConstructorProperties(true, false),

    /**
     * 类内属性
     */
    ClassProperties(false, true),
}