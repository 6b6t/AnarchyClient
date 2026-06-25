import org.gradle.api.tasks.bundling.AbstractArchiveTask

plugins {
    id("net.fabricmc.fabric-loom") version "1.17.12"
    id("io.freefair.lombok") version "9.5.0"
    id("maven-publish")
}

val minecraftVersion = "26.2"
val loaderVersion = "0.19.3"
val fabricApiVersion = "0.153.0+26.2"
val immutablesVersion = "2.12.2"
val commonsVersion = "1.9.2"
val lambdaEventsVersion = "2.4.2"
val reflectVersion = "1.5.0"
val asmVersion = "9.10.1"
val javaVersion = 25

version = "0.2.0"
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
    implementation("net.lenni0451.commons:core:$commonsVersion")
    implementation("net.lenni0451.commons:animation:$commonsVersion")
    implementation("net.lenni0451:LambdaEvents:$lambdaEventsVersion")
    implementation("net.lenni0451:Reflect:$reflectVersion")
    implementation("org.ow2.asm:asm:$asmVersion")
    implementation("com.github.Lenni0451.rivet:core:40c6f6c3a7")
    include("com.github.Lenni0451.rivet:core:40c6f6c3a7")
    add("include", "net.lenni0451.commons:core:$commonsVersion")
    add("include", "net.lenni0451.commons:animation:$commonsVersion")
    add("include", "net.lenni0451:LambdaEvents:$lambdaEventsVersion")
    add("include", "net.lenni0451:Reflect:$reflectVersion")
    add("include", "org.ow2.asm:asm:$asmVersion")

    compileOnly("org.immutables:value-annotations:$immutablesVersion")
    annotationProcessor("org.immutables:value:$immutablesVersion")
    testCompileOnly("org.immutables:value-annotations:$immutablesVersion")
    testAnnotationProcessor("org.immutables:value:$immutablesVersion")

    compileOnly("com.google.code.findbugs:jsr305:3.0.2")
    compileOnly("org.jetbrains:annotations:26.1.0")

    testImplementation(platform("org.junit:junit-bom:6.1.0"))
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
