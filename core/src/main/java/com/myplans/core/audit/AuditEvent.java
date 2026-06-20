package com.myplans.core.audit;

public record AuditEvent(
        Integer idTag,
        Integer idUsuario,
        String estadoAnterior,
        String estadoNuevo,
        String observaciones,
        Boolean porIa
) {
}