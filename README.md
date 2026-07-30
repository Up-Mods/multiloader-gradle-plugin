# Multiloader Gradle Plugin [![Gradle Plugin Portal Version](https://img.shields.io/gradle-plugin-portal/v/dev.upcraft.gradle.multiloader?style=flat-square&label=Latest%20Version)](https://plugins.gradle.org/plugin/dev.upcraft.gradle.multiloader "Gradle Plugin Portal")

A Gradle plugin to facilitate multiloader Minecraft mod development

### Usage

```kts
plugins {
    id("dev.upcraft.gradle.multiloader")
}

multiLoader {
    javaVersion = 25 // make all compile tasks use Java 25 JDK
    minecraftVersion = "26.1.2" // add Minecraft 26.1.2 dependency via MDG/Loom
    
    loader = "fabric" // tell the plugin which loader subproject this is, valid values are 'common', 'fabric', 'neoforge'

    // generate a testmod source set and make it depend on the main mod
    withTestmod()

    // apply standard variable replacements during processResources tasks
    applyMetadataReplacements(listOf("pack.mcmeta", "*.mixins.json"))
}
```

<details>
    <summary>Full usage example (click to expand)</summary>

#### `settings.gradle.kts`:
```kts
plugins {
    // for latest version see https://plugins.gradle.org/plugin/dev.upcraft.gradle.multiloader
    id("dev.upcraft.gradle.multiloader.settings") version "+"
}
```

#### `common/build.gradle.kts`:
```kts
plugins {
    id("dev.upcraft.gradle.multiloader")
}

neoForge.neoFormVersion = libs.versions.neoform.get()

multiLoader {
    javaVersion = 25
    minecraftVersion = "26.1.2"
    
    applyMetadataReplacements(listOf("pack.mcmeta", "*.mixins.json"))
}
```

#### `fabric/build.gradle.kts`:
```kts
plugins {
    id("dev.upcraft.gradle.multiloader")
    id("net.fabricmc.fabric-loom")
}

multiLoader {
    javaVersion = 25
    minecraftVersion = "26.1.2"
    
    loader = "fabric"
    
    applyMetadataReplacements(listOf("pack.mcmeta", "*.mixins.json", "fabric.mod.json"), mapOf(
        "fabric_api_version" to libs.versions.fabric.api,
        "fabric_loader_version" to libs.versions.fabric.loader
    ))
}
```

#### `neoforge/build.gradke.kts`:
```kts
plugins {
    id("dev.upcraft.gradle.multiloader")
    id("net.neoforged.moddev")
}

neoForge.version = libs.versions.neoforge.get()

multiLoader {
    javaVersion = 25
    minecraftVersion = "26.1.2"
    
    loader = "neoforge"
    
    applyMetadataReplacements(listOf("pack.mcmeta", "*.mixins.json", "META-INF/neoforge.mods.toml"), mapOf(
        "neoforge_version" to libs.versions.neoforge
    ))
}
```

</details>
