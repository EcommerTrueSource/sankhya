import java.util.Properties

plugins {
    id("java")
}

group = "br.com.sankhya.truebrands"

val appVersion: String by project
val buildNumber: String by project
// Configura encoding para UTF-8 (Correto)
tasks.withType<JavaCompile> {
    options.encoding = "ISO-8859-1"
}

repositories {
    mavenCentral()

    // Reposit�rio local (flatDir)
    flatDir {
        dirs("./libs")
    }
}

dependencies {

    implementation(mapOf("name" to "mge-modelcore-4.34b156"))
    implementation(mapOf("name" to "jape-4.34b156"))
    implementation(mapOf("name" to "sanws"))
    implementation(mapOf("name" to "sanutil"))
    implementation(mapOf("name" to "SankhyaW-extensions"))
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.json:json:20231013")
    implementation("commons-io:commons-io:2.15.1")
    implementation("org.cuckoo:cuckoo-core:1.0.0")
}

tasks.test {
    useJUnitPlatform()
}

// Configura��o dos sourceSets (Correto)
sourceSets {
    main {
        java {
            srcDirs("Java/src")
        }
        resources {
            srcDirs("Java/resources")
        }
    }
}

tasks.clean {
    // O m�todo delete() do Groovy pode ser chamado diretamente no KTS
    delete("jbossgenerated")
    println("jbosgenerated removido")

    delete(file("dist"))
    println("dist removido")
}

tasks.register<Jar>("Gerar Jar Projeto Intelipost") {
    group = "0 - true"

    archiveBaseName.set("intelipost")
    val incrementVersion = getAndIncrementVersion("version-intelipost")
    archiveVersion.set(incrementVersion)
    destinationDirectory.set(file("dist/intelipost"))
    doFirst {
        destinationDirectory.get().asFile.mkdirs()
    }
    include("br/com/sankhya/true/utils/**")
    include("br/com/sankhya/true/intelipost/**")

    // Inclui apenas os arquivos compilados (.class e recursos)
    from(sourceSets.main.get().output)
}

fun getAndIncrementVersion(nome:String): String {
    val versionFile = File(rootProject.projectDir, nome)
    var current = versionFile.readText().trim()

    val parts = current.split(".").map { it.toInt() }.toMutableList()

    // incrementa apenas o patch
    parts[parts.size - 1] = parts.last() + 1

    val next = parts.joinToString(".")
    versionFile.writeText(next)

    return current
}

tasks.register("incrementaVersao") {
    doFirst {
        group = "0 - Tfd"
        description = "Incrementa a BUILD_NUMBER no gradle.properties"

        val properties = Properties()
        val versionFile = File("gradle.properties")

        // 2. Lendo o arquivo (usando use, que garante que o stream ser� fechado)
        versionFile.inputStream().use { properties.load(it) }

        // 3. Pegando e processando a vers�o antiga
        val oldVersion = properties.getProperty("version") ?: "0.0.0"

        // Divide a string por '.' e converte para uma lista de inteiros
        val parts = oldVersion.split('.').map { it.toIntOrNull() ?: 0 }

        // Certifique-se de que h� pelo menos 3 partes (Major, Minor, Patch)
        if (parts.size < 3) {
            throw IllegalArgumentException("Formato de vers�o inv�lido: $oldVersion. Esperado: X.Y.Z")
        }

        // 4. Incrementando o patch (o �ltimo elemento)
        val major = parts[0]
        val minor = parts[1]
        val newPatch = parts[2] + 1

        val newVersion = "$major.$minor.$newPatch"

        // 5. Reescrevendo o arquivo
        properties.setProperty("version", newVersion)
        versionFile.outputStream().use { properties.store(it, null) }
    }




}
