package com.sampoom.factory.common.config.jwt;

import com.sampoom.factory.common.exception.BadRequestException;
import com.sampoom.factory.common.exception.CustomAuthenticationException;
import com.sampoom.factory.common.exception.UnauthorizedException;
import com.sampoom.factory.common.response.ErrorStatus;
import io.jsonwebtoken.*;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Component
public class JwtProvider {
//    private final Key key;
    private final PublicKey publicKey;

    public JwtProvider(@Value("${jwt.public-key-base64}") String publicKeyBase64) {
        if (publicKeyBase64 == null || publicKeyBase64.isBlank()) {
            throw new BadRequestException(ErrorStatus.INVALID_PUBLIC_KEY);
        }
        try {
            this.publicKey = loadPublicKey(publicKeyBase64);
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException(ErrorStatus.INVALID_PUBLIC_KEY);
        }

    }

    private PublicKey loadPublicKey(String base64) throws Exception {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(base64);
            PublicKey key = KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(keyBytes));
            if (key instanceof RSAPublicKey rsaKey) {
                if (rsaKey.getModulus().bitLength() < 2048) {
                    throw new BadRequestException(ErrorStatus.SHORT_PUBLIC_KEY);
                }
            }
            return key;
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException(ErrorStatus.INVALID_PUBLIC_KEY);
        }
    }

    public Claims parse(String token) {
        if (token == null || token.isBlank()) {
            throw new BadRequestException(ErrorStatus.NULL_BLANK_TOKEN);
        }
        try{
            return Jwts.parserBuilder().setSigningKey(publicKey).build()
                    .parseClaimsJws(token).getBody();
        }
        catch (ExpiredJwtException e) {
            throw new CustomAuthenticationException(ErrorStatus.EXPIRED_TOKEN);
        }
        catch (Exception e) {
            // 잘못된 형식 or 위조된 토큰
            throw new CustomAuthenticationException(ErrorStatus.INVALID_TOKEN);
        }
    }

    public String resolveAccessToken(HttpServletRequest request) {
        // 쿠키에서 ACCESS_TOKEN 찾기
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("ACCESS_TOKEN".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        // Bearer 방식일 때
        String header = request.getHeader("Authorization");
        if (header == null) return null;
        if (!header.startsWith("Bearer "))
            throw new UnauthorizedException(ErrorStatus.INVALID_TOKEN);
        return header.substring(7); // "Bearer " 제거
    }
}
