package ru.daniil.bulletinBoard.entity.response.auth;

import jakarta.persistence.PrePersist;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JwtResponse {
    private String accessToken;
    private String refreshToken;
    private String type;
    private Long expiresIn;

    public JwtResponse(String accessToken, String refreshToken, Long expiresIn) {
        type = "Bearer";
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresIn = expiresIn;
    }

    @PrePersist
    protected void onCreate() {
        type = "Bearer";
    }
}