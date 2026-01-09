plugins {
  id("org.springframework.boot")
  id("io.spring.dependency-management")
  java
}

val springBootVersion: String by project

dependencies {
  implementation(project(":outbox-spring-boot-starter"))
  implementation("org.springframework.boot:spring-boot-starter:$springBootVersion")
}
