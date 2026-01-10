plugins {
    id("java-library")
    id("java")
    id("maven-publish")
    id("io.papermc.paperweight.userdev") version "1.7.1" apply false
    id("io.ktor.plugin") version "3.1.0"
}

group = "fr.mrbaguette07"
version = "1.12.0.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://ci.ender.zone/plugin/repository/everything/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://jitpack.io")
    maven {
        url = uri("https://repo.opencollab.dev/main/")
    }
    maven("https://maven.enginehub.org/repo/")
    maven("https://repo.bluecolored.de/releases/")
}

dependencies {
    // API Minecraft - Compatible 1.21.4
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    compileOnly("org.geysermc.floodgate:api:2.2.3-SNAPSHOT")
    compileOnly("dev.folia:folia-api:1.20.4-R0.1-SNAPSHOT")
    
    // Base de données - HikariCP pour MySQL/SQLite
    implementation("com.zaxxer:HikariCP:5.1.0")
    
    // MongoDB
    implementation("org.mongodb:mongodb-driver-sync:5.1.0")
    
    // Redis (Jedis)
    implementation("redis.clients:jedis:5.1.0")
    
    // API Velocity
    compileOnly("com.velocitypowered:velocity-api:3.3.0-SNAPSHOT")
    annotationProcessor("com.velocitypowered:velocity-api:3.3.0-SNAPSHOT")
    
    // Dépendances des plugins
    compileOnly(files("libs/PlaceholderAPI-2.11.6.jar"))
    compileOnly("com.github.LoneDev6:api-itemsadder:3.6.1")
    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.4") {
        exclude(group = "org.bstats", module = "bstats-bukkit")
        exclude(group = "org.spigotmc", module = "spigot-api")
        exclude(group = "org.bukkit", module = "bukkit")
    }
    compileOnly("com.github.MilkBowl:VaultAPI:1.7") {
        exclude(group = "org.bukkit", module = "bukkit")
    }
    compileOnly("de.bluecolored.bluemap:BlueMapAPI:2.7.2")
    compileOnly("net.md-5:bungeecord-chat:1.16-R0.4")
    
    // Utilitaires
    implementation("com.mojang:authlib:1.5.21")
    // Use compileOnly for flow-math to avoid conflicts with BlueMap
    compileOnly("com.flowpowered:flow-math:1.0.3")
    implementation("com.google.code.gson:gson:2.10.1")
    
    // Tests
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.7.1")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.7.1")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.jar {
    archiveBaseName.set("slclaim")
    archiveVersion.set(version.toString())
    
    manifest {
        attributes["Main-Class"] = "fr.mrbaguette07.SLclaim.SLclaim"
    }
    
    // Inclusion des dépendances
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) }) {
        exclude("META-INF/*.SF")
        exclude("META-INF/*.DSA")
        exclude("META-INF/*.RSA")
        // Exclude flowpowered to avoid conflicts with BlueMap
        exclude("com/flowpowered/**")
    }
    
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

application {
    mainClass.set("fr.mrbaguette07.SLclaim.SLclaim")
}

ktor {
    fatJar {
        archiveFileName.set("slclaim.jar")
    }
}
