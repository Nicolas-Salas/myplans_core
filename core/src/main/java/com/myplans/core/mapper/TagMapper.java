package com.myplans.core.mapper;

import com.myplans.core.dto.TagResponseDTO;
import com.myplans.core.entity.Tag;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TagMapper {
    TagResponseDTO toResponse(Tag entity);
    List<TagResponseDTO> toResponseList(List<Tag> entities);
}