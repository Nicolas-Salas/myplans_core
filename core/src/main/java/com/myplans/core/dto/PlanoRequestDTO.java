package com.myplans.core.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PlanoRequestDTO(
        @NotBlank
        @Size(max = 100)
        String codigo,

        @NotBlank
        @Size(max = 255)
        String nombre,

        String descripcion,

        @NotNull
        Integer idUsuarioIngreso
) {
}