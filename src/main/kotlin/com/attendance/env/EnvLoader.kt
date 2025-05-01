package com.attendance.env

import io.github.cdimascio.dotenv.dotenv

class EnvLoader {
    private val dotenv = dotenv {
        ignoreIfMissing = true
    }

    fun readEnv(key: String): String {
        return System.getenv(key) ?: dotenv[key] ?: error("Missing environment variable: $key")
    }
}