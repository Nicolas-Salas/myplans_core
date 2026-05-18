package com.myplans.core.dto;

import com.myplans.core.entity.enums.TagEstado;
import com.myplans.core.entity.enums.TagTipo;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record TagResponseDTO(
        Integer idTag,
        Integer idPlano,
        String codigo,
        String descripcion,
        String area,
        TagEstado estadoActual,
        String comentario,
        TagTipo tipo,
        Integer idUsuarioIngreso,
        LocalDate fechaIngreso,
        Integer idUsuarioActualizacion,
        LocalDateTime ultimaModificacion
) {
}
