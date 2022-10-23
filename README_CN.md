<h1 align="center">Buff</h1>

<p align="center">将beans中的某些字段转换为可以直接使用的MutableState&lt;T&gt;,适用于Jetpack(jb) Compose</p>

<p>⚠️非主流警告:此项目违背了'唯一可信数据源'和'函数式编程思想',若此页面所述内容引起了您的不适,请按下Ctrl+W</p>

<p align="center">
<img src="https://img.shields.io/badge/license-Apache%202-blue.svg?maxAge=2592000">
<img src="https://jitpack.io/v/ltttttttttttt/Buff.svg"/>
</p>

<div align="center"><a href="https://github.com/ltttttttttttt/Buff/blob/main/README.md">us English</a> | cn 简体中文</div>

## 目前功能

1. 将beans中的某些字段转换为可以直接使用的MutableState&lt;T&gt;

## 待实现功能

1. 适用于多平台
2. 对List的支持(可能)
3. 对嵌套类和嵌套List的支持(可能)
4. 对其他json解析框架的支持
5. 支持自定义增加代码
6. (可能需要支持外部包)

## 使用方式

Step 1.在项目的根目录的build.gradle.kts内添加:

```kotlin
buildscript {
    repositories {
        maven("https://jitpack.io")//this
        ...
    }
}

allprojects {
    repositories {
        maven("https://jitpack.io")//this
        ...
    }
}
```

Step 2.在app模块目录内的build.gradle.kts内添加:

version = [![](https://jitpack.io/v/ltttttttttttt/Buff.svg)](https://jitpack.io/#ltttttttttttt/Buff)

```kotlin
plugins {
    ...
    id("com.google.devtools.ksp") version "1.7.10-1.0.6"//this,前面的1.7.10对应你的kotlin版本,更多版本参考: https://github.com/google/ksp/releases
}

buildTypes {
    release {
        ...
        kotlin {
            sourceSets.main {
                kotlin.srcDir("build/generated/ksp/release/kotlin")//this todo 后续修改为自动获取
            }
        }
    }
    debug {
        ...
        kotlin {
            sourceSets.main {
                kotlin.srcDir("build/generated/ksp/debug/kotlin")//this
            }
        }
    }
}

dependencies {
    ...
    implementation("com.github.ltttttttttttt:Buff:$version")//this,比如0.0.2
    ksp("com.github.ltttttttttttt:Buff:$version")//this,比如0.0.2
}
```

Step 3.使用BUff注解

给你的bean类加上@Buff注解,然后调用类的addBuff方法转换为新类,会将其中非构造中的属性(如name)自动转换为MutableState&lt;T&gt;

```kotlin
@Buff
class BuffBean(
    val id: Int? = null,
) {
    var name: String? = null
}
```

代码示例如下(具体可以参考项目中UseBuff.kt文件):

```kotlin
val buffBean = BuffBean(0)//这个BuffBean可以自己new出来,也可以通过序列化等方式
val bean = buffBean.addBuff()//增加Buff,类型改为BuffBeanWithBuff
bean.name//这个name的get和set就有了MutableState<T>的效果
bean.removeBuff()//退回为BuffBean(可选方法,可以不使用)
```