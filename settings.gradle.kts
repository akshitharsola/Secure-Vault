pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        // Use explicit Maven Central URL to avoid 403 errors in CI
        maven {
            url = uri("https://repo1.maven.org/maven2/")
        }
        mavenCentral()
    }
}

rootProject.name = "SecureVault-Android"
include(":app")
 