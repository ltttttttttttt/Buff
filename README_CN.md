<h1 align="center">Buff</h1>

<p align="center">将beans中的某些字段转换为可以直接使用的MutableState&lt;T&gt;,适用于Jetpack(jb) Compose</p>

<p align="center">⚠️非主流警告:若此页面所述内容引起了您的不适,请按下Ctrl+W</p>

<p align="center">
<img src="https://img.shields.io/badge/Kotlin-Multiplatform-%237f52ff?logo=kotlin">
<img src="https://img.shields.io/badge/license-Apache%202-blue.svg?maxAge=2592000">
<img src="https://img.shields.io/maven-central/v/io.github.ltttttttttttt/Buff"/>
</p>

<div align="center"><a href="https://github.com/ltttttttttttt/Buff/blob/main/README.md">us English</a> | cn 简体中文</div>

## 目前功能

1. 将beans中的某些字段转换为可以直接使用的MutableState&lt;T&gt;

## 使用方式

Step 1和2.添加依赖:

version
= [![](https://img.shields.io/maven-central/v/io.github.ltttttttttttt/Buff)](https://repo1.maven.org/maven2/io/github/ltttttttttttt/Buff/)

* 如果是单平台,在app模块目录内的build.gradle.kts内添加

```kotlin
plugins {
    ...
    id("com.google.devtools.ksp") version "1.8.20-1.0.10"//this,前面的1.8.20对应你的kotlin版本,更多版本参考: https://github.com/google/ksp/releases
}

dependencies {
    ...
    implementation("io.github.ltttttttttttt:Buff-lib:$version")//this,比如1.0.1
    ksp("io.github.ltttttttttttt:Buff:$version")//this,比如1.0.1
}
```

* 如果是多平台,在common模块目录内的build.gradle.kts内添加

```kotlin
plugins {
    ...
    id("com.google.devtools.ksp") version "1.8.20-1.0.10"//this,前面的1.8.20对应你的kotlin版本,更多版本参考: https://github.com/google/ksp/releases
}

...
val commonMain by getting {
    dependencies {
        ...
        api("io.github.ltttttttttttt:Buff-lib:$version")//this,比如1.0.1
    }
}

...
dependencies {
    add("kspCommonMainMetadata", "io.github.ltttttttttttt:Buff:$version")
}
```

Step 3.使用Buff注解

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

Step 4.如果你使用的ksp版本小于1.0.9则需要以下配置:

<a href="https://github.com/ltttttttttttt/Buff/blob/main/README_KSP_SRC_CN.md">ksp配置</a>

Step 5.可选配置

本项目对序列化的默认支持为:kotlinx-serialization,如使用其他序列化支持,则修改,在app模块目录内的build.gradle.kts内添加:

```kotlin
ksp {
    //设置类序列化所需要的注解,其他序列化库一般不需要,所以我们放一个注释即可
    arg("classSerializeAnnotationWithBuff", "//Not have")
    //设置属性无需被序列化的注解,一般使用jvm中的transient关键字
    arg("fieldSerializeTransientAnnotationWithBuff", "@kotlin.jvm.Transient")
}
```

支持自定义增加代码,属性参考[KspOptions.handlerCustomCode],在app模块目录内的build.gradle.kts内添加:

```kotlin
ksp {
    arg("customInClassWithBuff", "//Class end")//类内
    arg("customInFileWithBuff", "//File end")//类外,kt文件内
}
```

项目提供了对Compose可变性注解的支持,如果原Bean带有 @Stable 或 @Immutable 注解,则生成的Buff类也带有相应注解