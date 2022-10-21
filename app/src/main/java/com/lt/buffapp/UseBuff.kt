package com.lt.buffapp

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.lt.buff.Buff

/**
 * creator: lt  2022/10/19  lt.dygzs@qq.com
 * effect : 使用Buff的方式处理bean to state
 * warning:
 */
@Composable
fun ColumnScope.UseBuff() {
    val bean = remember {
        """{"id":1,"name":"UseBuff"}""".jsonToAny<BuffBean>().addBuff()
    }
    Text(text = "Buff方式,id=${bean.id}")
    TextField(value = bean.name ?: "", onValueChange = { bean.name = it })
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
) {
    var name: String? = null
}