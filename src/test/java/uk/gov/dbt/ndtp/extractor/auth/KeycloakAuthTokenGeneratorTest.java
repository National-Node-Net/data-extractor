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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class KeycloakAuthTokenGeneratorTest {

    private static final String CLIENT_ID = "test-client";
    private static final String GRANT_TYPE = "grant-type";
    private static final String USERNAME = "test-user";
    private static final String PASSWORD = "test-pass";

    private MockWebServer server;
    private KeycloakAuthTokenGenerator underTest;
    private String endPoint;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();

        endPoint = server.url("").toString();
        underTest = new KeycloakAuthTokenGenerator(endPoint, CLIENT_ID, GRANT_TYPE, USERNAME, PASSWORD);
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
        underTest.close();
    }

    /**
     * Test successful authentication with valid username and password
     */
    @Test
    void testAuthenticateWithValidCredentials() throws InterruptedException {
        MockResponse response = new MockResponse()
                .setResponseCode(200)
                .setBody(
                        """
                            {
                                "access_token":"sample access token",
                                "expires_in":60,
                                "refresh_expires_in":1800,
                                "refresh_token":"sample refresh token",
                                "token_type":"Bearer",
                                "id_token":"sample id token",
                                "scope":"openid email profile"
                            }
                            """);
        server.enqueue(response);

        String token = underTest.generate();

        assertEquals("sample id token", token);

        RecordedRequest request = server.takeRequest();

        assertEquals("POST", request.getMethod());
        assertEquals(endPoint, request.getRequestUrl().toString());
        assertEquals(
                "password=%s&grant_type=%s&scope=openid&client_id=%s&username=%s"
                        .formatted(PASSWORD, GRANT_TYPE, CLIENT_ID, USERNAME),
                request.getBody().readUtf8());
    }

    /**
     * Test authentication failure when the server response does not contain an id_token
     */
    @Test
    void testAuthenticateWithMissingIdToken() {
        MockResponse response = new MockResponse()
                .setResponseCode(200)
                .setBody(
                        """
                            {
                                "access_token":"sample access token",
                                "expires_in":60,
                                "refresh_expires_in":1800,
                                "refresh_token":"sample refresh token",
                                "token_type":"Bearer",
                                "scope":"openid email profile"
                            }
                            """);
        server.enqueue(response);

        assertThrows(AuthenticationException.class, underTest::generate);
    }

    /**
     * Test authentication failure when the server returns a non-200 status code
     */
    @Test
    void testAuthenticateWithNon200StatusCode() {
        server.enqueue(new MockResponse().setResponseCode(401).setBody("Unauthorized"));

        assertThrows(AuthenticationException.class, underTest::generate);
    }
}
