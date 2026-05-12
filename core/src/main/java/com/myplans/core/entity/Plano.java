package com.myplans.core.entity;

import com.myplans.core.entity.enums.PlanoEstado;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
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
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "PLANO", uniqueConstraints = {@UniqueConstraint(columnNames = {"codigo"})})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Plano {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idPlano;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String codigo;

    @NotBlank
    @Size(max = 255)
    @Column(nullable = false, length = 255)
    private String nombre;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @NotNull
    @Column(nullable = false)
    private Integer idUsuarioIngreso;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDate fechaIngreso;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private PlanoEstado estado = PlanoEstado.PENDIENTE;

    @Size(max = 255)
    @Column(length = 255)
    private String urlPdf;

    @OneToMany(mappedBy = "plano", cascade = CascadeType.ALL)
    private List<Tag> tags;
}