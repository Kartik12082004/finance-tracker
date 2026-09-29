package com.kartik.finance_tracker.auth.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    @Mock
    private JwtEncoder jwtEncoder;

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(
                jwtEncoder,
                "finance-tracker",
                900
        );
    }

    @Test
    void generateAccessToken_shouldCreateSignedJwtWithExpectedClaims() {

        UUID userId = UUID.randomUUID();

        Jwt encodedJwt = Jwt.withTokenValue("test-access-token")
                .header("alg", "RS256")
                .claim("iss", "finance-tracker")
                .claim("sub", userId.toString())
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(900))
                .build();

        when(jwtEncoder.encode(
                org.mockito.ArgumentMatchers.any(JwtEncoderParameters.class)
        )).thenReturn(encodedJwt);

        String token = jwtService.generateAccessToken(userId);

        assertThat(token).isEqualTo("test-access-token");

        ArgumentCaptor<JwtEncoderParameters> captor =
                ArgumentCaptor.forClass(JwtEncoderParameters.class);

        verify(jwtEncoder).encode(captor.capture());

        JwtClaimsSet claims = captor.getValue().getClaims();

        // The JWT subject is our internal Finance Tracker user ID.
        assertThat(claims.getSubject())
                .isEqualTo(userId.toString());

        // The issuer identifies Finance Tracker as the token issuer.
        String issuer = (String) claims.getClaim("iss");
        assertThat(issuer)
                .isEqualTo("finance-tracker");

        // The access token should expire 15 minutes after issuance.
        assertThat(claims.getExpiresAt())
                .isAfter(claims.getIssuedAt());

        assertThat(
                claims.getExpiresAt().getEpochSecond()
                        - claims.getIssuedAt().getEpochSecond()
        ).isEqualTo(900);
    }
}
