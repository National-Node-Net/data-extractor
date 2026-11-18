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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.dbt.ndtp.extractor.DataExtractorClient.DataExtractionException;
import uk.gov.dbt.ndtp.extractor.auth.AuthTokenGenerator;
import uk.gov.dbt.ndtp.extractor.auth.AuthenticationException;

class DataExtractorClientTest {
    public static final String QUERY = "some query";
    public static final String TOKEN = "generated token";
    private MockWebServer server;

    private DataExtractorClient underTest;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();

        server.start();
        underTest = new DataExtractorClient(
                new StaticTokenGenerator(), server.url("/ds").uri());
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
        underTest.close();
    }

    @Test
    void extractData() throws IOException, InterruptedException {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("<some successful response>"));

        InputStream data = underTest.extractData(QUERY);

        try (InputStreamReader reader = new InputStreamReader(data)) {
            StringWriter dataBuffer = new StringWriter();
            reader.transferTo(dataBuffer);
            assertEquals("<some successful response>", dataBuffer.toString());
        }

        RecordedRequest request = server.takeRequest();

        assertEquals("POST", request.getMethod());
        assertNotNull(request.getRequestUrl());
        assertEquals(server.url("/ds").toString(), request.getRequestUrl().toString());
        assertEquals("bearer " + TOKEN, request.getHeader("Authorization"));
        assertEquals("application/x-www-form-urlencoded", request.getHeader("Content-Type"));
        assertEquals("query=some query", request.getBody().readUtf8());
    }

    @Test
    void extractData_failure() {
        server.enqueue(new MockResponse().setResponseCode(401).setBody("no"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> underTest.extractData(QUERY));

        assertEquals(
                """
                Could not extract data from secure agent. Received status code '401'.
                Response body:
                no""",
                exception.getMessage());
    }

    @Test
    void extractData_failure_no_body() {
        server.enqueue(new MockResponse().setResponseCode(401));

        DataExtractionException exception =
                assertThrows(DataExtractionException.class, () -> underTest.extractData(QUERY));

        assertEquals("Could not extract data from secure agent. Received status code '401'.", exception.getMessage());
    }

    @Test
    void extractData_failure_token_generation() {
        AuthenticationException cause = new AuthenticationException("Oops");
        underTest = new DataExtractorClient(
                new FailingTokenGenerator(cause), server.url("/ds").uri());

        DataExtractionException exception =
                assertThrows(DataExtractionException.class, () -> underTest.extractData(QUERY));

        assertEquals("Could not generate token: Oops", exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    private static class StaticTokenGenerator implements AuthTokenGenerator {
        @Override
        public String generate() throws AuthenticationException {
            return TOKEN;
        }

        @Override
        public void close() {
            // no-op
        }
    }

    private static class FailingTokenGenerator implements AuthTokenGenerator {
        private final AuthenticationException cause;

        FailingTokenGenerator(AuthenticationException cause) {
            this.cause = cause;
        }

        @Override
        public String generate() throws AuthenticationException {
            throw cause;
        }

        @Override
        public void close() {
            // no-op
        }
    }
}
