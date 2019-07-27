plugins {
    kotlin("multiplatform")
    application
}

application {
    mainClassName = "godot.generator.MainKt"
}

val generatedSrcDir = file("$projectDir/src/generated/kotlin")

kotlin {

    // Add JVM Target (for Kotlin class generation)
    jvm {
        withJava()
    }

    mingwX64("mingw") {
        compilations {
            val main by getting {
                binaries {
                    staticLib()
                }
                kotlinOptions {
                    freeCompilerArgs = freeCompilerArgs + "-Xuse-experimental=kotlin.Experimental"
                }
                val godotapi by cinterops.creating {}
            }
        }
    }
    sourceSets {
        val jvmMain by getting {
            dependencies {
                api(kotlin("reflect"))
                api("com.squareup:kotlinpoet:1.3.0")
                api("com.google.code.gson:gson:2.8.5")
            }
        }
        val mingwMain by getting {
            kotlin {
                kotlin.srcDir(generatedSrcDir)
            }
        }
    }
}

val deleteGenerated by tasks.registering(Delete::class) {
    delete(generatedSrcDir)
}

tasks {
    "run"(JavaExec::class) {
        environment("GEN_SRC_DIR", generatedSrcDir.toString())
        dependsOn(getByName("compileKotlinJvm"))
    }

    getByName("compileKotlinMingw") {
        dependsOn("run"(JavaExec::class))
    }

    clean {
        dependsOn(deleteGenerated)
    }
}