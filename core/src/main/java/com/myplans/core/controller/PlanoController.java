package com.myplans.core.controller;

import com.myplans.core.dto.PageResponseDTO;
import com.myplans.core.dto.PlanoRequestDTO;
import com.myplans.core.dto.PlanoResponseDTO;
import com.myplans.core.dto.PlanoUpdateDTO;
import com.myplans.core.entity.enums.PlanoEstado;
import com.myplans.core.security.AuthenticatedUser;
import com.myplans.core.service.PlanoService;
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

@RestController
@RequestMapping("/api/v1/planos")
@RequiredArgsConstructor
@Tag(name = "Planos", description = "Gestión de planos (CU-07 a CU-12, CU-16, CU-17, CU-18)")
public class PlanoController {

    private final PlanoService planoService;

    @Operation(summary = "Crear plano (CU-08)",
            description = "Crea un plano vacío en estado ABIERTO. El PDF se sube en un paso posterior.")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<PlanoResponseDTO> createPlano(
            @Valid @RequestBody PlanoRequestDTO request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        PlanoResponseDTO response = planoService.createPlano(request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Buscar plano por ID (CU-09)")
    @GetMapping("/{id}")
    public ResponseEntity<PlanoResponseDTO> getPlanoById(@PathVariable Integer id) {
        return ResponseEntity.ok(planoService.getPlanoById(id));
    }

    @Operation(summary = "Dashboard de planos (CU-10)",
            description = "Listado paginado, opcionalmente filtrado por estado.")
    @GetMapping
    public ResponseEntity<PageResponseDTO<PlanoResponseDTO>> getAllPlanos(
            @RequestParam(required = false) PlanoEstado status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(planoService.getAllPlanos(status, page, size));
    }

    @Operation(summary = "Editar plano",
            description = "Edición parcial. Cualquier campo opcional; al menos uno requerido.")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    @PutMapping("/{id}")
    public ResponseEntity<PlanoResponseDTO> updatePlano(
            @PathVariable Integer id,
            @Valid @RequestBody PlanoUpdateDTO request) {
        return ResponseEntity.ok(planoService.updatePlano(id, request));
    }

    @Operation(summary = "Cargar PDF del plano (CU-08, RF-11)")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/{id}/pdf", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PlanoResponseDTO> uploadPlanoPdf(
            @PathVariable Integer id,
            @RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(planoService.uploadPlanoPdf(id, file));
    }

    @Operation(summary = "Validar plano (CU-16, RF-23)",
            description = "Cambia el estado a VALIDADO. Solo Supervisor.")
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'ADMIN')")
    @PutMapping("/{id}/validar")
    public ResponseEntity<PlanoResponseDTO> validarPlano(@PathVariable Integer id) {
        return ResponseEntity.ok(planoService.validarPlano(id));
    }

    @Operation(summary = "Cerrar plano (CU-17, RF-24)",
            description = "Cambia el estado a CERRADO. Requiere que esté VALIDADO. Solo Supervisor.")
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'ADMIN')")
    @PutMapping("/{id}/cerrar")
    public ResponseEntity<PlanoResponseDTO> cerrarPlano(@PathVariable Integer id) {
        return ResponseEntity.ok(planoService.cerrarPlano(id));
    }
}
