import net.ltgt.gradle.errorprone.errorprone
import org.gradle.api.tasks.bundling.AbstractArchiveTask

plugins {
    id("net.fabricmc.fabric-loom") version "1.19.4"
    id("io.freefair.lombok") version "9.8.3"
    id("maven-publish")
    id("net.ltgt.errorprone") version "5.2.0"
    id("com.diffplug.spotless") version "8.12.0"
}

val minecraftVersion = "26.3"
val loaderVersion = "0.20.1"
val fabricApiVersion = "0.166.2+26.3"
val immutablesVersion = "2.12.3"
val commonsVersion = "1.9.2"
val lambdaEventsVersion = "2.4.2"
val reflectVersion = "1.6.4"
val asmVersion = "9.10.1"
val javaVersion = 25
val errorProneVersion = "2.53.0"
val palantirJavaFormatVersion = "2.103.0"
val mixinHandlerAnnotations = listOf("Inject", "Redirect", "ModifyVariable")
    .map { "org.spongepowered.asm.mixin.injection.$it" }
val mixinShadow = "org.spongepowered.asm.mixin.Shadow"

version = "0.3.0"
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

    errorprone("com.google.errorprone:error_prone_core:$errorProneVersion")

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

fabricApi {
    configureTests {
        createSourceSet = true
        modId = "anarchyclient-gametest"
        enableGameTests = false
        enableClientGameTests = true
    }
}

tasks.processResources {
    val modVersion = project.version.toString()
    inputs.property("version", modVersion)

    filesMatching("fabric.mod.json") {
        expand("version" to modVersion)
    }
}

tasks.withType<AbstractArchiveTask>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(javaVersion)
    options.errorprone {
        disableWarningsInGeneratedCode.set(true)
        // Mixin calls handler methods and fills @Shadow fields by reflection, and fixes the handler
        // signatures, so Error Prone would report them as unused.
        option("UnusedMethod:ExemptingMethodAnnotations", (mixinHandlerAnnotations + mixinShadow).joinToString(","))
        option("Unused:methodAnnotationsExemptingParameters", mixinHandlerAnnotations.joinToString(","))
    }
}

spotless {
    java {
        target("src/*/java/**/*.java")
        palantirJavaFormat(palantirJavaFormatVersion)
    }
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
