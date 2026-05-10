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
plugins {
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://storage.googleapis.com/download.flutter.io") }
    }
}

rootProject.name = "Google-World-Web"
include(":app")

val flutterFile = file("flutter_module/.android/include_flutter.groovy")
if (flutterFile.exists()) {
    apply(from = flutterFile)
} else {
    logger.warn("Flutter module inclusion script not found at ${flutterFile.absolutePath}. " +
            "If you are using CI, ensure you run 'flutter pub get' in the flutter_module directory first.")
}
