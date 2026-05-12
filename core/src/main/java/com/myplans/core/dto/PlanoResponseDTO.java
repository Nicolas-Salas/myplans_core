package com.myplans.core.dto;

import com.myplans.core.entity.enums.PlanoEstado;
import java.time.LocalDate;

public record PlanoResponseDTO(
        Integer idPlano,
        String codigo,
        String nombre,
        PlanoEstado estado,
        String urlPdf,
        LocalDate fechaIngreso
) {
}