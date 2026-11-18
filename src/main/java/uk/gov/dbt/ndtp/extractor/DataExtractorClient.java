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
package uk.gov.dbt.ndtp.extractor;

import io.avaje.config.Config;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.dbt.ndtp.extractor.auth.AuthTokenGenerator;
import uk.gov.dbt.ndtp.extractor.auth.AuthenticationException;

public class DataExtractorClient implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataExtractorClient.class);
    private static final String GRAPH_SERVICE_URL = "graph.service.url";

    private final HttpClient client;
    private final AuthTokenGenerator tokenGenerator;
    private final URI clientUrl;

    DataExtractorClient(AuthTokenGenerator tokenGenerator) throws DataExtractionException {
        this(tokenGenerator, Config.get(GRAPH_SERVICE_URL, "http://localhost:3030/ds"));
    }

    DataExtractorClient(AuthTokenGenerator tokenGenerator, URI clientUrl) {
        this.client = HttpClient.newHttpClient();
        this.clientUrl = clientUrl;
        this.tokenGenerator = tokenGenerator;
    }

    DataExtractorClient(AuthTokenGenerator tokenGenerator, String clientUrl) throws DataExtractionException {
        this(tokenGenerator, mapUrl(clientUrl));
    }

    private static URI mapUrl(String clientUrl) throws DataExtractionException {
        try {
            return new URI(clientUrl);
        } catch (URISyntaxException e) {
            throw new DataExtractionException("Could not parse client URL: " + e.getMessage(), e);
        }
    }

    public InputStream extractData(String query) {
        HttpRequest request = generateRequest(query);

        try {
            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            int statusCode = response.statusCode();
            if (statusCode >= 200 && statusCode < 300) {
                LOGGER.info("Secure agent query successful");
                return response.body();
            }
            LOGGER.warn("Secure agent query unsuccessful, received status {}", statusCode);
            throw handleUnsuccessfulRequest(statusCode, response);
        } catch (IOException e) {
            throw new DataExtractionException("Could not extract data from secure agent: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DataExtractionException("Could not extract data from secure agent: " + e.getMessage(), e);
        }
    }

    private static DataExtractionException handleUnsuccessfulRequest(int statusCode, HttpResponse<InputStream> response)
            throws IOException {
        String message = "Could not extract data from secure agent. Received status code '" + statusCode + "'.";
        try (InputStreamReader reader = new InputStreamReader(response.body())) {
            StringWriter buffer = new StringWriter();
            reader.transferTo(buffer);
            String content = buffer.toString();
            if (!content.isBlank()) {
                message += "\nResponse body:\n" + content;
            }
        }
        return new DataExtractionException(message);
    }

    private HttpRequest generateRequest(String query) {
        try {
            return HttpRequest.newBuilder(clientUrl)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Authorization", "bearer " + tokenGenerator.generate())
                    .POST(HttpRequest.BodyPublishers.ofString("query=" + query))
                    .build();
        } catch (AuthenticationException e) {
            throw new DataExtractionException("Could not generate token: " + e.getMessage(), e);
        }
    }

    @Override
    public void close() {
        try {
            tokenGenerator.close();
        } catch (Exception e) {
            LOGGER.warn("Failed to close token generator", e);
        }
        client.close();
    }

    static class DataExtractionException extends RuntimeException {
        DataExtractionException(String message, Throwable cause) {
            super(message, cause);
        }

        DataExtractionException(String message) {
            super(message);
        }
    }
}
