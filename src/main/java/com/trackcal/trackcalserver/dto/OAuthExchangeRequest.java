package com.trackcal.trackcalserver.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OAuthExchangeRequest {
    @NotBlank(message = "OAuth code is required")
    private String code;
}
