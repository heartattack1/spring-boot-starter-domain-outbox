plugins {
  `java-library`
  id("org.liquibase.gradle")
}

val springBootVersion: String by project

dependencies {
  api(platform("org.springframework.boot:spring-boot-dependencies:$springBootVersion"))
  api(project(":outbox-core"))

  compileOnly("org.projectlombok:lombok")
  annotationProcessor("org.projectlombok:lombok")
  implementation("org.springframework:spring-jdbc")
  implementation("com.fasterxml.jackson.core:jackson-databind")

  liquibaseRuntime("org.liquibase:liquibase-core")
  liquibaseRuntime("org.postgresql:postgresql")
}

liquibase {
  activities.register("main") {
    arguments = mapOf(
      "changelogFile" to "db/changelog/db.changelog-master.yaml",
      "url" to (System.getenv("DB_URL") ?: "jdbc:postgresql://localhost:5432/outbox"),
      "username" to (System.getenv("DB_USER") ?: "outbox"),
      "password" to (System.getenv("DB_PASS") ?: "outbox")
    )
  }
  runList = "main"
}
