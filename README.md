<h1 align="center">Buff</h1>

<p align="center">Add status to beans in Jetpack(jb) Compose, Fields in beans can be directly used as the MutableState&lt;T&gt;</p>

<p>⚠️Non mainstream warning:This item violates 'Unique trusted data source' and 'FP',If the content on this page causes your discomfort,please press Ctrl+W</p>

<p align="center">
<img src="https://img.shields.io/badge/Kotlin-Multiplatform-%237f52ff?logo=kotlin">
<img src="https://img.shields.io/badge/license-Apache%202-blue.svg?maxAge=2592000">
<img src="https://img.shields.io/maven-central/v/io.github.ltttttttttttt/Buff"/>
</p>

<div align="center">us English | <a href="https://github.com/ltttttttttttt/Buff/blob/main/README_CN.md">cn 简体中文</a></div>

## ability

1. Convert some fields in beans to MutableState&lt;T&gt; that can be used directly

## Capability to be achieved

<a href="https://github.com/ltttttttttttt/Buff/blob/main/README_CN.md">see this</a>

## How to use

Step 1 and 2.add dependencies:

version
= [![](https://img.shields.io/maven-central/v/io.github.ltttttttttttt/Buff)](https://repo1.maven.org/maven2/io/github/ltttttttttttt/Buff/)

* If it is a single platform, add it to build.gradle.kts in the app module directory

```kotlin
plugins {
    ...
    id("com.google.devtools.ksp") version "1.8.20-1.0.10"//this,The left 1.8.20 corresponds to your the Kotlin version,more version: https://github.com/google/ksp/releases
}

dependencies {
    ...
    implementation("io.github.ltttttttttttt:Buff-lib:$version")//this,such as 0.1.2
    ksp("io.github.ltttttttttttt:Buff:$version")//this,such as 0.1.2
}
```

* If it is multi-platform, add it to build.gradle.kts in the common module directory

```kotlin
plugins {
    ...
    id("com.google.devtools.ksp") version "1.8.20-1.0.10"
}

...
val commonMain by getting {
    dependencies {
        ...
        api("io.github.ltttttttttttt:Buff-lib:$version")//this,such as 0.1.2
    }
}

...
dependencies {
    add("kspCommonMainMetadata", "io.github.ltttttttttttt:Buff:$version")
}
```

Step 3.Use Buff

Add the @Buff to your bean, call the addBuff() transform to the new Any, The attribute (such as
name) not in the constructor will be automatically converted to MutableState&lt;T&gt;

```kotlin
@Buff
class BuffBean(
    val id: Int? = null,
) {
    var name: String? = null
}
```

Example(reference UseBuff.kt):

```kotlin
val buffBean = BuffBean(0)
val bean = buffBean.addBuff()//Transform to the BuffBeanWithBuff
bean.name//The name's getter and setter have the effect of MutableState<T>
bean.removeBuff()//Fallback to BuffBean(optional)
```

Step 4.If you are using a version of ksp less than 1.0.9, the following configuration is required:

<a href="https://github.com/ltttttttttttt/Buff/blob/main/README_KSP_SRC.md">Ksp configuration</a>

Step 5.Optional configuration

Serialize of this project uses kotlinx-serialization by default, If using other serialization support, modify it, Your app dir,
build.gradle.kts add:

```kotlin
ksp {
    //Set the Annotation of the class, Usually as follows
    arg("classSerializeAnnotationWithBuff", "//Not have")
    //Set the Annotation of the field to transient, Usually as follows
    arg("fieldSerializeTransientAnnotationWithBuff", "@kotlin.jvm.Transient")
}
```

Add custom code, reference [KspOptions.handlerCustomCode], Your app dir, build.gradle.kts add:

```kotlin
ksp {
    arg("customInClassWithBuff", "//Class end")//in class
    arg("customInFileWithBuff", "//File end")//in file
}
```
