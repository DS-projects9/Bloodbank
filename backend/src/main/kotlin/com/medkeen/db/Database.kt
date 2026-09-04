package com.medkeen.db

import com.medkeen.config.AppConfig
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.sql.Connection

object Database {
    lateinit var cfg: AppConfig
    lateinit var ds: HikariDataSource

    fun init(cfg: AppConfig) {
        this.cfg = cfg
        ds = HikariDataSource(HikariConfig().apply {
            jdbcUrl = cfg.jdbcUrl
            username = cfg.jdbcUser
            password = cfg.jdbcPassword
            maximumPoolSize = 10
            isAutoCommit = true
        })
        createTable()
    }

    private fun createTable() {
        ds.connection.use { conn ->
            conn.createStatement().execute(
                """
                CREATE TABLE IF NOT EXISTS documents (
                    collection text NOT NULL,
                    id         text NOT NULL,
                    data       jsonb NOT NULL,
                    PRIMARY KEY (collection, id)
                )
                """.trimIndent()
            )
        }
    }

    fun connection(): Connection = ds.connection
}
