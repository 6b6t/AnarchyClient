import com.github.spotbugs.snom.Confidence
import com.github.spotbugs.snom.Effort
import com.github.spotbugs.snom.SpotBugsTask
import net.fabricmc.loom.task.RunGameTask
import net.ltgt.gradle.errorprone.errorprone
import org.gradle.api.tasks.bundling.AbstractArchiveTask

plugins {
    id("net.fabricmc.fabric-loom") version "1.19.4"
    id("io.freefair.lombok") version "9.8.3"
    id("maven-publish")
    id("jacoco")
    id("net.ltgt.errorprone") version "5.2.0"
    id("com.diffplug.spotless") version "8.12.0"
    id("com.github.spotbugs") version "6.5.13"
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
val nullAwayVersion = "0.14.4"
val palantirJavaFormatVersion = "2.103.0"
val spotbugsVersion = "4.10.5"
val sbContribVersion = "7.7.4"
val findSecBugsVersion = "1.14.0"
val archUnitVersion = "1.5.1"
val jacocoVersion = "0.8.16"
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
    errorprone("com.uber.nullaway:nullaway:$nullAwayVersion")
    spotbugsPlugins("com.mebigfatguy.sb-contrib:sb-contrib:$sbContribVersion")
    spotbugsPlugins("com.h3xstream.findsecbugs:findsecbugs-plugin:$findSecBugsVersion")

    compileOnly("com.google.code.findbugs:jsr305:3.0.2")
    compileOnly("org.jetbrains:annotations:26.1.0")

    testImplementation(platform("org.junit:junit-bom:6.1.3"))
    testImplementation("net.fabricmc:fabric-loader-junit:$loaderVersion")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("com.tngtech.archunit:archunit:$archUnitVersion")
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
    // Error Prone and javac warnings fail the build, so the count stays at zero.
    options.compilerArgs.add("-Werror")
    options.errorprone {
        disableWarningsInGeneratedCode.set(true)
        // Mixin calls handler methods and fills @Shadow fields by reflection, and fixes the handler
        // signatures, so Error Prone would report them as unused.
        option("UnusedMethod:ExemptingMethodAnnotations", (mixinHandlerAnnotations + mixinShadow).joinToString(","))
        option("Unused:methodAnnotationsExemptingParameters", mixinHandlerAnnotations.joinToString(","))
        // Minecraft compares entities, levels, menus, and registry entries such as blocks and items by
        // identity, so == is the intended check almost everywhere it appears.
        disable("ReferenceEquality")
        // NullAway checks every package marked @NullMarked in its package-info.java; @Nullable (JSpecify) marks
        // the values that can be null there.
        error("NullAway")
        option("NullAway:OnlyNullMarked", "true")
        // Fabric calls the entrypoint before anything else, so fields it sets count as initialized.
        option("NullAway:KnownInitializers", "net.fabricmc.api.ClientModInitializer.onInitializeClient")
        // Mixin fills @Shadow fields with the target class's own fields.
        option("NullAway:ExcludedFieldAnnotations", mixinShadow)
    }
}

// Tests pass null on purpose to check how the code handles it.
tasks.named<JavaCompile>("compileTestJava") {
    options.errorprone.disable("NullAway")
}
tasks.named<JavaCompile>("compileGametestJava") {
    options.errorprone.disable("NullAway")
}

spotless {
    java {
        target("src/*/java/**/*.java")
        palantirJavaFormat(palantirJavaFormatVersion)
    }
}

spotbugs {
    toolVersion.set(spotbugsVersion)
    effort.set(Effort.MAX)
    reportLevel.set(Confidence.MEDIUM)
    excludeFilter.set(file("config/spotbugs/exclude.xml"))
}

tasks.withType<SpotBugsTask>().configureEach {
    reports.create("html") { required.set(true) }
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(javaVersion))
    withSourcesJar()
}

tasks.test {
    useJUnitPlatform()
}

// Coverage reports, without a minimum: `./gradlew jacocoTestReport` for the unit tests, and
// `./gradlew runClientGameTest jacocoGametestReport` for the gametests. Both write HTML to build/reports/jacoco.
jacoco {
    toolVersion = jacocoVersion
}

val runClientGameTest = tasks.named<RunGameTask>("runClientGameTest")
jacoco.applyTo(runClientGameTest.get())
runClientGameTest.configure {
    // Instrument only our classes, not all of Minecraft.
    extensions.configure<JacocoTaskExtension> {
        includes = listOf("net.blockhost.anarchyclient.*")
        excludes = listOf("net.blockhost.anarchyclient.mixin.*", "net.blockhost.anarchyclient.gametest.*")
    }
}

// Mixin classes are copied into Minecraft's classes at runtime and never run as themselves.
val coverageExcludes = listOf("net/blockhost/anarchyclient/mixin/**")

tasks.jacocoTestReport {
    classDirectories.setFrom(sourceSets.main.get().output.asFileTree.matching { exclude(coverageExcludes) })
}

tasks.register<JacocoReport>("jacocoGametestReport") {
    description = "Writes a coverage report for the last client gametest run."
    group = "verification"
    executionData(runClientGameTest.map { it.extensions.getByType<JacocoTaskExtension>().destinationFile!! })
    sourceDirectories.setFrom(sourceSets.main.get().allJava.srcDirs)
    classDirectories.setFrom(sourceSets.main.get().output.asFileTree.matching { exclude(coverageExcludes) })
    mustRunAfter(runClientGameTest)
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = base.archivesName.get()
            from(components["java"])
        }
    }
}
