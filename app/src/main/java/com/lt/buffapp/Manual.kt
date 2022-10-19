package com.lt.buffapp

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

/**
 * creator: lt  2022/10/19  lt.dygzs@qq.com
 * effect : 手动的方式处理bean to state
 * warning:
 */
@Composable
fun ColumnScope.Manual() {
    val bean = remember {
        """{"id":1,"name":"Manual"}""".jsonToAny<ManualBean>()
    }
    Text(text = "手动方式,id=${bean.id}")
    TextField(value = bean.name ?: "", onValueChange = { bean.name = it })
    Button(onClick = {
        bean.toJson().showToast()
    }) {
        Text(text = "show json")
    }
}

@kotlinx.serialization.Serializable
data class ManualBean(
    val id: Int? = null,
) {
    @kotlinx.serialization.Transient
    val nameState = mutableStateOf<String?>(null)
    var name: String? = null
        get() {
            nameState.value = field
            return nameState.value
        }
        set(value) {
            field = value
            nameState.value = value
        }
}