package com.myplans.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.LinkedMultiValueMap;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("h2test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@TestPropertySource(properties = {
                "audit.service.uri=http://localhost:65000",
                "audit.service.timeout-ms=500",
                "audit.enforce-strict=false"
})
class AuditDownNotStrictTest {

        @Autowired
        private TestRestTemplate restTemplate;

        @BeforeEach
        void setUpHttpClient() {
                restTemplate.getRestTemplate()
                                .setRequestFactory(new HttpComponentsClientHttpRequestFactory());
        }

        @Test
        void changeTagState_persistsAndReturns200_evenIfAuditIsDown() {
                String token = TestJwtHelper.tokenFor("admin@test.com", 1, List.of("ROLE_ADMIN"));

                HttpHeaders authHeaders = new HttpHeaders();
                authHeaders.setBearerAuth(token);
                authHeaders.setContentType(MediaType.APPLICATION_JSON);

                ResponseEntity<Map> planoResp = restTemplate.postForEntity(
                                "/api/v1/planos",
                                new HttpEntity<>("{\"nombre\":\"P\",\"formulario\":\"F\"}", authHeaders),
                                Map.class);
                assertEquals(HttpStatus.CREATED, planoResp.getStatusCode());
                Integer idPlano = (Integer) planoResp.getBody().get("idPlano");

                HttpHeaders mpHeaders = new HttpHeaders();
                mpHeaders.setBearerAuth(token);
                mpHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);

                LinkedMultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
                form.add("file", new ByteArrayResource(TestExcelHelper.xlsxWithOneTag("TAG-001", "EQUIPO")) {
                        @Override
                        public String getFilename() {
                                return "tags.xlsx";
                        }
                });

                ResponseEntity<List> tagsResp = restTemplate.postForEntity(
                                "/api/v1/planos/" + idPlano + "/tags/excel",
                                new HttpEntity<>(form, mpHeaders),
                                List.class);
                assertEquals(HttpStatus.CREATED, tagsResp.getStatusCode());
                Integer idTag = (Integer) ((Map) tagsResp.getBody().get(0)).get("idTag");

                ResponseEntity<Map> updateResp = restTemplate.exchange(
                                "/api/v1/tags/" + idTag + "/estado",
                                HttpMethod.PATCH,
                                new HttpEntity<>("{\"estadoNuevo\":\"APROBADO\"}", authHeaders),
                                Map.class);

                assertEquals(HttpStatus.OK, updateResp.getStatusCode());
                assertEquals("APROBADO", updateResp.getBody().get("estadoActual"));
        }
}
