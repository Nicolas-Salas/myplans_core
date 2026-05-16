# MyPlans Core Service

Microservicio transaccional que gestiona planos y TAGs eléctricos para
la plataforma MyPlans. Alineado al modelo ER, casos de uso y requisitos
funcionales del documento de diseño.

## Cómo correr

```bash
cd core
mvn clean package -DskipTests
java -jar -Dspring.profiles.active=dev target/core-1.0.0.jar
```

Profiles:
- `dev` (default): MySQL local en `localhost:3306/db_myplans_core`.
- `h2test`: H2 en memoria, útil para tests e integración rápida.

## Cambios aplicados sobre el código original

### 1. Modelo de datos alineado al doc ER

La entidad `Plano` original tenía: `codigo`, `nombre`, `descripcion`,
`urlPdf`, estados `PENDIENTE/VALIDADO/CERRADO`. El doc ER pide:
`nombre`, `formulario`, `urlS3`, `alcance`, `subsistema`, `status` en
`ABIERTO/VALIDADO/CERRADO`, `codigoPlano`, `rev`, `observaciones`,
`responsable`, `fechaFirma`, `nroPaginas`, `fechaCreacion`.

`Plano.java` y `Tag.java` se reescribieron para reflejar exactamente
el diccionario de datos del doc, incluyendo el enum `ABIERTO` correcto.

### 2. Compatibilidad JWT con el microservicio Auth

**Bug crítico encontrado:** el JWT del Auth se firma con
`Decoders.BASE64.decode(secret)` → HS384, pero el Core lo validaba
con `secret.getBytes()` → HS512. Cuando un token del Auth llegue al
Core a través del API Gateway, el Core rechazaría todos.

**Arreglo:** `JwtUtil` del Core ahora usa BASE64 decode también.
Verificado con tokens reales generados por el helper
`TestJwtHelper.java` (que replica exactamente el flujo del Auth).

Adicionalmente, el Core lee dos claims nuevos del JWT que se agregan
al Auth con el parche provisto (`auth-patch/JwtUtil.java`):
- `id_usuario` → para registrar `idUsuarioIngreso/idUsuarioActualizacion`.
- `roles` → para el RBAC (`hasRole("SUPERVISOR")`, etc.).

### 3. Casos de uso implementados

| CU | Endpoint | Rol | Estado |
| -- | -------- | --- | ------ |
| CU-07 RF-07 | `POST /api/v1/planos/{id}/tags/excel` | ADMIN | Carga masiva de TAGs desde Excel con validación de duplicados |
| CU-08 RF-11 | `POST /api/v1/planos` + `POST /api/v1/planos/{id}/pdf` | ADMIN | Crear plano + subir PDF (cuenta páginas con PDFBox) |
| CU-09 RF-13 | `GET /api/v1/planos/{id}` | Autenticado | Buscar plano por ID |
| CU-10 RF-12 | `GET /api/v1/planos?status&page&size` | Autenticado | Dashboard paginado, filtrable por estado |
| CU-11 RF-14 | `urlS3` en la respuesta | Autenticado | El frontend usa la URL para el visor |
| CU-12 RF-15 | `GET /api/v1/planos/{id}/tags` | Autenticado | Listar TAGs del plano |
| CU-13 RF-16 | `PATCH /api/v1/tags/{idTag}/estado` | OPERADOR/SUP/ADMIN | Cambio de estado |
| CU-14 RF-17 | (mismo endpoint) | OPERADOR/SUP/ADMIN | OBSERVADO requiere `comentario` no vacío |
| RF-18 / CU-13 alt 3b | (mismo endpoint) | SUPERVISOR/ADMIN | Revertir desde APROBADO solo Supervisor |
| CU-13 alt 3c | (mismo endpoint) | bloquea con 409 | Plano CERRADO bloquea cambios |
| CU-16 RF-23 | `PUT /api/v1/planos/{id}/validar` | SUPERVISOR/ADMIN | Validar plano (estado ABIERTO → VALIDADO) |
| CU-17 RF-24 | `PUT /api/v1/planos/{id}/cerrar` | SUPERVISOR/ADMIN | Cerrar plano (VALIDADO → CERRADO) |
| CU-18 RF-25 | `GET /api/v1/planos/{id}/export` | SUPERVISOR/ADMIN | Exportar matriz Excel (solo si CERRADO) |

CU-15 (consultar historial) **no se implementa aquí**: el doc de
arquitectura asigna el historial al microservicio de Auditoría, que
es independiente. El Core ya tiene un punto de inyección comentado en
`TagServiceImpl.updateEstado()` donde publicar el evento de auditoría
cuando el servicio Audit exista.

### 4. Manejo centralizado de errores

