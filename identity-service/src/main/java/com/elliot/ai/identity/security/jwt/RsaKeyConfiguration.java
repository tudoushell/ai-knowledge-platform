package com.elliot.ai.identity.security.jwt;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Configuration(proxyBeanMethods = false)
public class RsaKeyConfiguration {

    /**
     * 创建 JWT Decoder。
     */
    @Bean
    public JwtDecoder jwtDecoder(
            RSAPublicKey publicKey,
            JwtProperties properties
    ) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withPublicKey(publicKey)
                .signatureAlgorithm(SignatureAlgorithm.RS256)
                .build();

        // 验证 JWT 签发者、有效期和生效时间。
        decoder.setJwtValidator(
                JwtValidators.createDefaultWithIssuer(
                        properties.getIssuer()
                )
        );

        return decoder;
    }

    /**
     * 加载 RSA 公钥。
     */
    @Bean
    public RSAPublicKey jwtPublicKey(
            JwtProperties properties,
            ResourceLoader resourceLoader
    ) {
        try {
            byte[] encoded = readPem(
                    resourceLoader,
                    properties.getPublicKeyLocation(),
                    "PUBLIC KEY"
            );

            KeyFactory keyFactory = KeyFactory.getInstance("RSA");

            return (RSAPublicKey) keyFactory.generatePublic(
                    new X509EncodedKeySpec(encoded)
            );

        } catch (GeneralSecurityException | IOException
                 | IllegalArgumentException e) {
            throw new IllegalStateException(
                    "Failed to load JWT public key", e
            );
        }
    }

    /**
     * 加载 RSA 私钥。
     */
    @Bean
    public RSAPrivateKey jwtPrivateKey(
            JwtProperties properties,
            ResourceLoader resourceLoader
    ) {
        try {
            byte[] encoded = readPem(
                    resourceLoader,
                    properties.getPrivateKeyLocation(),
                    "PRIVATE KEY"
            );

            KeyFactory keyFactory = KeyFactory.getInstance("RSA");

            return (RSAPrivateKey) keyFactory.generatePrivate(
                    new PKCS8EncodedKeySpec(encoded)
            );

        } catch (GeneralSecurityException | IOException
                 | IllegalArgumentException e) {
            throw new IllegalStateException(
                    "Failed to load JWT private key", e
            );
        }
    }

    /**
     * 构建 Nimbus JWKSource。
     */
    @Bean
    public JWKSource<SecurityContext> jwkSource(
            RSAPublicKey publicKey,
            RSAPrivateKey privateKey,
            JwtProperties properties
    ) {
        // 确保公钥和私钥属于同一 RSA 密钥对。
        if (!publicKey.getModulus().equals(privateKey.getModulus())) {
            throw new IllegalStateException(
                    "JWT public and private keys do not match"
            );
        }

        RSAKey rsaKey = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(properties.getKeyId())
                .keyUse(KeyUse.SIGNATURE)
                .algorithm(JWSAlgorithm.RS256)
                .build();

        return new ImmutableJWKSet<>(
                new JWKSet(rsaKey)
        );
    }

    /**
     * 创建 JWT Encoder。
     */
    @Bean
    public JwtEncoder jwtEncoder(
            JWKSource<SecurityContext> jwkSource
    ) {
        return new NimbusJwtEncoder(jwkSource);
    }

    /**
     * 读取 PEM 文件并提取 Base64 编码内容。
     */
    private byte[] readPem(
            ResourceLoader resourceLoader,
            String location,
            String type
    ) throws IOException {

        Resource resource = resourceLoader.getResource(location);

        if (!resource.exists()) {
            throw new IllegalStateException(
                    "JWT key file does not exist: " + location
            );
        }

        String pem;

        try (InputStream inputStream = resource.getInputStream()) {
            pem = StreamUtils.copyToString(
                    inputStream,
                    StandardCharsets.US_ASCII
            );
        }

        String begin = "-----BEGIN " + type + "-----";
        String end = "-----END " + type + "-----";

        int beginIndex = pem.indexOf(begin);
        int endIndex = pem.indexOf(end);

        if (beginIndex < 0 || endIndex < 0
                || endIndex <= beginIndex + begin.length()) {
            throw new IllegalArgumentException(
                    "Invalid PEM format: " + location
            );
        }

        String base64 = pem.substring(
                beginIndex + begin.length(),
                endIndex
        ).replaceAll("\\s", "");

        return Base64.getDecoder().decode(base64);
    }
}