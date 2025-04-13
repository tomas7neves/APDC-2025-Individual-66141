package pt.unl.fct.di.apdc.firstwebapp.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;

import java.util.Date;
import java.util.UUID;

public class TokenCreator {

    private static final String SECRET_KEY = "MtWlivPN3jP8GyZv1JBoR2Fn5S7JMtguCUALOT5/c5ThRCg0/A4CH9c1AKTswjY3/4BscBpNdm3N5+/C3WEvgQ==";

    public static String generateToken(String id, String username, String role) {
        long validityDuration = 2 * 60 * 60 * 1000; // 2 hours in milliseconds
        long now = System.currentTimeMillis();
        long validTo = now + validityDuration;
        String verifier = UUID.randomUUID().toString();

        return JWT.create()
                .withSubject(id)
                .withClaim("USERNAME", username)
                .withClaim("ROLE", role)
                .withClaim("VALID_FROM", now)
                .withClaim("VALID_TO", validTo)
                .withClaim("VERIF", verifier)
                .withExpiresAt(new Date(validTo))
                .sign(Algorithm.HMAC512(SECRET_KEY));
    }

}
