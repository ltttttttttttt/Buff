* 如果是多平台,在common模块目录内的build.gradle.kts内添加

```kotlin
val commonMain by getting {
    //配置ksp生成目录
    kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
}
```

* 如果是单平台

在app模块目录内的build.gradle.kts内添加:

```kotlin
//如果你的是安卓项目,且未设置多渠道
android {
    buildTypes {
        release {
            kotlin {
                sourceSets.main {
                    kotlin.srcDir("build/generated/ksp/release/kotlin")
                }
            }
        }
        debug {
            kotlin {
                sourceSets.main {
                    kotlin.srcDir("build/generated/ksp/debug/kotlin")
                }
            }
        }
    }
    kotlin {
        sourceSets.test {
            kotlin.srcDir("build/generated/ksp/test/kotlin")
        }
    }
}

//如果你的是安卓项目,且设置了多渠道
applicationVariants.all {
    outputs.all {
        val flavorAndBuildTypeName = name
        kotlin {
            sourceSets.main {
                kotlin.srcDir(
                    "build/generated/ksp/${
                        flavorAndBuildTypeName.split("-").let {
                            it.first() + it.last()[0].toUpperCase() + it.last().substring(1)
                        }
                    }/kotlin"
                )
            }
        }
    }
}
kotlin {
    sourceSets.test {
        kotlin.srcDir("build/generated/ksp/test/kotlin")
    }
}

//如果你的是jvm等项目
kotlin {
    sourceSets.main {
        kotlin.srcDir("build/generated/ksp/main/kotlin")
    }
    sourceSets.test {
        kotlin.srcDir("build/generated/ksp/test/kotlin")
    }
}
```