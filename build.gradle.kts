plugins {
    eclipse
    id("org.jetbrains.gradle.plugin.idea-ext")
    id("net.kyori.indra") apply false
    id("net.kyori.indra.git") apply false
    id("net.kyori.indra.license-header") apply false
    id("com.diffplug.eclipse.apt") apply false
}

group = "com.maldloader"
version = "0.3.3"

subprojects {
    apply(plugin="net.kyori.indra")
    apply(plugin="net.kyori.indra.license-header")
    apply(plugin="net.kyori.indra.git")
    apply(plugin="com.diffplug.eclipse.apt")
    apply(plugin="signing")

    repositories {
        maven("https://repo.spongepowered.org/repository/maven-public/") {
            name = "sponge"
        }
    }

    extensions.configure(net.kyori.indra.IndraExtension::class) {
        github("MaldLoader", "MaldGradle") {
            ci(true)
        }
        mitLicense()

        javaVersions {
            testWith(8, 16)
        }

        configurePublications {
            pom {
                organization {
                    name.set("MaldLoader")
                    url.set("https://maldloader.com")
                }
            }
        }

        if (
            project.hasProperty("maldRepo")
        ) {
            publishSnapshotsTo("maldloader", project.property("maldRepo") as String)
            publishReleasesTo("maldloader", project.property("maldRepo") as String)
        }
    }

    extensions.configure(org.cadixdev.gradle.licenser.LicenseExtension::class) {
        val organization: String by project
        val projectUrl: String by project

        properties {
            this["name"] = "VanillaGradle"
            this["organization"] = organization
            this["url"] = projectUrl
        }
        header(rootProject.file("HEADER.txt"))
    }

    afterEvaluate {
        extensions.configure(SigningExtension::class) {
            val spongeSigningKey = project.findProperty("spongeSigningKey") as String?
            val spongeSigningPassword = project.findProperty("spongeSigningPassword") as String?
            if (spongeSigningKey != null && spongeSigningPassword != null) {
                val keyFile = rootProject.file(spongeSigningKey)
                if (keyFile.exists()) {
                    useInMemoryPgpKeys(keyFile.readText(Charsets.UTF_8), spongeSigningPassword)
                } else {
                    useInMemoryPgpKeys(spongeSigningKey, spongeSigningPassword)
                }
            } else {
                signatories = PgpSignatoryProvider() // don't use gpg agent
            }
        }
    }

    tasks {
        withType(Jar::class).configureEach {
            // project.extensions.getByType(net.kyori.indra.git.IndraGitExtension::class).applyVcsInformationToManifest(manifest)
            manifest.attributes(
                "Specification-Title" to "VanillaGradle",
                "Specification-Vendor" to "SpongePowered",
                "Specification-Version" to project.version,
                "Implementation-Title" to project.name,
                "Implementation-Version" to project.version,
                "Implementation-Vendor" to "SpongePowered"
            )
        }

        withType(JavaCompile::class).configureEach {
            options.compilerArgs.add("-Xlint:-processing")
        }
    }
}
