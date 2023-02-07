package com.lt.buffapp

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.lt.buff.Buff
import com.lt.buffapp.ui.theme.Type

/**
 * creator: lt  2022/10/19  lt.dygzs@qq.com
 * effect : 使用Buff的方式处理bean to state
 * warning:
 */
@Composable
fun ColumnScope.UseBuff() {
    val bean = remember {
        """{"id":1,"name":"UseBuff","info":{"nick":"init"}}""".jsonToAny<BuffBean>().addBuff()
    }
    Text(text = "Buff方式,id=${bean.id},nick=${bean.info?.nick}")
    TextField(value = bean.name ?: "", onValueChange = {
        bean.name = it
        bean.info?.nick = it
    })
    Button(onClick = {
        bean.toJson().showToast()
    }) {
        Text(text = "show json")
    }
}

@kotlinx.serialization.Serializable
@Buff
class BuffBean(
    val id: Int? = null,
    var info2: InfoBean? = null,
    var infoList2: List<InfoBean>? = null,
) {
    var name: String? = null
    var info: InfoBean? = null
    var type: Type? = null
    var infoList: List<InfoBean>? = null
    var list: List<String>? = null
    var infoListList: List<List<InfoBean>>? = null
    var map: Map<String, InfoBean>? = null
}

@kotlinx.serialization.Serializable
@Buff
class InfoBean(
    val age: Int? = null,
) {
    var nick: String? = null
}