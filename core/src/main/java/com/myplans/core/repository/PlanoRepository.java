package com.myplans.core.repository;

import com.myplans.core.entity.Plano;
import com.myplans.core.entity.enums.PlanoEstado;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlanoRepository extends JpaRepository<Plano, Integer> {
    Page<Plano> findByStatus(PlanoEstado status, Pageable pageable);
}