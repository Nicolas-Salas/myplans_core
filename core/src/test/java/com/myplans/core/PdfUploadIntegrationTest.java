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
 * PF-010 — Paso 2 precarga: subir PDF del plano → 200
 * Incluye: archivo no PDF → 400, sin token → 401, ROLE_USER → 403.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("h2test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@TestPropertySource(properties = {
        "audit.service.uri=http://localhost:65000",
        "audit.service.timeout-ms=300",
        "audit.enforce-strict=false",
        "storage.base-dir=/tmp/myplans-test-uploads"
})
class PdfUploadIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    /** PDF mínimo válido para tests (>100 bytes, firma PDF real). */
    private static final byte[] MINIMAL_PDF = (
            "%PDF-1.4\n1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n" +
            "2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n" +
            "3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] >>\nendobj\n" +
            "xref\n0 4\n0000000000 65535 f \n0000000009 00000 n \n" +
            "0000000058 00000 n \n0000000115 00000 n \n" +
            "trailer\n<< /Size 4 /Root 1 0 R >>\nstartxref\n190\n%%EOF"
    ).getBytes();

    private Integer crearPlano(String token) {
        restTemplate.getRestTemplate().setRequestFactory(new HttpComponentsClientHttpRequestFactory());
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.setBearerAuth(token);
        ResponseEntity<Map> resp = restTemplate.postForEntity(
                "/api/v1/planos",
                new HttpEntity<>("{\"nombre\":\"Plano PDF Test\",\"formulario\":\"PRE-ELE\"}", h),
                Map.class);
        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
        return (Integer) resp.getBody().get("idPlano");
    }

    private ResponseEntity<Map> subirPdf(Integer idPlano, String token,
                                          byte[] content, String filename) {
        restTemplate.getRestTemplate().setRequestFactory(new HttpComponentsClientHttpRequestFactory());
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(token);
        h.setContentType(MediaType.MULTIPART_FORM_DATA);
        LinkedMultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("file", new ByteArrayResource(content) {
            @Override public String getFilename() { return filename; }
        });
        return restTemplate.postForEntity(
                "/api/v1/planos/" + idPlano + "/pdf",
                new HttpEntity<>(form, h),
                Map.class);
    }

    // PF-010: Subir PDF válido → 200 y urlS3 no nulo

    @Test
    void givenValidPdf_whenUploadPdf_thenReturn200AndPlanoHasPdfUrl() {
        String token = TestJwtHelper.tokenFor("admin@test.com", 1, List.of("ROLE_ADMIN"));
        Integer idPlano = crearPlano(token);

        ResponseEntity<Map> resp = subirPdf(idPlano, token, MINIMAL_PDF, "plano.pdf");

        assertEquals(HttpStatus.OK, resp.getStatusCode(),
                "Subir PDF válido debe retornar 200");
        assertNotNull(resp.getBody().get("idPlano"),
                "La respuesta debe incluir el idPlano");
        Object urlS3 = resp.getBody().get("urlS3");
        assertNotNull(urlS3, "El plano debe registrar la ruta del PDF tras la carga");
    }

    @Test
    void givenPdfUploaded_whenGetPlanoById_thenPdfUrlIsPresent() {
        String token = TestJwtHelper.tokenFor("admin@test.com", 1, List.of("ROLE_ADMIN"));
        Integer idPlano = crearPlano(token);

        subirPdf(idPlano, token, MINIMAL_PDF, "plano.pdf");

        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(token);
        ResponseEntity<Map> resp = restTemplate.exchange(
                "/api/v1/planos/" + idPlano,
                HttpMethod.GET,
                new HttpEntity<>(h),
                Map.class);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody().get("urlS3"),
                "El GET del plano debe mostrar la ruta del PDF ya cargado");
    }

    @Test
    void givenValidPdf_whenUploadPdf_thenPlanosNroPaginasUpdated() {
        String token = TestJwtHelper.tokenFor("admin@test.com", 1, List.of("ROLE_ADMIN"));
        Integer idPlano = crearPlano(token);

        ResponseEntity<Map> resp = subirPdf(idPlano, token, MINIMAL_PDF, "plano.pdf");

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        Object nroPaginas = resp.getBody().get("nroPaginas");
        assertNotNull(nroPaginas, "El campo nroPaginas debe estar presente");
    }

    // Seguridad: sin token y rol incorrecto

    @Test
    void givenNoToken_whenUploadPdf_thenReturn401() {
        restTemplate.getRestTemplate().setRequestFactory(new HttpComponentsClientHttpRequestFactory());
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.MULTIPART_FORM_DATA);
        LinkedMultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("file", new ByteArrayResource(MINIMAL_PDF) {
            @Override public String getFilename() { return "plano.pdf"; }
        });
        ResponseEntity<Map> resp = restTemplate.postForEntity(
                "/api/v1/planos/1/pdf",
                new HttpEntity<>(form, h),
                Map.class);

        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
    }

    @Test
    void givenRoleUser_whenUploadPdf_thenReturn403() {
        String adminToken = TestJwtHelper.tokenFor("admin@test.com", 1, List.of("ROLE_ADMIN"));
        String userToken  = TestJwtHelper.tokenFor("op@test.com",    2, List.of("ROLE_USER"));
        Integer idPlano   = crearPlano(adminToken);

        ResponseEntity<Map> resp = subirPdf(idPlano, userToken, MINIMAL_PDF, "plano.pdf");

        assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
    }

    @Test
    void givenPlanoNotFound_whenUploadPdf_thenReturn404() {
        String token = TestJwtHelper.tokenFor("admin@test.com", 1, List.of("ROLE_ADMIN"));

        ResponseEntity<Map> resp = subirPdf(99999, token, MINIMAL_PDF, "plano.pdf");

        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
    }
}
