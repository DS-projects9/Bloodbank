package com.medvault.config

import io.github.cdimascio.dotenv.Dotenv

class AppConfig private constructor(
    val port: Int,
    val projectId: String,
    val openaiApiKey: String?,
) {
    companion object {
        fun load(): AppConfig {
            val dotenv = Dotenv.configure().ignoreIfMissing().load()

            fun env(key: String): String? =
                System.getenv(key) ?: dotenv[key]

            return AppConfig(
                port = env("PORT")?.toIntOrNull() ?: 8080,
                projectId = env("GOOGLE_CLOUD_PROJECT") ?: "medvault-11c68",
                openaiApiKey = env("OPENAI_API_KEY"),
            )
        }
    }
}
