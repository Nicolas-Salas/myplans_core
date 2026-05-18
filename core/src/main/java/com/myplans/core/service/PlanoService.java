package com.myplans.core.service;

import com.myplans.core.dto.PageResponseDTO;
import com.myplans.core.dto.PlanoRequestDTO;
import com.myplans.core.dto.PlanoResponseDTO;
import com.myplans.core.dto.PlanoUpdateDTO;
import com.myplans.core.entity.enums.PlanoEstado;
import com.myplans.core.security.AuthenticatedUser;
import org.springframework.web.multipart.MultipartFile;

public interface PlanoService {
    PlanoResponseDTO createPlano(PlanoRequestDTO request, AuthenticatedUser user);
    PlanoResponseDTO getPlanoById(Integer idPlano);
    PageResponseDTO<PlanoResponseDTO> getAllPlanos(PlanoEstado status, int page, int size);
    PlanoResponseDTO updatePlano(Integer idPlano, PlanoUpdateDTO request);
    PlanoResponseDTO uploadPlanoPdf(Integer idPlano, MultipartFile file);
    PlanoResponseDTO validarPlano(Integer idPlano);
    PlanoResponseDTO cerrarPlano(Integer idPlano);
}