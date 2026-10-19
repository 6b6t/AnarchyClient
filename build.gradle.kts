import org.gradle.api.tasks.bundling.AbstractArchiveTask

plugins {
    id("net.fabricmc.fabric-loom") version "1.18.4"
    id("io.freefair.lombok") version "9.8.2"
    id("maven-publish")
}

val minecraftVersion = "26.2"
val loaderVersion = "0.20.1"
val fabricApiVersion = "0.164.0+26.4"
val immutablesVersion = "2.12.3"
val commonsVersion = "1.9.2"
val lambdaEventsVersion = "2.4.2"
val reflectVersion = "1.6.4"
val asmVersion = "9.10.1"
val javaVersion = 25

version = "0.2.2"
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
    implementation(include("net.fabricmc.fabric-api:fabric-api:$fabricApiVersion") as Any)

    implementation(include("com.github.Lenni0451.rivet:core:8dfcc3dd17") as Any)
    implementation(include("net.lenni0451.commons:core:$commonsVersion") as Any)
    implementation(include("net.lenni0451.commons:animation:$commonsVersion") as Any)
    implementation(include("net.lenni0451:LambdaEvents:$lambdaEventsVersion") as Any)
    implementation(include("net.lenni0451:Reflect:$reflectVersion") as Any)

    annotationProcessor("org.immutables:gson:$immutablesVersion")
    compileOnly("org.immutables:value:${immutablesVersion}")
    annotationProcessor("org.immutables:value:$immutablesVersion")

    compileOnly("com.google.code.findbugs:jsr305:3.0.2")
    compileOnly("org.jetbrains:annotations:26.1.0")

    testImplementation(platform("org.junit:junit-bom:6.1.3"))
    testImplementation("net.fabricmc:fabric-loader-junit:$loaderVersion")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

loom {
    accessWidenerPath.set(file("src/main/resources/anarchyclient.accesswidener"))

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

tasks.withType<AbstractArchiveTask>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
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
