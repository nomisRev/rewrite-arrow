plugins {
    kotlin("jvm") version "1.8.0"
    id("org.openrewrite.rewrite") version "5.36.0"
}

group = "io.arrow-kt"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
}

tasks.named<JavaCompile>("compileJava") {
    options.release.set(8)
}

tasks.test {
    useJUnitPlatform()
}

rewrite {
    activeRecipe(
        "arrow.SayHelloRecipe"
    )
}

configurations.all {
    resolutionStrategy.cacheChangingModulesFor(0, "seconds")
}

dependencies {
    annotationProcessor("org.projectlombok:lombok:latest.release")

    implementation(platform("org.openrewrite.recipe:rewrite-recipe-bom:1.15.0"))
    compileOnly("org.openrewrite:rewrite-core")
    compileOnly("org.projectlombok:lombok:latest.release")
    implementation("org.openrewrite:rewrite-java")

    implementation("org.openrewrite:rewrite-kotlin:0.2.0-SNAPSHOT")

    testRuntimeOnly("io.arrow-kt:arrow-core:1.1.6-alpha.28")

    testImplementation("org.openrewrite:rewrite-test")
    testImplementation("org.openrewrite.recipe:rewrite-testing-frameworks")
    testImplementation("org.junit.jupiter:junit-jupiter-api:latest.release")
    testImplementation("org.junit.jupiter:junit-jupiter-params:latest.release")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:latest.release")
    testRuntimeOnly("ch.qos.logback:logback-classic:1.2.+")
    testImplementation("org.assertj:assertj-core:latest.release")
}
