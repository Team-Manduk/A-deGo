pluginManagement {
    includeBuild("build-logic")
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
        mavenCentral()
    }
}

rootProject.name = "A-dego"
include(":app")
include(
    ":feature:main",
    ":feature:home",
)
include(
    ":core:designsystem",
    ":core:navigation",
    ":core:data-api",
    ":core:remote",
    ":core:data",
    ":core:domain",
    ":core:model",
)
