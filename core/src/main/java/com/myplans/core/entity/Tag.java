package com.myplans.core.entity;

import com.myplans.core.entity.enums.TagEstado;
import com.myplans.core.entity.enums.TagTipo;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "TAG", uniqueConstraints = {
        @UniqueConstraint(name = "uk_tag_plano_codigo", columnNames = {"id_plano", "codigo"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tag")
    private Integer idTag;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_plano", nullable = false)
    private Plano plano;

    @NotBlank
    @Size(max = 100)
    @Column(name = "codigo", nullable = false, length = 100)
    private String codigo;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Size(max = 100)
    @Column(name = "area", length = 100)
    private String area;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "estado_actual", nullable = false, length = 20)
    private TagEstado estadoActual = TagEstado.PENDIENTE;

    @Column(name = "comentario", columnDefinition = "TEXT")
    private String comentario;

    @NotNull
    @Column(name = "id_usuario_ingreso", nullable = false)
    private Integer idUsuarioIngreso;

    @CreatedDate
    @Column(name = "fecha_ingreso", nullable = false, updatable = false)
    private LocalDate fechaIngreso;

    @Column(name = "id_usuario_actualizacion")
    private Integer idUsuarioActualizacion;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    private TagTipo tipo;

    @LastModifiedDate
    @Column(name = "ultima_modificacion")
    private LocalDateTime ultimaModificacion;
}