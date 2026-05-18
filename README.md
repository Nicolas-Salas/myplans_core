# MyPlans Core Service

Microservicio transaccional que gestiona planos y TAGs eléctricos para
la plataforma MyPlans. Alineado al modelo ER, casos de uso y requisitos
funcionales del documento de diseño.

## Cómo correr

### Requisitos previos

- **Java 17** (Eclipse Temurin recomendado).
- **Maven 3.8+** (también funciona el wrapper `./mvnw`).
- **MySQL 8** corriendo en `localhost:3306` (solo para el profile `dev`).
- Si usas **VS Code**, instala las extensiones:
  - *Extension Pack for Java* (incluye Maven, Debugger, Test Runner).
  - *Lombok Annotations Support for VS Code*. Sin esta extensión, VS
    Code muestra cientos de errores rojos en archivos que usan Lombok
    (entidades, DTOs, etc.) aunque la app **compile y corra sin
    problemas**. Ver la sección de troubleshooting más abajo.

### Comandos

```bash
cd core
mvn clean package -DskipTests
java -jar -Dspring.profiles.active=dev target/core-1.0.0.jar
```

Profiles:
- `dev` (default): MySQL local en `localhost:3306/db_myplans_core`.
- `h2test`: H2 en memoria, útil para tests e integración rápida.

## Troubleshooting

### "Problems: 56" en VS Code, pero la app SÍ compila y corre

**Causa:** falta la extensión *Lombok Annotations Support* en VS Code.
Sin Lombok, VS Code no entiende que `@Getter`, `@Setter`, `@Builder`,
`@RequiredArgsConstructor` generan métodos en tiempo de compilación, y
marca como error cualquier referencia a esos getters/setters/builders.

Los archivos generados por MapStruct (`PlanoMapperImpl.java`,
`TagMapperImpl.java` en `target/generated-sources/`) también suelen
mostrar `9+` problemas porque dependen de los métodos generados por
Lombok.

**Solución:**
1. Instalar la extensión *Lombok Annotations Support for VS Code*
   (publisher: GabrielBB / Microsoft, según versión).
2. Reiniciar VS Code: `Ctrl+Shift+P` → "Java: Clean Java Language
   Server Workspace" → confirmar reinicio.
3. Después del reinicio, los "Problems 56" deberían bajar a 0.

**Verificación de que la app SÍ funciona pese a los problemas del IDE:**

```bash
cd core
mvn clean package -DskipTests
```

Si el build dice `BUILD SUCCESS`, el código es correcto. Los errores
del IDE son únicamente cosméticos y no afectan la ejecución.

### Otros equipos pueden ver problemas distintos según OS

- **Windows + JDK distinto al 17**: si tu `JAVA_HOME` apunta a otra
  versión, el build falla con errores raros. Verifica con `java -version`.
  Si tienes varias versiones, configura `JAVA_HOME` al JDK 17 antes
  de ejecutar Maven.
- **macOS con M1/M2**: usar un JDK ARM-nativo (Temurin para
  `aarch64`). El JDK x86 funciona via Rosetta pero más lento.
- **Linux**: normalmente "just works" si el JDK 17 está en PATH.

### Errores de CORS en el frontend

Si el frontend ve este error en la consola:

```
The 'Access-Control-Allow-Origin' header contains multiple values
'http://localhost:5173, http://localhost:5173', but only one is allowed.
```

**Causa:** alguno de los microservicios downstream estaba agregando
su propio header CORS, sumándose al que ya agrega el gateway. El
navegador rechaza headers CORS duplicados, aunque traigan el mismo
valor.

**Solución (ya aplicada en este repo):** el Core, el Audit y el Auth
tienen CORS **deshabilitado** en sus `SecurityConfig` (`.cors(disable)`).
CORS es responsabilidad del **API Gateway** únicamente — él es el
punto de entrada del frontend, los demás servicios viven en la red
interna y nunca son llamados directamente por un navegador en
producción.

