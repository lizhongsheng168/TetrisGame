pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/")
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/")
    }
}

rootProject.name = "TetrisGame"
include(":app")
