buildscript {
    repositories {
        mavenServers()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:7.4.0")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    }
}

allprojects {
    repositories {
        mavenServers()
    }
}