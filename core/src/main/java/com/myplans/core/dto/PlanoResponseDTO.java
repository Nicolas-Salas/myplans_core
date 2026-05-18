package com.myplans.core.dto;

import com.myplans.core.entity.enums.PlanoEstado;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PlanoResponseDTO(
        Integer idPlano,
        String nombre,
        String formulario,
        String urlS3,
        String alcance,
        String subsistema,
        PlanoEstado status,
        String codigoPlano,
        String rev,
        String observaciones,
        String responsable,
        LocalDate fechaFirma,
        Integer nroPaginas,
        LocalDateTime fechaCreacion,
        Long cantidadTags
) {
}