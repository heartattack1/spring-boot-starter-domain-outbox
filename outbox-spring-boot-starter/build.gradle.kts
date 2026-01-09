plugins {
  `java-library`
}

val springBootVersion: String by project

dependencies {
  api(project(":outbox-core"))
  api(project(":outbox-jdbc"))

  compileOnly("org.projectlombok:lombok")
  annotationProcessor("org.projectlombok:lombok")
  implementation("org.springframework.boot:spring-boot-autoconfigure:$springBootVersion")
  implementation("org.springframework.boot:spring-boot-starter:$springBootVersion")
  implementation("org.springframework.boot:spring-boot-starter-jdbc:$springBootVersion")
  annotationProcessor("org.springframework.boot:spring-boot-configuration-processor:$springBootVersion")
}
