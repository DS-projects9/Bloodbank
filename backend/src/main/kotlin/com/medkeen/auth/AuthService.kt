package com.medkeen.auth

import at.favre.lib.crypto.bcrypt.BCrypt
import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import com.medkeen.config.AppConfig
import java.util.Date

object AuthService {
    private lateinit var cfg: AppConfig
    private lateinit var verifier: JWTVerifier
    private lateinit var algorithm: Algorithm

    private const val TOKEN_EXPIRY_MS = 24 * 60 * 60 * 1000L // 24 hours

    fun init(cfg: AppConfig) {
        this.cfg = cfg
        algorithm = Algorithm.HMAC256(cfg.jwtSecret)
        verifier = JWT.require(algorithm)
            .withIssuer(cfg.jwtIssuer)
            .withAudience(cfg.jwtAudience)
            .acceptExpiresAt(60) // allow 60s clock drift
            .build()
    }

    fun issueToken(uid: String, email: String?, role: String?, verified: Boolean): String =
        JWT.create()
            .withIssuer(cfg.jwtIssuer)
            .withAudience(cfg.jwtAudience)
            .withSubject(uid)
            .withClaim("email", email)
            .withClaim("role", role)
            .withClaim("verified", verified)
            .withIssuedAt(Date())
            .withExpiresAt(Date(System.currentTimeMillis() + TOKEN_EXPIRY_MS))
            .sign(algorithm)

    fun verifyToken(token: String): DecodedJWT = verifier.verify(token)

    fun hashPassword(password: String): String =
        BCrypt.withDefaults().hashToString(12, password.toCharArray())

    fun verifyPassword(password: String, hash: String): Boolean =
        try {
            BCrypt.verifyer().verify(password.toCharArray(), hash).verified
        } catch (_: Exception) {
            false
        }
}
