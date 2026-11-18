// SPDX-License-Identifier: Apache-2.0
// © Crown Copyright 2026. This work has been developed by the National Digital Twin Programme
// and is legally attributed to the UK's Department for Business, Innovation, Science and Trade (BIST) as the governing entity.

/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package uk.gov.dbt.ndtp.extractor.auth;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.avaje.config.Config;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class KeycloakAuthTokenGenerator implements AuthTokenGenerator {
    public static final String APPLICATION_X_WWW_FORM_URLENCODED = "application/x-www-form-urlencoded";
    public static final String ID_TOKEN = "id_token";
    public static final int TOKEN_EXPIRES_VALUE = 3600;
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    private static final String OPEN_ID_SCOPE = "openid";
    private static final String KEYCLOAK_CLIENT_URL = "keycloak.client.url";
    private static final String KEYCLOAK_CLIENT_ID = "keycloak.client.id";
    private static final String KEYCLOAK_GRANT_TYPE = "keycloak.grant.type";
    private static final String KEYCLOAK_USERNAME = "keycloak.username";
    private static final String KEYCLOAK_PASSWORD = "keycloak.password";

    private final HttpClient httpClient;
    private final String keycloakEndpoint;
    private final String clientId;
    private final String grantType;
    private final String username;
    private final String password;

    private String cachedToken;
    private long tokenExpiration;

    public KeycloakAuthTokenGenerator() {
        this(
                Config.get(KEYCLOAK_CLIENT_URL, "http://0.0.0.0:9229"),
                Config.get(KEYCLOAK_CLIENT_ID),
                Config.get(KEYCLOAK_GRANT_TYPE),
                Config.get(KEYCLOAK_USERNAME),
                Config.get(KEYCLOAK_PASSWORD));
    }

    public KeycloakAuthTokenGenerator(
            String endpoint, String clientId, String grantType, String username, String password) {
        this.keycloakEndpoint = endpoint;
        this.clientId = clientId;
        this.grantType = grantType;
        this.username = username;
        this.password = password;
        this.httpClient = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
    }

    @Override
    public String generate() throws AuthenticationException {
        if (isTokenValid()) {
            return cachedToken;
        }

        try {
            Map<String, String> formData = new HashMap<>();
            formData.put("grant_type", grantType);
            formData.put("client_id", clientId);
            formData.put("username", username);
            formData.put("password", password);
            formData.put("scope", OPEN_ID_SCOPE);

            String formBody = formData.entrySet().stream()
                    .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                    .reduce((a, b) -> a + "&" + b)
                    .orElse("");

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(keycloakEndpoint))
                    .timeout(TIMEOUT)
                    .header("Content-Type", APPLICATION_X_WWW_FORM_URLENCODED)
                    .POST(HttpRequest.BodyPublishers.ofString(formBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new AuthenticationException("Failed to authenticate. Status: " + response.statusCode());
            }

            Map<String, Object> tokenResponse = MAPPER.readValue(response.body(), new TypeReference<>() {});

            if (!tokenResponse.containsKey(ID_TOKEN)) {
                throw new AuthenticationException("No id token in response");
            }

            cachedToken = (String) tokenResponse.get(ID_TOKEN);
            int expiresIn = (Integer) tokenResponse.getOrDefault("expires_in", TOKEN_EXPIRES_VALUE);
            tokenExpiration = System.currentTimeMillis() + (expiresIn * 1000L);

            return cachedToken;

        } catch (IOException e) {
            throw new AuthenticationException("Authentication failed due to I/O error", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AuthenticationException("Authentication was interrupted", e);
        } catch (Exception e) {
            throw new AuthenticationException("Unexpected error during authentication", e);
        }
    }

    private boolean isTokenValid() {
        return cachedToken != null && System.currentTimeMillis() < tokenExpiration - 60000; // 1 minute buffer
    }

    @Override
    public void close() {
        this.httpClient.close();
    }
}
