<h1 align="center">Buff</h1>

<p align="center">Add status to beans in Compose Multiplatform, Fields in beans can be directly used as the MutableState&lt;T&gt;</p>

<p align="center">
<img src="https://img.shields.io/badge/Kotlin-Multiplatform-%237f52ff?logo=kotlin">
<img src="https://img.shields.io/badge/license-Apache%202-blue.svg?maxAge=2592000">
<img src="https://img.shields.io/maven-central/v/io.github.ltttttttttttt/Buff"/>
</p>

<div align="center">us English | <a href="https://github.com/ltttttttttttt/Buff/blob/main/README_CN.md">cn 简体中文</a></div>

## ability

1. Convert some fields in beans to MutableState&lt;T&gt; that can be used directly

## How to use

Step 1 and 2.add dependencies:

version
= [![](https://img.shields.io/maven-central/v/io.github.ltttttttttttt/Buff)](https://repo1.maven.org/maven2/io/github/ltttttttttttt/Buff/)

* If it is a single platform, add it to build.gradle.kts in the app module directory

```kotlin
plugins {
    ...
    id("com.google.devtools.ksp") version "2.1.21-2.0.1"//this,The left 2.1.21 corresponds to your the Kotlin version,more version: https://github.com/google/ksp/releases
}

dependencies {
    ...
    implementation("io.github.ltttttttttttt:Buff-lib:$version")//this,such as 2.0.1
    ksp("io.github.ltttttttttttt:Buff:$version")//this,such as 2.0.1
}
```

* If it is multi-platform, add it to build.gradle.kts in the common module directory

```kotlin
plugins {
    ...
    id("com.google.devtools.ksp") version "2.1.21-2.0.1"
}

...
val commonMain by getting {
    dependencies {
        ...
        api("io.github.ltttttttttttt:Buff-lib:$version")//this,such as 2.0.1
    }
}

...
dependencies {
    add("kspCommonMainMetadata", "io.github.ltttttttttttt:Buff:$version")
}
```

Step 3.Use Buff

Add the @Buff to your bean, call the addBuff() transform to the new Any, The attribute (such as
name) will be automatically converted to MutableState&lt;T&gt;

```kotlin
@Buff
class BuffBean(
    val id: Int? = null,
) {
    var isSelect: Boolean = false
}
```

Example(reference UseBuff.kt):

```kotlin
val buffBean = BuffBean(0)
val bean = buffBean.addBuff()//Transform to the BuffBeanWithBuff
bean.isSelect = true//The isSelect's getter and setter have the effect of MutableState<T>
bean.removeBuff()//Fallback to BuffBean(optional)
```

Step 4.If you are using a version of ksp less than 1.0.9, the following configuration is required:

<a href="https://github.com/ltttttttttttt/Buff/blob/main/README_KSP_SRC.md">Ksp configuration</a>

Step 5.Optional configuration

```kotlin
@Buff(
    scope,//which attributes of the bean should be buffed
    variability//What mutable properties to deal with
)
```

Add custom code, reference [KspOptions.handlerCustomCode], Your app dir, build.gradle.kts add:

```kotlin
ksp {
    arg("customInClassWithBuff", "//Class end")//in class
    arg("customInFileWithBuff", "//File end")//in file
}
```

The project provides support for Compose variability annotations. If the original bean has @Stable or @Immutable annotations, the generated Buff class also has corresponding annotations