rootProject.name = "spring-boot-starter-domain-outbox"

val springBootVersion: String by settings
val dependencyManagementVersion: String by settings
val liquibaseGradlePluginVersion: String by settings

pluginManagement {
  repositories {
    gradlePluginPortal()
    mavenCentral()
  }
  plugins {
    id("org.springframework.boot") version springBootVersion
    id("io.spring.dependency-management") version dependencyManagementVersion
    id("org.liquibase.gradle") version liquibaseGradlePluginVersion
  }
}

include(
  "outbox-core",
  "outbox-jdbc",
  "outbox-spring-boot-starter",
  "example-app"
)
