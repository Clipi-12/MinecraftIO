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
