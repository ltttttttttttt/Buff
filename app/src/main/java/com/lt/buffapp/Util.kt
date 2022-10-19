package com.lt.buffapp

import android.widget.Toast
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * kotlin json配置
 */
val kotlinJsonLibrary = Json {
    encodeDefaults = true//默认值也参与到序列化中(如果为false,则值等于默认值时json不会出现相应字段)
    ignoreUnknownKeys = true//key不严格模式
    isLenient = true//接受不兼容的json
    useAlternativeNames = false//如果不使用@JsonNames,设置为false可以提升性能,若使用必须为true
}

/**
 * 对象转换为json字符串
 */
inline fun <reified T : Any> T.toJson(): String = kotlinJsonLibrary.encodeToString(this)

/**
 * json转对象
 */
inline fun <reified T : Any> String.jsonToAny(): T = kotlinJsonLibrary.decodeFromString<T>(this)

/**
 * 弹toast
 */
fun String.showToast() = Toast.makeText(App.instance, this, Toast.LENGTH_SHORT).show()