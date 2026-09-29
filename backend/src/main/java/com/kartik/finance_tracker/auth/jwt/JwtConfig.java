package com.kartik.finance_tracker.auth.jwt;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@Configuration
public class JwtConfig {

    @Bean
    public JwtEncoder jwtEncoder(
            @Value("${jwt.private-key-path}") String privateKeyPath
    ) throws Exception {

        // The public key is stored beside the private key.
        // It is used later to verify the JWT signature.
        Path privateKeyFile = Path.of(privateKeyPath);
        Path publicKeyFile = privateKeyFile
                .resolveSibling("jwt-public.pem");

        RSAPrivateKey privateKey = loadPrivateKey(privateKeyFile);
        RSAPublicKey publicKey = loadPublicKey(publicKeyFile);

        // RS256 signs the JWT with the private key.
        // The matching public key verifies the signature.
        return NimbusJwtEncoder
                .withKeyPair(publicKey, privateKey)
                .algorithm(SignatureAlgorithm.RS256)
                .build();
    }

    @Bean
    public JwtDecoder jwtDecoder(
            @Value("${jwt.private-key-path}") String privateKeyPath
    ) throws Exception {

        // The public key verifies JWTs that were signed by our private key.
        Path privateKeyFile = Path.of(privateKeyPath);
        Path publicKeyFile = privateKeyFile
                .resolveSibling("jwt-public.pem");

        RSAPublicKey publicKey = loadPublicKey(publicKeyFile);

        return NimbusJwtDecoder
                .withPublicKey(publicKey)
                .build();
    }

    private RSAPrivateKey loadPrivateKey(Path path) throws Exception {

        String pem = Files.readString(path);

        // Remove the PEM wrapper and whitespace before Base64 decoding.
        String base64 = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");

        byte[] keyBytes = Base64.getDecoder().decode(base64);

        PKCS8EncodedKeySpec keySpec =
                new PKCS8EncodedKeySpec(keyBytes);

        KeyFactory keyFactory = KeyFactory.getInstance("RSA");

        return (RSAPrivateKey) keyFactory.generatePrivate(keySpec);
    }

    private RSAPublicKey loadPublicKey(Path path) throws Exception {

        String pem = Files.readString(path);

        // Remove the PEM wrapper and whitespace before Base64 decoding.
        String base64 = pem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");

        byte[] keyBytes = Base64.getDecoder().decode(base64);

        X509EncodedKeySpec keySpec =
                new X509EncodedKeySpec(keyBytes);

        KeyFactory keyFactory = KeyFactory.getInstance("RSA");

        return (RSAPublicKey) keyFactory.generatePublic(keySpec);
    }
}