Si en un dev environment alguien necesita llamar al Core directo desde
un navegador (por ejemplo, para probar el Swagger desde otro origen),
puede levantarlo con un perfil específico que reactive CORS. Por ahora
no es necesario.

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

**CU-15 (consultar historial)** se sirve desde el microservicio Audit
independiente. El Core **sí** participa: en cada cambio de estado
(`PATCH /api/v1/tags/{idTag}/estado`) publica un evento al Audit
con `AuditServiceClient`. Ver sección "Integración con el Audit Service"
más abajo.

### 4. Integración con el Audit Service

`TagServiceImpl.updateEstado()` publica un evento al microservicio
Audit en cada cambio de estado de TAG, cumpliendo RF-19 (registro
automático), RF-20 (historial completo) y CU-15.

**Comunicación punto-a-punto, NO via gateway:**
- El gateway sirve al frontend (tráfico humano con JWT de usuario).
- El Core llama al Audit directo en la red interna usando el header
  `X-Internal-Token` (secreto compartido entre Core y Audit).
- En producción, gateway y backends están en la misma red privada;
  el Audit no está expuesto al internet.

**Política configurable cuando el Audit no responde** (`audit.enforce-strict`):

| Valor | Comportamiento | Cuándo usar |
| ----- | --------------- | ----------- |
| `false` (default) | Loguea WARN y sigue. El cambio del TAG persiste. | Disponibilidad operativa: los operadores en terreno no quedan bloqueados si Audit se cae. |
| `true` | Lanza `AuditServiceUnavailableException`, transacción del Core hace **rollback**, endpoint responde 503. | Integridad forense: ningún cambio se persiste sin su registro de auditoría. |

Se cambia con variable de entorno `AUDIT_ENFORCE_STRICT=true`. Sin
reiniciar todo el cluster: solo el Core.

**Configuración relevante en `application.yml`:**

```yaml
audit:
  service:
    uri: ${AUDIT_SERVICE_URI:http://localhost:8082}
    internal-token: ${AUDIT_INTERNAL_TOKEN:dev-internal-token-please-change-in-prod}
    timeout-ms: ${AUDIT_TIMEOUT_MS:2000}
  enforce-strict: ${AUDIT_ENFORCE_STRICT:false}
```

El `internal-token` **debe coincidir exactamente** con el del Audit.

### 5. Manejo centralizado de errores

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

### 6. Storage abstracto (S3-ready)

`StorageService` interface + `LocalStorageService` impl en disco. Para
producción, basta con crear una `S3StorageService` con el SDK de AWS y
marcarla con `@Primary` o un profile distinto — el resto del código no
cambia. Se evitó añadir el SDK pesado de AWS al pom mientras no se
necesite.

### 7. Tests de integración

`SecurityIntegrationTest` levanta toda la app con H2 y valida 9
escenarios end-to-end: 401 sin token, 401 token inválido, 401 token
expirado, 200 con token ADMIN, 403 con OPERADOR, 201 creando plano,
400 sin campos obligatorios, 400 body vacío en PUT, 409 al exportar
plano no cerrado.

Adicionalmente se ejecutó un **smoke test manual** con curl y JWTs
reales que cubre **26 escenarios** y los 12 casos de uso del Core.
Todos pasaron.

### 8. Otros arreglos

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

1. **Comunicación async con el Audit (RabbitMQ/Kafka)**: actualmente
   Core → Audit es HTTP síncrono punto-a-punto. Para mayor resiliencia
   (eventos que sobreviven a caídas del Audit), considerar publicar a
   un broker. El cambio queda contenido en `AuditServiceClient`.
2. **S3StorageService**: agregar dependencia
   `software.amazon.awssdk:s3` y crear la implementación. El resto del
   código no cambia.
3. **Docker**: el JAR ya es self-contained. Un Dockerfile simple basado
   en `eclipse-temurin:17-jre` lo deja listo.
