package com.lt.buffapp.model

import com.lt.buff.Buff
import com.lt.buff.BuffScope
import com.lt.buff.BuffVariability
import com.lt.buffapp.BuffBean

/**
 * creator: lt  2025/12/26  lt.dygzs@qq.com
 * effect :
 * warning:
 */
@Buff(
    scope = BuffScope.All,
    variability = BuffVariability.Variable
)
class AllVar(
    val valConstructor: Int,
    var varConstructor: String,
    val valConstructorBuff: BuffBean,
    var varConstructorBuff: BuffBean,
    val valConstructorList: List<String>,
    var varConstructorList: List<String>,
    val valConstructorBuffList: List<BuffBean>,
    var varConstructorBuffList: List<BuffBean>,
    val valConstructorMutableList: MutableList<String>,
    var varConstructorMutableList: MutableList<String>,
    val valConstructorBuffMutableList: MutableList<BuffBean>,
    var varConstructorBuffMutableList: MutableList<BuffBean>,
) {
    val valClass: Int = 0
    var varClass: String = ""
    val valClassBuff: BuffBean? = null
    var varClassBuff: BuffBean? = null
    val valClassList: List<String>? = null
    var varClassList: List<String>? = null
    val valClassBuffList: List<BuffBean>? = null
    var varClassBuffList: List<BuffBean>? = null
    val valClassMutableList: MutableList<String>? = null
    var varClassMutableList: MutableList<String>? = null
    val valClassBuffMutableList: MutableList<BuffBean>? = null
    var varClassBuffMutableList: MutableList<BuffBean>? = null
}