plugins {
    java
}

group = "io.arrow-kt"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

tasks.named<JavaCompile>("compileJava") {
    options.release.set(8)
}

tasks.test {
    useJUnitPlatform()
}

configurations.all {
    resolutionStrategy.cacheChangingModulesFor(0, "seconds")
}

dependencies {
    annotationProcessor(libs.lombok)
    compileOnly(libs.lombok)

    implementation(platform(libs.rewrite.recipe.bom))
    compileOnly(libs.rewrite.core)
    implementation(libs.bundles.rewrite)

    testImplementation(libs.bundles.rewrite.test)
    testRuntimeOnly(libs.arrow.core)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.lombok)
}
