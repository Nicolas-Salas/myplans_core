package com.myplans.core.mapper;

import com.myplans.core.dto.TagResponseDTO;
import com.myplans.core.entity.Tag;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TagMapper {
    @Mapping(target = "idPlano", source = "plano.idPlano")
    TagResponseDTO toResponse(Tag entity);
    List<TagResponseDTO> toResponseList(List<Tag> entities);
}