plugins {
  `java-library`
  id("org.liquibase.gradle")
}

dependencies {
  api(project(":outbox-core"))

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
