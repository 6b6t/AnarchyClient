plugins {
    id("net.fabricmc.fabric-loom") version "1.16.2"
    id("io.freefair.lombok") version "9.5.0"
    id("maven-publish")
}

val minecraftVersion = "26.1.2"
val loaderVersion = "0.19.2"
val fabricApiVersion = "0.148.2+26.1.2"
val immutablesVersion = "2.12.2"
val javaVersion = 25

version = "0.1.0-SNAPSHOT"
group = "net.blockhost"

base {
    archivesName.set("anarchyclient-mc-$minecraftVersion")
}

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraftVersion")
    implementation("net.fabricmc:fabric-loader:$loaderVersion")
    implementation("net.fabricmc.fabric-api:fabric-api:$fabricApiVersion")

    implementation("com.google.code.gson:gson:2.14.0")
    implementation("net.lenni0451.commons:core:1.9.2")
    implementation("net.lenni0451.commons:animation:1.9.2")
    implementation("com.github.Lenni0451.rivet:core:40c6f6c3a7")
    include("com.github.Lenni0451.rivet:core:40c6f6c3a7")
    add("include", "net.lenni0451.commons:core:1.9.2")
    add("include", "net.lenni0451.commons:animation:1.9.2")

    compileOnly("org.immutables:value-annotations:$immutablesVersion")
    annotationProcessor("org.immutables:value:$immutablesVersion")
    testCompileOnly("org.immutables:value-annotations:$immutablesVersion")
    testAnnotationProcessor("org.immutables:value:$immutablesVersion")

    compileOnly("com.google.code.findbugs:jsr305:3.0.2")
    compileOnly("org.jetbrains:annotations:26.1.0")

    testImplementation(platform("org.junit:junit-bom:6.1.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

loom {
    runConfigs.all {
        ideConfigGenerated(true)
        runDir = "run"
    }
}

tasks.processResources {
    inputs.property("version", project.version)

    filesMatching("fabric.mod.json") {
        expand("version" to project.version)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(javaVersion)
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(javaVersion))
    withSourcesJar()
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = base.archivesName.get()
            from(components["java"])
        }
    }
}
