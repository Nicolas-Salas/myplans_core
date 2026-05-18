package com.myplans.core.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record PlanoRequestDTO(

        @NotBlank(message = "El nombre del plano es obligatorio")
        @Size(max = 255)
        String nombre,

        @NotBlank(message = "El campo 'formulario' es obligatorio")
        @Size(max = 100)
        String formulario,

        @Size(max = 100)
        String alcance,

        @Size(max = 100)
        String subsistema,

        @Size(max = 100)
        String codigoPlano,

        @Size(max = 20)
        String rev,

        String observaciones,

        @Size(max = 255)
        String responsable,

        LocalDate fechaFirma
) {
}