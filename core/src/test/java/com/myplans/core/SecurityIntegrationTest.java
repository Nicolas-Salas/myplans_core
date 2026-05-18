package com.myplans.core;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("h2test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class SecurityIntegrationTest {

        @Autowired
        private TestRestTemplate restTemplate;

        @Test
        void givenNoToken_whenAccessProtected_thenReturn401WithMessage() {
                ResponseEntity<Map> response = restTemplate.exchange(
                                "/api/v1/planos",
                                HttpMethod.GET,
                                new HttpEntity<>(new HttpHeaders()),
                                Map.class);

                assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
                assertNotNull(response.getBody().get("message"));
        }

        @Test
        void givenInvalidToken_whenAccessProtected_thenReturn401WithTokenMessage() {
                HttpHeaders h = new HttpHeaders();
                h.setBearerAuth("token-falso");
                ResponseEntity<Map> response = restTemplate.exchange(
                                "/api/v1/planos",
                                HttpMethod.GET,
                                new HttpEntity<>(h),
                                Map.class);

                assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
                String msg = (String) response.getBody().get("message");
                assertTrue(msg.toLowerCase().contains("token")
                                || msg.toLowerCase().contains("sesión")
                                || msg.toLowerCase().contains("inicia"));
        }

        @Test
        void givenExpiredToken_whenAccessProtected_thenReturn401WithExpiredMessage() {
                String expired = TestJwtHelper.expiredTokenFor("a@test.com", 1, List.of("ROLE_ADMIN"));
                HttpHeaders h = new HttpHeaders();
                h.setBearerAuth(expired);
                ResponseEntity<Map> response = restTemplate.exchange(
                                "/api/v1/planos",
                                HttpMethod.GET,
                                new HttpEntity<>(h),
                                Map.class);

                assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
                String msg = (String) response.getBody().get("message");
                assertTrue(msg.toLowerCase().contains("expirado")
                                || msg.toLowerCase().contains("sesión")
                                || msg.toLowerCase().contains("inicia"));
        }

        @Test
        void givenValidAdminToken_whenListPlanos_thenReturn200() {
                String token = TestJwtHelper.tokenFor("admin@test.com", 1, List.of("ROLE_ADMIN"));
                HttpHeaders h = new HttpHeaders();
                h.setBearerAuth(token);
                ResponseEntity<Map> response = restTemplate.exchange(
                                "/api/v1/planos",
                                HttpMethod.GET,
                                new HttpEntity<>(h),
                                Map.class);
                assertEquals(HttpStatus.OK, response.getStatusCode());
        }

        @Test
        void givenOperadorToken_whenCreatePlano_thenReturn403() {
                String token = TestJwtHelper.tokenFor("op@test.com", 2, List.of("ROLE_OPERADOR"));
                HttpHeaders h = new HttpHeaders();
                h.setBearerAuth(token);
                h.setContentType(MediaType.APPLICATION_JSON);
                String body = """
                                { "nombre": "Plano 1", "formulario": "PRE-ELE" }
                                """;
                ResponseEntity<Map> response = restTemplate.exchange(
                                "/api/v1/planos",
                                HttpMethod.POST,
                                new HttpEntity<>(body, h),
                                Map.class);

                assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
                assertNotNull(response.getBody().get("message"));
        }

        @Test
        void givenAdmin_whenCreatePlano_thenReturn201() {
                String token = TestJwtHelper.tokenFor("admin@test.com", 1, List.of("ROLE_ADMIN"));
                HttpHeaders h = new HttpHeaders();
                h.setBearerAuth(token);
                h.setContentType(MediaType.APPLICATION_JSON);
                String body = """
                                {
                                "nombre": "Plano de prueba",
                                "formulario": "PRE-ELE-YL",
                                "alcance": "SKIC",
                                "subsistema": "Subestación A",
                                "codigoPlano": "P-001",
                                "rev": "0"
                                }
                                """;
                ResponseEntity<Map> response = restTemplate.postForEntity(
                                "/api/v1/planos", new HttpEntity<>(body, h), Map.class);

                assertEquals(HttpStatus.CREATED, response.getStatusCode());
                assertEquals("Plano de prueba", response.getBody().get("nombre"));
                assertEquals("ABIERTO", response.getBody().get("status"));
        }

        @Test
        void givenAdmin_whenCreatePlanoWithoutRequiredField_thenReturn400() {
                String token = TestJwtHelper.tokenFor("admin@test.com", 1, List.of("ROLE_ADMIN"));
                HttpHeaders h = new HttpHeaders();
                h.setBearerAuth(token);
                h.setContentType(MediaType.APPLICATION_JSON);
                String body = """
                                { "alcance": "SKIC" }
                                """;
                ResponseEntity<Map> response = restTemplate.postForEntity(
                                "/api/v1/planos", new HttpEntity<>(body, h), Map.class);

                assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                String msg = (String) response.getBody().get("message");
                assertNotNull(msg);
                assertTrue(msg.toLowerCase().contains("obligatorio"));
        }

        @Test
        void givenAdmin_whenUpdatePlanoWithEmptyBody_thenReturn400WithFieldsList() {
                String token = TestJwtHelper.tokenFor("admin@test.com", 1, List.of("ROLE_ADMIN"));
                HttpHeaders h = new HttpHeaders();
                h.setBearerAuth(token);
                h.setContentType(MediaType.APPLICATION_JSON);
                ResponseEntity<Map> created = restTemplate.postForEntity(
                                "/api/v1/planos",
                                new HttpEntity<>("{\"nombre\":\"X\",\"formulario\":\"F\"}", h),
                                Map.class);
                Integer id = (Integer) created.getBody().get("idPlano");

                ResponseEntity<Map> updated = restTemplate.exchange(
                                "/api/v1/planos/" + id,
                                HttpMethod.PUT,
                                new HttpEntity<>("{}", h),
                                Map.class);
                assertEquals(HttpStatus.BAD_REQUEST, updated.getStatusCode());
                String msg = (String) updated.getBody().get("message");
                assertTrue(msg.toLowerCase().contains("campo"));
        }

        @Test
        void givenAdmin_whenExportPlanoNotClosed_thenReturn409() {
                String token = TestJwtHelper.tokenFor("admin@test.com", 1, List.of("ROLE_ADMIN"));
                HttpHeaders h = new HttpHeaders();
                h.setBearerAuth(token);
                h.setContentType(MediaType.APPLICATION_JSON);
                ResponseEntity<Map> created = restTemplate.postForEntity(
                                "/api/v1/planos",
                                new HttpEntity<>("{\"nombre\":\"X\",\"formulario\":\"F\"}", h),
                                Map.class);
                Integer id = (Integer) created.getBody().get("idPlano");

                ResponseEntity<Map> exported = restTemplate.exchange(
                                "/api/v1/planos/" + id + "/export",
                                HttpMethod.GET,
                                new HttpEntity<>(h),
                                Map.class);
                assertEquals(HttpStatus.CONFLICT, exported.getStatusCode());
        }
}