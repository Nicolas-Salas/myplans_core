package com.myplans.core.controller;

import com.myplans.core.dto.TagEstadoUpdateDTO;
import com.myplans.core.dto.TagResponseDTO;
import com.myplans.core.security.AuthenticatedUser;
import com.myplans.core.service.TagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Tags", description = "Operaciones sobre TAGs (CU-07, CU-12, CU-13, CU-14)")
public class TagController {

    private final TagService tagService;

    // -------- endpoints anidados al plano --------

    @Operation(summary = "Cargar TAGs desde Excel (CU-07, RF-07)",
            description = "El idUsuarioIngreso se toma del JWT, no se acepta del cliente.")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/api/v1/planos/{idPlano}/tags/excel",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<TagResponseDTO>> uploadTagsFromExcel(
            @PathVariable Integer idPlano,
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal AuthenticatedUser user) {
        List<TagResponseDTO> response = tagService.uploadTagsFromExcel(idPlano, file, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Listar TAGs de un plano (CU-12, RF-15)")
    @GetMapping("/api/v1/planos/{idPlano}/tags")
    public ResponseEntity<List<TagResponseDTO>> getTagsByPlano(@PathVariable Integer idPlano) {
        return ResponseEntity.ok(tagService.getTagsByPlano(idPlano));
    }

    // -------- endpoints individuales --------

    @Operation(summary = "Detalle de un TAG")
    @GetMapping("/api/v1/tags/{idTag}")
    public ResponseEntity<TagResponseDTO> getTagById(@PathVariable Integer idTag) {
        return ResponseEntity.ok(tagService.getTagById(idTag));
    }

    @Operation(summary = "Cambiar estado de TAG (CU-13, CU-14, RF-16/17/18)",
            description = "Reglas: OBSERVADO requiere comentario; revertir " +
                    "desde APROBADO requiere rol Supervisor; un plano CERRADO " +
                    "bloquea cualquier cambio.")
    @PreAuthorize("hasAnyRole('OPERADOR', 'SUPERVISOR', 'ADMIN')")
    @PatchMapping("/api/v1/tags/{idTag}/estado")
    public ResponseEntity<TagResponseDTO> updateEstado(
            @PathVariable Integer idTag,
            @Valid @RequestBody TagEstadoUpdateDTO request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(tagService.updateEstado(idTag, request, user));
    }
}