package com.myplans.core.controller;

import com.myplans.core.dto.PageResponseDTO;
import com.myplans.core.dto.PlanoRequestDTO;
import com.myplans.core.dto.PlanoResponseDTO;
import com.myplans.core.service.PlanoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/planos")
@RequiredArgsConstructor
public class PlanoController {

    private final PlanoService planoService;

    @PostMapping
    public ResponseEntity<PlanoResponseDTO> createPlano(@Valid @RequestBody PlanoRequestDTO request) {
        PlanoResponseDTO response = planoService.createPlano(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PlanoResponseDTO> getPlanoById(@PathVariable Integer id) {
        PlanoResponseDTO response = planoService.getPlanoById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<PageResponseDTO<PlanoResponseDTO>> getAllPlanos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageResponseDTO<PlanoResponseDTO> response = planoService.getAllPlanos(page, size);
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/{id}/pdf", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PlanoResponseDTO> uploadPlanoPdf(
            @PathVariable Integer id,
            @RequestPart("file") MultipartFile file) {
        PlanoResponseDTO response = planoService.uploadPlanoPdf(id, file);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/validar")
    public ResponseEntity<PlanoResponseDTO> validarPlano(@PathVariable Integer id) {
        PlanoResponseDTO response = planoService.validarPlano(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/cerrar")
    public ResponseEntity<PlanoResponseDTO> cerrarPlano(@PathVariable Integer id) {
        PlanoResponseDTO response = planoService.cerrarPlano(id);
        return ResponseEntity.ok(response);
    }
}