Mismo patrón que el Auth (sesión anterior): excepciones tipadas y un
`GlobalExceptionHandler` que devuelve un body uniforme:

```json
{
  "timestamp": "2026-05-16T13:50:36",
  "status": 409,
  "error": "Conflict",
  "message": "El TAG 'DUP-001' ya existe en este plano"
}
```

Excepciones del Core:
- `ResourceNotFoundException` → 404 (plano/tag no existe)
- `ConflictException` → 409 (estado incompatible, duplicado)
- `ForbiddenOperationException` → 403 (regla de negocio prohíbe)
- `BusinessException` → 400 (validación de negocio)
- `NoFieldsToUpdateException` → 400 (body vacío en PUT)
- `ExpiredJwtException` / `JwtException` → 401 con mensaje específico
- `AccessDeniedException` → 403 con JSON consistente
- `DataIntegrityViolationException` → 409 (último recurso)
- Fallback `Exception` → 500 ocultando stack trace al frontend

### 5. Storage abstracto (S3-ready)

`StorageService` interface + `LocalStorageService` impl en disco. Para
producción, basta con crear una `S3StorageService` con el SDK de AWS y
marcarla con `@Primary` o un profile distinto — el resto del código no
cambia. Se evitó añadir el SDK pesado de AWS al pom mientras no se
necesite.

### 6. Tests de integración

`SecurityIntegrationTest` levanta toda la app con H2 y valida 9
escenarios end-to-end: 401 sin token, 401 token inválido, 401 token
expirado, 200 con token ADMIN, 403 con OPERADOR, 201 creando plano,
400 sin campos obligatorios, 400 body vacío en PUT, 409 al exportar
plano no cerrado.

Adicionalmente se ejecutó un **smoke test manual** con curl y JWTs
reales que cubre **26 escenarios** y los 12 casos de uso del Core.
Todos pasaron.

### 7. Otros arreglos

- **pom.xml:** agregado `<parameters>true</parameters>` al
  maven-compiler-plugin (sin esto, Spring 6 falla en runtime al
  resolver `@PathVariable Long id`). Lombok y MapStruct convergen
  correctamente.
- **CoreApplication:** agregado `@EnableJpaAuditing` para que
  `@CreatedDate`/`@LastModifiedDate` funcionen (sin esto los timestamps
  quedaban null y rompían los NOT NULL).
- **PDFBox 3.x:** para contar páginas del PDF al subirlo.
- **Apache POI:** para parsear/generar Excel.
- **CORS:** configurable y abierto en dev. En prod, el API gateway
  normalmente maneja esto.
- **Actuator health endpoint** público para health checks del gateway
  y de Docker.
- **Multipart limit:** 50MB por defecto (configurable en yml).

## Estructura de la API

Todas las rutas bajo `/api/v1/`. JWT en header `Authorization: Bearer …`.

### Planos
- `POST /planos` (ADMIN) — crear plano
- `GET /planos` — dashboard paginado, filtro por status
- `GET /planos/{id}` — detalle
- `PUT /planos/{id}` (ADMIN/SUP) — actualización parcial
- `POST /planos/{id}/pdf` (ADMIN) — subir PDF
- `PUT /planos/{id}/validar` (SUP/ADMIN)
- `PUT /planos/{id}/cerrar` (SUP/ADMIN)
- `GET /planos/{id}/export` (SUP/ADMIN) — descargar matriz Excel

### TAGs
- `POST /planos/{idPlano}/tags/excel` (ADMIN) — carga masiva
- `GET /planos/{idPlano}/tags` — listar
- `GET /tags/{idTag}` — detalle
- `PATCH /tags/{idTag}/estado` (OP/SUP/ADMIN) — cambio de estado

### Swagger
Disponible en `http://localhost:8081/swagger-ui.html` (público para
facilitar pruebas y la integración con el gateway).

## Integración con el Auth

El JWT del Auth debe llevar dos claims: `id_usuario` (Integer) y
`roles` (lista con prefijo `ROLE_`). Aplica el parche en
`auth-patch/` (solo `JwtUtil.java` cambia).

Ambos servicios comparten el mismo `jwt.secret` en su `application.yml`
o, mejor, vía variable de entorno `JWT_SECRET`.

## Próximos pasos sugeridos

1. **Microservicio Audit**: cuando esté listo, completar el punto de
   inyección comentado en `TagServiceImpl.updateEstado()` para publicar
   eventos de cambio de estado y satisfacer RF-19/20 y CU-15.
2. **S3StorageService**: agregar dependencia
   `software.amazon.awssdk:s3` y crear la implementación. El resto del
   código no cambia.
3. **Docker**: el JAR ya es self-contained. Un Dockerfile simple basado
   en `eclipse-temurin:17-jre` lo deja listo.
