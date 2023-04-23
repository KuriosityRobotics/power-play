plugins {
    id("java")
    id("org.jetbrains.intellij") version "1.5.2"
    id ("com.diffplug.spotless") version "6.9.1"
}

group = "com.kuriosityrobotics.client"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
//    implementation (project(":hardware-independent"))
    implementation ("org.apache.commons:commons-collections4:4.4")
    implementation("org.openpnp:opencv:4.5.1-2")
    implementation("org.apache.commons:commons-math3:3.6.1")
    implementation ("com.nqzero:permit-reflect:0.4")

    implementation ("org.reflections:reflections:0.10.2")


    implementation ("com.ericsson.commonlibrary:proxy:1.2.13")
    implementation("net.bytebuddy:byte-buddy:1.12.12")
    implementation("org.slf4j:slf4j-api:1.7.36")
    implementation("org.slf4j:slf4j-simple:1.7.36")
    implementation ("de.ruedigermoeller:fst:2.56")
    implementation ("org.ojalgo:ojalgo:51.1.0")
    implementation(project(":api"))
}

// Configure Gradle IntelliJ Plugin - read more: https://github.com/JetBrains/gradle-intellij-plugin
intellij {
    version.set("2022.1")
    type.set("IU") // Target IDE Platform

    plugins.set(listOf())
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "11"
        targetCompatibility = "11"
    }

    patchPluginXml {
        sinceBuild.set("212")
        untilBuild.set("222.*")
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }
}

task ("prepareKotlinBuildScriptModel") {

}

apply(from = "../spotless.gradle")
java {
    sourceCompatibility = JavaVersion.VERSION_16
    targetCompatibility = JavaVersion.VERSION_16
}

//
//java {
//    toolchain {
//        languageVersion.set(JavaLanguageVersion.of(11))
//        vendor.set(JvmVendorSpec.matching("Private Build"))
//    }
//}
