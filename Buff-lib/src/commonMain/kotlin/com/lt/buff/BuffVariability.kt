package com.lt.buff

/**
 * creator: lt  2025/12/24  lt.dygzs@qq.com
 * effect : 要处理哪些可变性的属性
 * warning:
 */
enum class BuffVariability {
    /**
     * val(构造函数中) + var + List + MutableList
     */
    All,

    /**
     * var + MutableList
     */
    Variable
}