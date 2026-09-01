plugins {
    // litertlm-jvm 0.17.0-alpha1 vem compilado com metadata Kotlin 2.4.x,
    // entao o plugin Kotlin precisa ser >= 2.4 para conseguir ler as classes.
    kotlin("jvm") version "2.4.10"
    application
}

group = "com.example"
version = "1.0.0"

repositories {
    // Necessario: as bibliotecas litertlm-jvm sao publicadas no Google Maven.
    google()
    mavenCentral()
}

dependencies {
    // Build para JVM (Linux, macOS, Windows). Versao fixada: toda a configuracao
    // abaixo (Kotlin 2.4, JDK 21, coroutines 1.11.0) foi ajustada para ela.
    // Versoes mais novas em:
    // https://maven.google.com/web/index.html#com.google.ai.edge.litertlm:litertlm-jvm
    implementation("com.google.ai.edge.litertlm:litertlm-jvm:0.17.0-alpha1")

    // litertlm-jvm 0.17.0-alpha1 declara coroutines 1.9.0 no POM, mas o bytecode
    // chama SendChannel.close$default como metodo estatico DE INTERFACE, que so
    // passou a existir no coroutines 1.11.0 (em 1.9.0/1.10.2 fica em
    // SendChannel$DefaultImpls). Sem isso: NoSuchMethodError ao terminar a resposta.
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
}

application {
    mainClass.set("MainKt")
}

kotlin {
    // litertlm-jvm 0.17.0-alpha1 e compilado para Java 21 (class file v65),
    // entao o runtime precisa ser JDK 21+.
    jvmToolchain(21)
}

// Permite ler o teclado (System.in) quando rodado via "./gradlew run"
tasks.withType<JavaExec> {
    standardInput = System.`in`
}
