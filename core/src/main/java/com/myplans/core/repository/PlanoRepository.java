package com.myplans.core.repository;

import com.myplans.core.entity.Plano;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PlanoRepository extends JpaRepository<Plano, Integer> {
    Optional<Plano> findByCodigo(String codigo);
    boolean existsByCodigo(String codigo);
}