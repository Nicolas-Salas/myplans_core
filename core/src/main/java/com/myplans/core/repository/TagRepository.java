package com.myplans.core.repository;

import com.myplans.core.entity.Plano;
import com.myplans.core.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TagRepository extends JpaRepository<Tag, Integer> {
    List<Tag> findByPlanoIdPlano(Integer idPlano);
    boolean existsByPlanoAndCodigo(Plano plano, String codigo);
}