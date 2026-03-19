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
    private String type;
    private Long expiresIn;

    public JwtResponse(String accessToken, Long expiresIn, String tokenType) {
        this.accessToken = accessToken;
        this.expiresIn = expiresIn;
        this.type = tokenType;
    }

    @PrePersist
    protected void onCreate() {
        type = "Bearer";
    }
}