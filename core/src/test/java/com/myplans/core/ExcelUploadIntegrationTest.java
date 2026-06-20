package com.myplans.core;

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
 * PF-011 — Paso 3 precarga: subir Excel con TAGs → 201
 * PF-012 — Excel con formato incorrecto → 400 con mensaje descriptivo
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("h2test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@TestPropertySource(properties = {
        "audit.service.uri=http://localhost:65000",
        "audit.service.timeout-ms=300",
        "audit.enforce-strict=false"
})
class ExcelUploadIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    private Integer crearPlano(String token) {
        restTemplate.getRestTemplate().setRequestFactory(new HttpComponentsClientHttpRequestFactory());
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.setBearerAuth(token);
        ResponseEntity<Map> resp = restTemplate.postForEntity(
                "/api/v1/planos",
                new HttpEntity<>("{\"nombre\":\"P\",\"formulario\":\"F\"}", h),
                Map.class);
        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
        return (Integer) resp.getBody().get("idPlano");
    }

    private ResponseEntity<List> subirExcel(Integer idPlano, String token, byte[] xlsx,
                                            String filename) {
        restTemplate.getRestTemplate().setRequestFactory(new HttpComponentsClientHttpRequestFactory());
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(token);
        h.setContentType(MediaType.MULTIPART_FORM_DATA);
        LinkedMultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("file", new ByteArrayResource(xlsx) {
            @Override public String getFilename() { return filename; }
        });
        return restTemplate.postForEntity(
                "/api/v1/planos/" + idPlano + "/tags/excel",
                new HttpEntity<>(form, h),
                List.class);
    }

    private ResponseEntity<Map> subirExcelExpectError(Integer idPlano, String token,
                                                       byte[] xlsx, String filename) {
        restTemplate.getRestTemplate().setRequestFactory(new HttpComponentsClientHttpRequestFactory());
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(token);
        h.setContentType(MediaType.MULTIPART_FORM_DATA);
        LinkedMultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("file", new ByteArrayResource(xlsx) {
            @Override public String getFilename() { return filename; }
        });
        return restTemplate.postForEntity(
                "/api/v1/planos/" + idPlano + "/tags/excel",
                new HttpEntity<>(form, h),
                Map.class);
    }

    // PF-011: Excel válido → 201 con TAGs en estado PENDIENTE

    @Test
    void givenValidExcel_whenUploadTags_thenReturn201WithPendingTags() {
        String token = TestJwtHelper.tokenFor("admin@test.com", 1, List.of("ROLE_ADMIN"));
        Integer idPlano = crearPlano(token);

        ResponseEntity<List> resp = subirExcel(
                idPlano, token,
                TestExcelHelper.xlsxWithOneTag("TAG-A01", "EQUIPO"),
                "tags.xlsx");

        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
        assertFalse(resp.getBody().isEmpty(), "Debe retornarse al menos un TAG");
        Map<?, ?> tag = (Map<?, ?>) resp.getBody().get(0);
        assertEquals("TAG-A01", tag.get("codigo"));
        assertEquals("PENDIENTE", tag.get("estadoActual"),
                "Un TAG recién cargado debe iniciar en estado PENDIENTE");
    }

    @Test
    void givenValidExcel_whenUploadMultipleTimes_thenConflictOnDuplicate() {
        String token = TestJwtHelper.tokenFor("admin@test.com", 1, List.of("ROLE_ADMIN"));
        Integer idPlano = crearPlano(token);

        ResponseEntity<List> first = subirExcel(
                idPlano, token,
                TestExcelHelper.xlsxWithOneTag("TAG-DUP", "EQUIPO"),
                "tags.xlsx");
        assertEquals(HttpStatus.CREATED, first.getStatusCode());

        ResponseEntity<Map> second = subirExcelExpectError(
                idPlano, token,
                TestExcelHelper.xlsxWithOneTag("TAG-DUP", "EQUIPO"),
                "tags2.xlsx");
        assertEquals(HttpStatus.CONFLICT, second.getStatusCode());
    }

    // PF-012: Excel con formato incorrecto → 400 con mensaje descriptivo

    @Test
    void givenExcelWithWrongHeaders_whenUploadTags_thenReturn400WithDescriptiveMessage() {
        String token = TestJwtHelper.tokenFor("admin@test.com", 1, List.of("ROLE_ADMIN"));
        Integer idPlano = crearPlano(token);

        ResponseEntity<Map> resp = subirExcelExpectError(
                idPlano, token,
                TestExcelHelper.xlsxWithWrongHeaders(),
                "tags_mal.xlsx");

        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        String msg = (String) resp.getBody().get("message");
        assertNotNull(msg, "El error debe incluir un mensaje descriptivo");
        assertTrue(
                msg.toLowerCase().contains("elemento") || msg.toLowerCase().contains("tipo")
                        || msg.toLowerCase().contains("columna"),
                "El mensaje debe indicar la columna faltante, pero fue: " + msg);
    }

    @Test
    void givenExcelMissingTipoColumn_whenUploadTags_thenReturn400() {
        String token = TestJwtHelper.tokenFor("admin@test.com", 1, List.of("ROLE_ADMIN"));
        Integer idPlano = crearPlano(token);

        ResponseEntity<Map> resp = subirExcelExpectError(
                idPlano, token,
                TestExcelHelper.xlsxWithMissingTipoColumn(),
                "sin_tipo.xlsx");

        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        String msg = (String) resp.getBody().get("message");
        assertNotNull(msg);
        assertTrue(msg.toLowerCase().contains("tipo") || msg.toLowerCase().contains("columna"),
                "El mensaje debe mencionar la columna TIPO faltante, pero fue: " + msg);
    }

    @Test
    void givenExcelWithHeaderOnly_whenUploadTags_thenReturn400() {
        String token = TestJwtHelper.tokenFor("admin@test.com", 1, List.of("ROLE_ADMIN"));
        Integer idPlano = crearPlano(token);

        ResponseEntity<Map> resp = subirExcelExpectError(
                idPlano, token,
                TestExcelHelper.xlsxWithHeaderOnly(),
                "vacio.xlsx");

        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        String msg = (String) resp.getBody().get("message");
        assertNotNull(msg);
        assertTrue(msg.toLowerCase().contains("tag") || msg.toLowerCase().contains("vacío")
                        || msg.toLowerCase().contains("filas") || msg.toLowerCase().contains("contiene"),
                "El mensaje debe indicar que no hay TAGs, pero fue: " + msg);
    }

    @Test
    void givenNonExcelFile_whenUploadTags_thenReturn400() {
        String token = TestJwtHelper.tokenFor("admin@test.com", 1, List.of("ROLE_ADMIN"));
        Integer idPlano = crearPlano(token);

        byte[] fakePdf = "%PDF-1.4 fake content".getBytes();
        ResponseEntity<Map> resp = subirExcelExpectError(
                idPlano, token, fakePdf, "no_es_excel.xlsx");

        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    @Test
    void givenUserRole_whenUploadExcel_thenReturn403() {
        String adminToken = TestJwtHelper.tokenFor("admin@test.com", 1, List.of("ROLE_ADMIN"));
        String userToken  = TestJwtHelper.tokenFor("op@test.com",    2, List.of("ROLE_USER"));
        Integer idPlano   = crearPlano(adminToken);

        ResponseEntity<Map> resp = subirExcelExpectError(
                idPlano, userToken,
                TestExcelHelper.xlsxWithOneTag("TAG-X", "EQUIPO"),
                "tags.xlsx");

        assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
    }
}
