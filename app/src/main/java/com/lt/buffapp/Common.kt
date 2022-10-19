package com.lt.buffapp

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

/**
 * creator: lt  2022/10/19  lt.dygzs@qq.com
 * effect : 普通的方式处理bean to state
 * warning:
 */
@Composable
fun ColumnScope.Common() {
    val bean = remember {
        val b = """{"id":1,"name":"Common"}""".jsonToAny<CommonBean>()
        CommonStateBean(b.id, mutableStateOf(b.name))
    }
    Text(text = "普通方式,id=${bean.id}")
    TextField(value = bean.name.value ?: "", onValueChange = { bean.name.value = it })
    Button(onClick = {
        CommonBean(bean.id, bean.name.value).toJson().showToast()
    }) {
        Text(text = "show json")
    }
}

@kotlinx.serialization.Serializable
class CommonBean(
    val id: Int? = null,
    val name: String? = null,
)

class CommonStateBean(
    val id: Int? = null,
    val name: MutableState<String?>,
)