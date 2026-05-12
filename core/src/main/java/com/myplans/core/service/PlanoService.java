package com.myplans.core.service;

import com.myplans.core.dto.PageResponseDTO;
import com.myplans.core.dto.PlanoRequestDTO;
import com.myplans.core.dto.PlanoResponseDTO;
import org.springframework.web.multipart.MultipartFile;

public interface PlanoService {
    PlanoResponseDTO createPlano(PlanoRequestDTO request);
    PlanoResponseDTO getPlanoById(Integer idPlano);
    PageResponseDTO<PlanoResponseDTO> getAllPlanos(int page, int size);
    PlanoResponseDTO uploadPlanoPdf(Integer idPlano, MultipartFile file);
    PlanoResponseDTO validarPlano(Integer idPlano);
    PlanoResponseDTO cerrarPlano(Integer idPlano);
}