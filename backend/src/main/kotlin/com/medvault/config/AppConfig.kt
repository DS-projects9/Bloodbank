package com.medvault.config

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
                projectId = env("GOOGLE_CLOUD_PROJECT") ?: "medvault",
                openaiApiKey = env("OPENCODE_API_KEY") ?: env("OPENAI_API_KEY"),
                llmBaseUrl = env("LLM_BASE_URL")?.trimEnd('/') ?: "https://api.openai.com/v1",
                llmModel = env("LLM_MODEL") ?: "gpt-4o-mini",
                jdbcUrl = env("JDBC_URL")
                    ?: "jdbc:postgresql://localhost:5432/medvault",
                jdbcUser = env("JDBC_USER") ?: "medvault",
                jdbcPassword = env("JDBC_PASSWORD") ?: "medvault",
                minioEndpoint = env("MINIO_ENDPOINT") ?: "http://localhost:9090",
                minioAccessKey = env("MINIO_ACCESS_KEY") ?: "minioadmin",
                minioSecretKey = env("MINIO_SECRET_KEY") ?: "minioadmin",
                minioBucket = env("MINIO_BUCKET") ?: "medvault",
                jwtSecret = env("JWT_SECRET")
                    ?: "dev-only-insecure-change-me-in-production",
                jwtIssuer = env("JWT_ISSUER") ?: "medvault",
                jwtAudience = env("JWT_AUDIENCE") ?: "medvault-app",
                jwtRealm = env("JWT_REALM") ?: "medvault",
                seedDemoData = env("SEED_DEMO_DATA")?.lowercase() == "true",
            )
        }
    }
}
