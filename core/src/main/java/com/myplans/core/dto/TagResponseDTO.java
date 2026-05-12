package com.myplans.core.dto;

import com.myplans.core.entity.enums.TagEstado;
import com.myplans.core.entity.enums.TagTipo;
import java.time.LocalDateTime;

public record TagResponseDTO(
        Integer idTag,
        String codigo,
        String descripcion,
        String area,
        TagEstado estadoActual,
        TagTipo tipo,
        LocalDateTime ultimaModificacion
) {
}