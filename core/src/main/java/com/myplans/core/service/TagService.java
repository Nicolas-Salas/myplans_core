package com.myplans.core.service;

import com.myplans.core.dto.TagResponseDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface TagService {
    List<TagResponseDTO> uploadTagsFromExcel(Integer idPlano, MultipartFile file, Integer idUsuarioIngreso);
    List<TagResponseDTO> getTagsByPlano(Integer idPlano);
}