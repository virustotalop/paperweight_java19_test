plugins {
    id("java")
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "com.gmail.virustotalop"
version = "1.0.0-SNAPSHOT"

subprojects {
    apply(plugin = "com.github.johnrengelman.shadow")
}