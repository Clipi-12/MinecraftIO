plugins {
    java
}

group = "me.clipi.mc"
version = "0.0.1"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("org.jetbrains:annotations:24.1.0")
    testCompileOnly("org.jetbrains:annotations:24.1.0")

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(22)
}

tasks.compileJava {
    options.run {
        isFork = true
        release = 8

        compilerArgs.add("-Xlint:all,-processing,-options")
        compilerArgs.add("-Werror")
        encoding = "UTF-8"
    }
}

tasks.compileTestJava {
    options.run {
        compilerArgs.add("-Xlint:all,-processing")
        compilerArgs.add("-Werror")
        encoding = "UTF-8"
    }
}

tasks.test {
    useJUnitPlatform()
    failFast = false
    maxHeapSize = "512m"
}