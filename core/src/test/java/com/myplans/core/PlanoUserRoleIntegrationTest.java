package com.myplans.core;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PF-008 — ROLE_USER puede listar planos (el listado no está restringido por usuario).
 *
 * Aclaración de implementación: el GET /api/v1/planos no lleva @PreAuthorize,
 * por lo que cualquier usuario autenticado puede ver todos los planos.
 * PF-008 verifica que ROLE_USER no recibe 403 y que obtiene la lista paginada.
 * La restricción de "solo sus planos" aplica en el frontend mediante el contexto
 * de usuario, no en el backend actual.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("h2test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@TestPropertySource(properties = {
        "audit.service.uri=http://localhost:65000",
        "audit.service.timeout-ms=300",
        "audit.enforce-strict=false"
})
class PlanoUserRoleIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    private HttpHeaders headers(String token) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.setBearerAuth(token);
        return h;
    }

    private void crearPlano(String adminToken, String nombre) {
        restTemplate.getRestTemplate().setRequestFactory(new HttpComponentsClientHttpRequestFactory());
        String body = String.format("{\"nombre\":\"%s\",\"formulario\":\"F\"}", nombre);
        ResponseEntity<Map> resp = restTemplate.postForEntity(
                "/api/v1/planos", new HttpEntity<>(body, headers(adminToken)), Map.class);
        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
    }

    // PF-008: ROLE_USER puede hacer GET /planos — no recibe 403

    @Test
    void givenRoleUser_whenListPlanos_thenReturn200NotForbidden() {
        String adminToken = TestJwtHelper.tokenFor("admin@test.com", 1, List.of("ROLE_ADMIN"));
        String userToken  = TestJwtHelper.tokenFor("op@test.com",    2, List.of("ROLE_USER"));
        crearPlano(adminToken, "Plano Admin A");

        ResponseEntity<Map> resp = restTemplate.exchange(
                "/api/v1/planos",
                HttpMethod.GET,
                new HttpEntity<>(headers(userToken)),
                Map.class);

        assertEquals(HttpStatus.OK, resp.getStatusCode(),
                "ROLE_USER debe poder listar planos (no recibir 403)");
        assertTrue(resp.getBody().containsKey("content"),
                "La respuesta debe tener estructura paginada con 'content'");
    }

    @Test
    void givenRoleUser_whenListPlanos_thenResponseIsPagedStructure() {
        String adminToken = TestJwtHelper.tokenFor("admin@test.com", 1, List.of("ROLE_ADMIN"));
        String userToken  = TestJwtHelper.tokenFor("op@test.com",    2, List.of("ROLE_USER"));
        crearPlano(adminToken, "Plano Test Paginado");

        ResponseEntity<Map> resp = restTemplate.exchange(
                "/api/v1/planos?page=0&size=10",
                HttpMethod.GET,
                new HttpEntity<>(headers(userToken)),
                Map.class);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        Map<?, ?> body = resp.getBody();
        assertTrue(body.containsKey("content"),       "Debe tener 'content'");
        assertTrue(body.containsKey("totalElements"), "Debe tener 'totalElements'");
        assertTrue(body.containsKey("totalPages"),    "Debe tener 'totalPages'");
    }

    @Test
    void givenRoleAuditor_whenListPlanos_thenReturn200() {
        String adminToken   = TestJwtHelper.tokenFor("admin@test.com", 1, List.of("ROLE_ADMIN"));
        String auditorToken = TestJwtHelper.tokenFor("aud@test.com",   3, List.of("ROLE_AUDITOR"));
        crearPlano(adminToken, "Plano Para Auditor");

        ResponseEntity<Map> resp = restTemplate.exchange(
                "/api/v1/planos",
                HttpMethod.GET,
                new HttpEntity<>(headers(auditorToken)),
                Map.class);

        assertEquals(HttpStatus.OK, resp.getStatusCode(),
                "ROLE_AUDITOR también debe poder listar planos");
    }

    @Test
    void givenRoleUser_whenGetPlanoById_thenReturn200() {
        String adminToken = TestJwtHelper.tokenFor("admin@test.com", 1, List.of("ROLE_ADMIN"));
        String userToken  = TestJwtHelper.tokenFor("op@test.com",    2, List.of("ROLE_USER"));

        restTemplate.getRestTemplate().setRequestFactory(new HttpComponentsClientHttpRequestFactory());
        ResponseEntity<Map> created = restTemplate.postForEntity(
                "/api/v1/planos",
                new HttpEntity<>("{\"nombre\":\"Plano Operador\",\"formulario\":\"F\"}",
                        headers(adminToken)),
                Map.class);
        Integer idPlano = (Integer) created.getBody().get("idPlano");

        ResponseEntity<Map> resp = restTemplate.exchange(
                "/api/v1/planos/" + idPlano,
                HttpMethod.GET,
                new HttpEntity<>(headers(userToken)),
                Map.class);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(idPlano, resp.getBody().get("idPlano"));
    }

    @Test
    void givenRoleUser_whenFilterByStatus_thenReturn200() {
        String userToken = TestJwtHelper.tokenFor("op@test.com", 2, List.of("ROLE_USER"));

        ResponseEntity<Map> resp = restTemplate.exchange(
                "/api/v1/planos?status=ABIERTO",
                HttpMethod.GET,
                new HttpEntity<>(headers(userToken)),
                Map.class);

        assertEquals(HttpStatus.OK, resp.getStatusCode(),
                "ROLE_USER puede filtrar planos por estado");
    }

    // Restricciones que SÍ aplican a ROLE_USER

    @Test
    void givenRoleUser_whenCreatePlano_thenReturn403() {
        String userToken = TestJwtHelper.tokenFor("op@test.com", 2, List.of("ROLE_USER"));
        restTemplate.getRestTemplate().setRequestFactory(new HttpComponentsClientHttpRequestFactory());
        ResponseEntity<Map> resp = restTemplate.postForEntity(
                "/api/v1/planos",
                new HttpEntity<>("{\"nombre\":\"Intento\",\"formulario\":\"F\"}",
                        headers(userToken)),
                Map.class);

        assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode(),
                "ROLE_USER no puede crear planos");
    }

    @Test
    void givenRoleUser_whenValidarPlano_thenReturn403() {
        String adminToken = TestJwtHelper.tokenFor("admin@test.com", 1, List.of("ROLE_ADMIN"));
        String userToken  = TestJwtHelper.tokenFor("op@test.com",    2, List.of("ROLE_USER"));
        crearPlano(adminToken, "Plano A Validar");

        restTemplate.getRestTemplate().setRequestFactory(new HttpComponentsClientHttpRequestFactory());
        ResponseEntity<Map> planos = restTemplate.exchange(
                "/api/v1/planos", HttpMethod.GET,
                new HttpEntity<>(headers(adminToken)), Map.class);
        List<?> content = (List<?>) planos.getBody().get("content");
        Integer idPlano = (Integer) ((Map<?, ?>) content.get(0)).get("idPlano");

        ResponseEntity<Map> resp = restTemplate.exchange(
                "/api/v1/planos/" + idPlano + "/validar",
                HttpMethod.PUT,
                new HttpEntity<>(headers(userToken)),
                Map.class);

        assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode(),
                "ROLE_USER no puede validar planos");
    }
}
