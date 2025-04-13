package pt.unl.fct.di.apdc.firstwebapp.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;

import java.util.Date;
import java.util.logging.Logger;

public class TokenUtil {

    private static final Logger LOG = Logger.getLogger(TokenUtil.class.getName());
    private static final String SECRET_KEY = "MtWlivPN3jP8GyZv1JBoR2Fn5S7JMtguCUALOT5/c5ThRCg0/A4CH9c1AKTswjY3/4BscBpNdm3N5+/C3WEvgQ==";
    private static final Algorithm ALGORITHM = Algorithm.HMAC512(SECRET_KEY);

    public static DecodedJWT decodeToken(String token) throws JWTDecodeException {
        return JWT.decode(token);
    }

    public static boolean verifyToken(String token) {
        try {
            JWTVerifier verifier = JWT.require(ALGORITHM).build();
            verifier.verify(token);
            return true;
        } catch (JWTVerificationException e) {
            LOG.warning("Token verification failed: " + e.getMessage());
            return false;
        }
    }

    public static String getIdFromToken(String token) {
        DecodedJWT decodedJWT = decodeToken(token);
        return decodedJWT.getSubject();
    }

    public static String getUsernameFromToken(String token) {
        DecodedJWT decodedJWT = decodeToken(token);
        return decodedJWT.getClaim("USERNAME").asString();
    }

    public static String getRoleFromToken(String token) {
        DecodedJWT decodedJWT = decodeToken(token);
        return decodedJWT.getClaim("ROLE").asString();
    }

    public static boolean isTokenExpired(String token) {
        DecodedJWT decodedJWT = decodeToken(token);
        Date expirationDate = decodedJWT.getExpiresAt();
        return expirationDate.before(new Date());
    }
}
