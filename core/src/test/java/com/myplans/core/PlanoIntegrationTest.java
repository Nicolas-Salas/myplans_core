package com.myplans.core;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.LinkedMultiValueMap;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PF-007, PF-009, PF-010, PF-011, PF-012, PF-015, PF-016, PF-017
 * PS-008, PS-009 — RBAC por rol
 * PI-008 — Core persiste TAG aunque Audit esté offline
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("h2test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@TestPropertySource(properties = {
        "audit.service.uri=http://localhost:65000",
        "audit.service.timeout-ms=300",
        "audit.enforce-strict=false"
})
class PlanoIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    private HttpHeaders authHeaders(String token) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.setBearerAuth(token);
        return h;
    }

    private Integer crearPlano(String token) {
        String body = """
                {
                  "nombre": "Plano Test Integración",
                  "formulario": "PRE-ELE-YL",
                  "alcance": "SKIC",
                  "subsistema": "Subestación A",
                  "codigoPlano": "P-TEST-001",
                  "rev": "0"
                }
                """;
        ResponseEntity<Map> resp = restTemplate.postForEntity(
                "/api/v1/planos",
                new HttpEntity<>(body, authHeaders(token)),
                Map.class);
        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
        return (Integer) resp.getBody().get("idPlano");
    }

    private Integer subirTagsExcel(Integer idPlano, String token) {
        restTemplate.getRestTemplate().setRequestFactory(new HttpComponentsClientHttpRequestFactory());
        HttpHeaders mpHeaders = new HttpHeaders();
        mpHeaders.setBearerAuth(token);
        mpHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);

        LinkedMultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("file", new ByteArrayResource(TestExcelHelper.xlsxWithOneTag("TAG-001", "EQUIPO")) {
            @Override
            public String getFilename() { return "tags.xlsx"; }
        });

        ResponseEntity<List> resp = restTemplate.postForEntity(
                "/api/v1/planos/" + idPlano + "/tags/excel",
                new HttpEntity<>(form, mpHeaders),
                List.class);
        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
        return (Integer) ((Map) resp.getBody().get(0)).get("idTag");
    }

    // PF-009: Crear plano (Paso 1 precarga)

    @Test
    void givenAdmin_whenCreatePlano_thenReturn201WithABIERTO() {
        String token = TestJwtHelper.tokenFor("admin@test.com", 1, List.of("ROLE_ADMIN"));
        String body = """
                {
                  "nombre": "Plano de Subestación Norte",
                  "formulario": "PRE-ELE-YL",
                  "alcance": "SKIC",
                  "subsistema": "Subestación A",
                  "codigoPlano": "P-001",
                  "rev": "0"
                }
                """;
        ResponseEntity<Map> resp = restTemplate.postForEntity(
                "/api/v1/planos", new HttpEntity<>(body, authHeaders(token)), Map.class);

        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
        assertNotNull(resp.getBody().get("idPlano"));
        assertEquals("Plano de Subestación Norte", resp.getBody().get("nombre"));
        assertEquals("ABIERTO", resp.getBody().get("status"),
                "Un plano recién creado debe iniciar en estado ABIERTO");
    }

    @Test
    void givenAdminCreatesPlano_whenGetById_thenReturnPlano() {
        String token = TestJwtHelper.tokenFor("admin@test.com", 1, List.of("ROLE_ADMIN"));
        Integer idPlano = crearPlano(token);

        ResponseEntity<Map> resp = restTemplate.exchange(
                "/api/v1/planos/" + idPlano,
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)),
                Map.class);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(idPlano, resp.getBody().get("idPlano"));
        assertEquals("Plano Test Integración", resp.getBody().get("nombre"));
    }

    @Test
    void givenNombreMissing_whenCreatePlano_thenReturn400WithMessage() {
        String token = TestJwtHelper.tokenFor("admin@test.com", 1, List.of("ROLE_ADMIN"));
        String body = """
                { "formulario": "PRE-ELE" }
                """;
        ResponseEntity<Map> resp = restTemplate.postForEntity(
                "/api/v1/planos", new HttpEntity<>(body, authHeaders(token)), Map.class);

        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        String msg = (String) resp.getBody().get("message");
        assertNotNull(msg);
        assertTrue(msg.toLowerCase().contains("obligatorio") || msg.toLowerCase().contains("nombre"),
                "El mensaje debe indicar campo obligatorio, fue: " + msg);
    }

    // PS-008/PS-009: RBAC — solo ROLE_ADMIN puede crear plano

    @Test
    void givenRoleUser_whenCreatePlano_thenReturn403() {
        String token = TestJwtHelper.tokenFor("op@test.com", 2, List.of("ROLE_USER"));
        String body = """
                { "nombre": "Plano Operador", "formulario": "PRE-ELE" }
                """;
        ResponseEntity<Map> resp = restTemplate.postForEntity(
                "/api/v1/planos", new HttpEntity<>(body, authHeaders(token)), Map.class);

        assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
        assertNotNull(resp.getBody().get("message"));
    }

    @Test
    void givenRoleAuditor_whenCreatePlano_thenReturn403() {
        String token = TestJwtHelper.tokenFor("aud@test.com", 3, List.of("ROLE_AUDITOR"));
        String body = """
                { "nombre": "Plano Auditor", "formulario": "PRE-ELE" }
                """;
        ResponseEntity<Map> resp = restTemplate.postForEntity(
                "/api/v1/planos", new HttpEntity<>(body, authHeaders(token)), Map.class);

        assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
    }

    // PF-007: Dashboard paginado

    @Test
    void givenAdmin_whenListPlanos_thenReturnPagedResponse() {
        String token = TestJwtHelper.tokenFor("admin@test.com", 1, List.of("ROLE_ADMIN"));
        crearPlano(token);

        ResponseEntity<Map> resp = restTemplate.exchange(
                "/api/v1/planos?page=0&size=10",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)),
                Map.class);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertTrue(resp.getBody().containsKey("content"), "La respuesta debe tener campo 'content'");
        assertTrue(resp.getBody().containsKey("totalElements"), "La respuesta debe tener 'totalElements'");
        List<?> content = (List<?>) resp.getBody().get("content");
        assertFalse(content.isEmpty(), "Debe haber al menos un plano en la lista");
    }

    @Test
    void givenNoToken_whenListPlanos_thenReturn401() {
        ResponseEntity<Map> resp = restTemplate.exchange(
                "/api/v1/planos",
                HttpMethod.GET,
                new HttpEntity<>(new HttpHeaders()),
                Map.class);

        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
        assertNotNull(resp.getBody().get("message"));
    }

    // PF-011: Cargar TAGs desde Excel (Paso 3 precarga)

    @Test
    void givenAdmin_whenUploadTagsExcel_thenReturn201WithTags() {
        String token = TestJwtHelper.tokenFor("admin@test.com", 1, List.of("ROLE_ADMIN"));
        Integer idPlano = crearPlano(token);
        Integer idTag = subirTagsExcel(idPlano, token);

        assertNotNull(idTag, "Debe retornarse el ID del TAG creado");
    }

    @Test
    void givenTagCreated_whenGetTagsByPlano_thenReturnList() {
        String token = TestJwtHelper.tokenFor("admin@test.com", 1, List.of("ROLE_ADMIN"));
        Integer idPlano = crearPlano(token);
        subirTagsExcel(idPlano, token);

        ResponseEntity<List> resp = restTemplate.exchange(
                "/api/v1/planos/" + idPlano + "/tags",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)),
                List.class);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertFalse(resp.getBody().isEmpty(), "Debe haber al menos un TAG");
        Map<?, ?> tag = (Map<?, ?>) resp.getBody().get(0);
        assertEquals("TAG-001", tag.get("codigo"));
        assertEquals("PENDIENTE", tag.get("estadoActual"),
                "Un TAG recién cargado debe estar en PENDIENTE");
    }

    // PF-014: Cambiar estado de TAG

    @Test
    void givenTag_whenUpdateEstadoToAprobado_thenReturn200() {
        restTemplate.getRestTemplate().setRequestFactory(new HttpComponentsClientHttpRequestFactory());
        String token = TestJwtHelper.tokenFor("admin@test.com", 1, List.of("ROLE_ADMIN"));
        Integer idPlano = crearPlano(token);
        Integer idTag = subirTagsExcel(idPlano, token);

        ResponseEntity<Map> resp = restTemplate.exchange(
                "/api/v1/tags/" + idTag + "/estado",
                HttpMethod.PATCH,
                new HttpEntity<>("{\"estadoNuevo\":\"APROBADO\"}", authHeaders(token)),
                Map.class);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals("APROBADO", resp.getBody().get("estadoActual"));
    }

    @Test
    void givenTag_whenUpdateEstadoToObservado_thenReturn200() {
        restTemplate.getRestTemplate().setRequestFactory(new HttpComponentsClientHttpRequestFactory());
        String token = TestJwtHelper.tokenFor("admin@test.com", 1, List.of("ROLE_ADMIN"));
        Integer idPlano = crearPlano(token);
        Integer idTag = subirTagsExcel(idPlano, token);

        ResponseEntity<Map> resp = restTemplate.exchange(
                "/api/v1/tags/" + idTag + "/estado",
                HttpMethod.PATCH,
                new HttpEntity<>("{\"estadoNuevo\":\"OBSERVADO\",\"comentario\":\"Falla detectada\"}", authHeaders(token)),
                Map.class);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals("OBSERVADO", resp.getBody().get("estadoActual"));
    }

    @Test
    void givenNoToken_whenUpdateTag_thenReturn401() {
        restTemplate.getRestTemplate().setRequestFactory(new HttpComponentsClientHttpRequestFactory());
        ResponseEntity<Map> resp = restTemplate.exchange(
                "/api/v1/tags/1/estado",
                HttpMethod.PATCH,
                new HttpEntity<>("{\"estadoNuevo\":\"APROBADO\"}", new HttpHeaders()),
                Map.class);

        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
    }

    // PF-015: Validar plano (→ VALIDADO)

    @Test
    void givenAuditor_whenValidarPlano_thenReturn200WithVALIDADO() {
        String token = TestJwtHelper.tokenFor("sup@test.com", 3, List.of("ROLE_SUPERVISOR"));
        String adminToken = TestJwtHelper.tokenFor("admin@test.com", 1, List.of("ROLE_ADMIN"));
        Integer idPlano = crearPlano(adminToken);

        ResponseEntity<Map> resp = restTemplate.exchange(
                "/api/v1/planos/" + idPlano + "/validar",
                HttpMethod.PUT,
                new HttpEntity<>(authHeaders(token)),
                Map.class);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals("VALIDADO", resp.getBody().get("status"),
                "El plano debe pasar a estado VALIDADO");
    }

    @Test
    void givenRoleUser_whenValidarPlano_thenReturn403() {
        String userToken = TestJwtHelper.tokenFor("op@test.com", 2, List.of("ROLE_USER"));
        String adminToken = TestJwtHelper.tokenFor("admin@test.com", 1, List.of("ROLE_ADMIN"));
        Integer idPlano = crearPlano(adminToken);

        ResponseEntity<Map> resp = restTemplate.exchange(
                "/api/v1/planos/" + idPlano + "/validar",
                HttpMethod.PUT,
                new HttpEntity<>(authHeaders(userToken)),
                Map.class);

        assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
    }

    // PI-009: Exportar plano no cerrado → endpoint en Reports service, no en Core

    @Disabled("El endpoint /export pertenece al Reports microservice, no a Core")
    @Test
    void givenPlanoABIERTO_whenExport_thenReturn409() {
        String token = TestJwtHelper.tokenFor("admin@test.com", 1, List.of("ROLE_ADMIN"));
        Integer idPlano = crearPlano(token);

        ResponseEntity<Map> resp = restTemplate.exchange(
                "/api/v1/planos/" + idPlano + "/export",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)),
                Map.class);

        assertEquals(HttpStatus.CONFLICT, resp.getStatusCode());
    }

    // Edición de plano

    @Test
    void givenAdmin_whenUpdatePlanoWithNombre_thenReturn200() {
        restTemplate.getRestTemplate().setRequestFactory(new HttpComponentsClientHttpRequestFactory());
        String token = TestJwtHelper.tokenFor("admin@test.com", 1, List.of("ROLE_ADMIN"));
        Integer idPlano = crearPlano(token);

        ResponseEntity<Map> resp = restTemplate.exchange(
                "/api/v1/planos/" + idPlano,
                HttpMethod.PUT,
                new HttpEntity<>("{\"nombre\":\"Plano Actualizado\"}", authHeaders(token)),
                Map.class);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals("Plano Actualizado", resp.getBody().get("nombre"));
    }

    // Detalle de TAG individual

    @Test
    void givenTagCreated_whenGetById_thenReturnTag() {
        String token = TestJwtHelper.tokenFor("admin@test.com", 1, List.of("ROLE_ADMIN"));
        Integer idPlano = crearPlano(token);
        Integer idTag = subirTagsExcel(idPlano, token);

        ResponseEntity<Map> resp = restTemplate.exchange(
                "/api/v1/tags/" + idTag,
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)),
                Map.class);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(idTag, resp.getBody().get("idTag"));
        assertEquals("TAG-001", resp.getBody().get("codigo"));
    }

    @Test
    void givenTagNotFound_whenGetById_thenReturn404() {
        String token = TestJwtHelper.tokenFor("admin@test.com", 1, List.of("ROLE_ADMIN"));

        ResponseEntity<Map> resp = restTemplate.exchange(
                "/api/v1/tags/99999",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)),
                Map.class);

        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
    }
}
