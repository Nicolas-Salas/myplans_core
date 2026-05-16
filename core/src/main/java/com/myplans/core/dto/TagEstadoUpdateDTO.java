package com.myplans.core.dto;

import com.myplans.core.entity.enums.TagEstado;
import jakarta.validation.constraints.NotNull;

public record TagEstadoUpdateDTO(

        @NotNull(message = "El nuevo estado es obligatorio")
        TagEstado estadoNuevo,

        String comentario
) {
}