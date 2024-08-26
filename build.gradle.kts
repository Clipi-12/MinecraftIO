plugins {
    java
}

group = "me.clipi.schematic_reader"
version = "0.0.1"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("org.jetbrains:annotations:24.1.0")
    testCompileOnly("org.jetbrains:annotations:24.1.0")

    testImplementation("org.junit.jupiter:junit-jupiter:5.7.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(8))
}

tasks.compileJava {
    options.run {
        isFork = true

        compilerArgs.add("-Xlint:all,-processing")
        compilerArgs.add("-Werror")
        encoding = "UTF-8"
    }
}

tasks.compileTestJava {
    javaCompiler = javaToolchains.compilerFor {
        languageVersion = JavaLanguageVersion.of(22)
    }
}

tasks.test {
    useJUnitPlatform()
    failFast = false
    maxHeapSize = "512m"

    javaLauncher = javaToolchains.launcherFor {
        languageVersion = JavaLanguageVersion.of(22)
    }
}