package com.myplans.core.dto;

import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record PlanoUpdateDTO(

        @Size(max = 255)
        String nombre,

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
    public boolean isEmpty() {
        return isBlank(nombre)
                && isBlank(formulario)
                && isBlank(alcance)
                && isBlank(subsistema)
                && isBlank(codigoPlano)
                && isBlank(rev)
                && isBlank(observaciones)
                && isBlank(responsable)
                && fechaFirma == null;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}