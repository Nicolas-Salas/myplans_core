package com.myplans.core.entity;

import com.myplans.core.entity.enums.PlanoEstado;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "PLANO")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Plano {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_plano")
    private Integer idPlano;

    @NotBlank
    @Size(max = 255)
    @Column(name = "nombre", nullable = false, length = 255)
    private String nombre;

    @NotBlank
    @Size(max = 100)
    @Column(name = "formulario", nullable = false, length = 100)
    private String formulario;

    @Size(max = 500)
    @Column(name = "url_s3", length = 500)
    private String urlS3;

    @Size(max = 100)
    @Column(name = "alcance", length = 100)
    private String alcance;

    @Size(max = 100)
    @Column(name = "subsistema", length = 100)
    private String subsistema;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "status", nullable = false, length = 20)
    private PlanoEstado status = PlanoEstado.ABIERTO;

    @Size(max = 100)
    @Column(name = "codigo_plano", length = 100)
    private String codigoPlano;

    @Size(max = 20)
    @Column(name = "rev", length = 20)
    private String rev;

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;

    @Size(max = 255)
    @Column(name = "responsable", length = 255)
    private String responsable;

    @Column(name = "fecha_firma")
    private LocalDate fechaFirma;

    @NotNull
    @Builder.Default
    @Column(name = "nro_paginas", nullable = false)
    private Integer nroPaginas = 0;

    @CreatedDate
    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    @OneToMany(mappedBy = "plano", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Tag> tags = new ArrayList<>();
}