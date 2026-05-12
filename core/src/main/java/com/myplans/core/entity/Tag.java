package com.myplans.core.entity;

import com.myplans.core.entity.enums.TagEstado;
import com.myplans.core.entity.enums.TagTipo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "TAG", uniqueConstraints = {@UniqueConstraint(columnNames = {"id_plano", "codigo"})})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idTag;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_plano", nullable = false)
    private Plano plano;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String codigo;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Size(max = 100)
    @Column(length = 100)
    private String area;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private TagEstado estadoActual = TagEstado.PENDIENTE;

    @Column(columnDefinition = "TEXT")
    private String comentario;

    @NotNull
    @Column(nullable = false)
    private Integer idUsuarioIngreso;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDate fechaIngreso;

    @Column
    private Integer idUsuarioActualizacion;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TagTipo tipo;

    @LastModifiedDate
    @Column
    private LocalDateTime ultimaModificacion;
}