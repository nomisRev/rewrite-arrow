@Suppress("DSL_SCOPE_VIOLATION") plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.openrewrite)
}

repositories {
    mavenCentral()
}


dependencies {
    implementation(kotlin("stdlib"))
    implementation(libs.arrow.core)
    rewrite(rootProject)
}

tasks.findByName("rewriteDryRun")!!.dependsOn(rootProject.tasks.jar)
tasks.findByName("rewriteRun")!!.dependsOn(rootProject.tasks.jar)
tasks.findByName("rewriteDiscover")!!.dependsOn(rootProject.tasks.jar)

rewrite {
<<<<<<< HEAD
    //activeRecipe()
=======
    activeRecipe(
        "arrow.RaiseImportRefactor",
//        "arrow.RaiseRecipe",
    )
>>>>>>> 7362d90 (Enable RaiseImportRefactor)
}
