plugins {
    id("java")
    alias(libs.plugins.shadow)
    alias(libs.plugins.loom)
    alias(libs.plugins.resourcefactory)
}

fun Project.git(command: String): String? {
    return try {
        providers.exec { commandLine(("git $command").split(" ")) }
            .standardOutput
            .asText
            .get()
            .trim()
    } catch (_: Exception) {
        null
    }
}

group = "codes.reason"
version = "1.0.0"
description = "A shamelessly vibecoded utility mod for Murder Game"

dependencies {
    minecraft(libs.minecraft)
    mappings(loom.officialMojangMappings())
    modImplementation(libs.fabric.api)
    modImplementation(libs.fabric.loader)
    implementation(libs.serialization.json)
}

tasks.shadowJar {
    mergeServiceFiles()
    minimize()
}

fabricModJson {
    id = "murdertils"
//    icon("assets/murdertils/icon.png")
    clientEntrypoint("codes.reason.murdertils.Murdertils")
    mixin("murdertils.mixins.json")
    depends("minecraft", libs.versions.minecraft.get())

    author("Reasonless") {
        contact.sources = "https://github.com/Reasonless/"
    }
}
