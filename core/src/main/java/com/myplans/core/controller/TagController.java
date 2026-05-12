package com.myplans.core.controller;

import com.myplans.core.dto.TagResponseDTO;
import com.myplans.core.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/planos/{idPlano}/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    @PostMapping(value = "/excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<TagResponseDTO>> uploadTagsFromExcel(
            @PathVariable Integer idPlano,
            @RequestPart("file") MultipartFile file,
            @RequestParam Integer idUsuario) {
        
        List<TagResponseDTO> response = tagService.uploadTagsFromExcel(idPlano, file, idUsuario);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<TagResponseDTO>> getTagsByPlano(@PathVariable Integer idPlano) {
        List<TagResponseDTO> response = tagService.getTagsByPlano(idPlano);
        return ResponseEntity.ok(response);
    }
}