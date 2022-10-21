plugins {
    id("java-library")
    id("org.jetbrains.kotlin.jvm")
    id("maven-publish")
}

group = "com.github.ltttttttttttt"
version = "1.0.0"

java {
    withSourcesJar()
    withJavadocJar()
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

publishing {
    publications {
        create("maven_public", MavenPublication::class) {
            groupId = "com.github.ltttttttttttt"
            artifactId = "library"
            version = "1.0.0"
            from(components.getByName("java"))
        }
    }
}

dependencies {
    implementation("com.google.devtools.ksp:symbol-processing-api:$kspVersion")
}