package com.catolica.Artepax.authentication.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GoogleAuthRequest {

    @NotBlank(message = "El idToken es obligatorio")
    private String idToken;

}
