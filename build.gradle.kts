plugins {
  id("maven-publish")
}

allprojects {
  group = "io.github.example"
  version = "0.1.0-SNAPSHOT"

  repositories {
    mavenCentral()
  }
}

subprojects {
  plugins.withId("java") {
    the<JavaPluginExtension>().toolchain {
      languageVersion.set(JavaLanguageVersion.of(25))
    }
  }
}
