package com.lt.buff

/**
 * creator: lt  2022/10/22  lt.dygzs@qq.com
 * effect : 用于记录生成方法参数的数据
 * warning:
 */
internal class FunctionFieldsInfo(
    val fieldName: String,
    val isInTheConstructor: Boolean,
    val isBuffBean: Boolean,
)