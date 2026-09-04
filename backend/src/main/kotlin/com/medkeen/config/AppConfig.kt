package com.medkeen.config

import io.github.cdimascio.dotenv.Dotenv

class AppConfig private constructor(
    val port: Int,
    val projectId: String,
    val openaiApiKey: String?,
    val llmBaseUrl: String,
    val llmModel: String,
    val jdbcUrl: String,
    val jdbcUser: String,
    val jdbcPassword: String,
    val minioEndpoint: String,
    val minioAccessKey: String,
    val minioSecretKey: String,
    val minioBucket: String,
    val jwtSecret: String,
    val jwtIssuer: String,
    val jwtAudience: String,
    val jwtRealm: String,
    val seedDemoData: Boolean,
) {
    companion object {
        fun load(): AppConfig {
            val dotenv = Dotenv.configure().ignoreIfMissing().load()

            fun env(key: String): String? =
                System.getenv(key) ?: dotenv[key]

            return AppConfig(
                port = env("PORT")?.toIntOrNull() ?: 8080,
                projectId = env("GOOGLE_CLOUD_PROJECT") ?: "medkeen",
                openaiApiKey = env("OPENCODE_API_KEY") ?: env("OPENAI_API_KEY"),
                llmBaseUrl = env("LLM_BASE_URL")?.trimEnd('/') ?: "https://api.openai.com/v1",
                llmModel = env("LLM_MODEL") ?: "gpt-4o-mini",
                jdbcUrl = env("JDBC_URL")
                    ?: "jdbc:postgresql://localhost:5432/medkeen",
                jdbcUser = env("JDBC_USER") ?: "medkeen",
                jdbcPassword = env("JDBC_PASSWORD") ?: "medkeen",
                minioEndpoint = env("MINIO_ENDPOINT") ?: "http://localhost:9090",
                minioAccessKey = env("MINIO_ACCESS_KEY") ?: "minioadmin",
                minioSecretKey = env("MINIO_SECRET_KEY") ?: "minioadmin",
                minioBucket = env("MINIO_BUCKET") ?: "medkeen",
                jwtSecret = env("JWT_SECRET")
                    ?: throw IllegalStateException(
                        "JWT_SECRET environment variable is required. " +
                        "Generate one with: openssl rand -base64 48"
                    ),
                jwtIssuer = env("JWT_ISSUER") ?: "medkeen",
                jwtAudience = env("JWT_AUDIENCE") ?: "medkeen-app",
                jwtRealm = env("JWT_REALM") ?: "medkeen",
                seedDemoData = env("SEED_DEMO_DATA")?.lowercase() == "true",
            )
        }
    }
}
