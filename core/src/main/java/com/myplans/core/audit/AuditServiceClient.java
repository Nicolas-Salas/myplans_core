package com.myplans.core.audit;

import com.myplans.core.exception.AuditServiceUnavailableException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class AuditServiceClient {

    private static final Logger log = LoggerFactory.getLogger(AuditServiceClient.class);

    private static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";
    private static final String HISTORIAL_ENDPOINT = "/api/v1/historial";

    @Value("${audit.service.uri:http://localhost:8082}")
    private String auditServiceUri;

    @Value("${audit.service.internal-token:dev-internal-token-please-change-in-prod}")
    private String internalToken;

    @Value("${audit.enforce-strict:false}")
    private boolean enforceStrict;

    @Value("${audit.service.timeout-ms:2000}")
    private long timeoutMs;

    private RestClient restClient;

    private RestClient client() {
        if (restClient == null) {
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(Duration.ofMillis(timeoutMs));
            factory.setReadTimeout(Duration.ofMillis(timeoutMs));

            restClient = RestClient.builder()
                    .baseUrl(auditServiceUri)
                    .requestFactory(factory)
                    .defaultHeader(INTERNAL_TOKEN_HEADER, internalToken)
                    .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .build();
        }
        return restClient;
    }

    public void publish(AuditEvent event) {
        try {
            client()
                    .post()
                    .uri(HISTORIAL_ENDPOINT)
                    .body(event)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            handleFailure(event, ex);
        } catch (Exception ex) {
            handleFailure(event, ex);
        }
    }

    private void handleFailure(AuditEvent event, Exception ex) {
        if (enforceStrict) {
            throw new AuditServiceUnavailableException(
                    "No se pudo publicar el evento de auditoría (idTag=" + event.idTag()
                    + ", estadoNuevo=" + event.estadoNuevo() + ")", ex);
        }
        log.warn("Audit Service no disponible. Evento NO registrado: idTag={}, idUsuario={}, {} -> {}. Causa: {}",
                event.idTag(), event.idUsuario(),
                event.estadoAnterior(), event.estadoNuevo(),
                ex.getMessage());
    }
}