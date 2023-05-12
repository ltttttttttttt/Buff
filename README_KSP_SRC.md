* If it is multi-platform, add it to build.gradle.kts in the common module directory

```kotlin
val commonMain by getting {
    //Configure the ksp generation directory
    kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
}
```

* If single platform

Your app dir, build.gradle.kts add:

```kotlin
//If your project is the android, and the productFlavors is not set
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

//If your project is the android, and the productFlavors is set
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

//If your project is the jvm or more
kotlin {
    sourceSets.main {
        kotlin.srcDir("build/generated/ksp/main/kotlin")
    }
    sourceSets.test {
        kotlin.srcDir("build/generated/ksp/test/kotlin")
    }
}
```