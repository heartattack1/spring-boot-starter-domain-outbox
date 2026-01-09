plugins {
  `java-library`
}

val springBootVersion: String by project

dependencies {
  api(project(":outbox-core"))
  api(project(":outbox-jdbc"))

  implementation("org.springframework.boot:spring-boot-autoconfigure:$springBootVersion")
  annotationProcessor("org.springframework.boot:spring-boot-configuration-processor:$springBootVersion")
}
