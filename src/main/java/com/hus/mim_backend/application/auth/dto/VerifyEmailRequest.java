package com.hus.mim_backend.application.auth.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class VerifyEmailRequest {
    private String token;
}
