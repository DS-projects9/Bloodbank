package com.medvault.auth

import at.favre.lib.crypto.bcrypt.BCrypt
import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import com.medvault.config.AppConfig

object AuthService {
    private lateinit var cfg: AppConfig
    private lateinit var verifier: JWTVerifier
    private lateinit var algorithm: Algorithm

    fun init(cfg: AppConfig) {
        this.cfg = cfg
        algorithm = Algorithm.HMAC256(cfg.jwtSecret)
        verifier = JWT.require(algorithm)
            .withIssuer(cfg.jwtIssuer)
            .withAudience(cfg.jwtAudience)
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
