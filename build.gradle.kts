plugins {
    // To optionally create a shadow/fat jar that bundle up any non-core dependencies
    id("com.gradleup.shadow") version "8.3.5"
    // QuPath Gradle extension convention plugin
    id("qupath-conventions")
}

//the create-extension command code (gradlew createExtension -PextensionName=MyAwesome)
//apply(from = "create-extension.gradle.kts")

// TODO: Configure your extension here (please change the defaults!)
qupathExtension {
    name = "qupath-extension-ocr"
    group = "io.github.qupath"
    version = "0.1.0-SNAPSHOT"
    description = "A QuPath extension converting label images to text"
    automaticModule = "io.github.qupath.extension.ocr"
}

// TODO: Define your dependencies here
dependencies {

    // Main dependencies for most QuPath extensions
    shadow(libs.bundles.qupath)
    shadow(libs.bundles.logging)
    shadow(libs.qupath.fxtras)

    // Source: https://mvnrepository.com/artifact/net.sourceforge.tess4j/tess4j
    implementation("net.sourceforge.tess4j:tess4j:5.17.0")

    // For testing
    testImplementation(libs.bundles.qupath)
    testImplementation(libs.junit)

}
