package com.lt.buff.options

/**
 * creator: lt  2022/10/22  lt.dygzs@qq.com
 * effect : 用于记录生成方法参数的数据
 * warning:
 */
internal class FunctionFieldsInfo(
    val fieldName: String,//属性名
    val isInTheConstructor: Boolean,//是否位于构造中
    val isVal: Boolean,//是否是val
    val typeInfo: BuffKSTypeInfo,//type信息
    var isToState: Boolean = false,//属性是否需要转为state
) {
    /**
     * 获取addBuff或removeBuff的后缀,用于区分type是否可空
     */
    fun getBuffSuffix(): String =
        if (typeInfo.isList && typeInfo.typeString.endsWith("?>")) "WithNull" else ""

    /**
     * 获取私有状态的名字
     */
    fun getStateFieldName(): String = "_${fieldName}_state"
}