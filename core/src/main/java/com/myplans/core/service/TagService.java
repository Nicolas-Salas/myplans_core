package com.myplans.core.service;

import com.myplans.core.dto.TagEstadoUpdateDTO;
import com.myplans.core.dto.TagResponseDTO;
import com.myplans.core.security.AuthenticatedUser;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface TagService {
    List<TagResponseDTO> uploadTagsFromExcel(Integer idPlano, MultipartFile file, AuthenticatedUser user);
    List<TagResponseDTO> getTagsByPlano(Integer idPlano);
    TagResponseDTO getTagById(Integer idTag);
    TagResponseDTO updateEstado(Integer idTag, TagEstadoUpdateDTO request, AuthenticatedUser user);
}