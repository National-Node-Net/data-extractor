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

import io.avaje.config.Config;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthFlowType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthenticationResultType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.InitiateAuthRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.InitiateAuthResponse;

class CognitoAuthTokenGenerator implements AuthTokenGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(CognitoAuthTokenGenerator.class);
    private static final String COGNITO_CLIENT_URL = "cognito.client.url";
    private static final String COGNITO_CLIENT_ID = "cognito.client.id";
    private static final String COGNITO_USERNAME = "cognito.username";
    private static final String COGNITO_PASSWORD = "cognito.password";
    private static final String AWS_REGION = "aws.region";

    private final CognitoIdentityProviderClient cognitoClient;
    private final String clientId;
    private final String username;
    private final String password;

    CognitoAuthTokenGenerator() {
        this(
                Config.get(COGNITO_CLIENT_URL, "http://0.0.0.0:9229"),
                Config.get(COGNITO_CLIENT_ID),
                Config.get(AWS_REGION),
                Config.get(COGNITO_USERNAME),
                Config.get(COGNITO_PASSWORD));
    }

    public CognitoAuthTokenGenerator(
            String endpoint, String clientId, String region, String username, String password) {
        this(
                clientId,
                username,
                password,
                CognitoIdentityProviderClient.builder()
                        .endpointOverride(URI.create(endpoint))
                        .region(Region.of(region))
                        .httpClient(UrlConnectionHttpClient.builder().build())
                        .build());
    }

    CognitoAuthTokenGenerator(
            String clientId, String username, String password, CognitoIdentityProviderClient cognitoClient) {
        this.clientId = clientId;
        this.username = username;
        this.password = password;
        this.cognitoClient = cognitoClient;
    }

    private AuthenticationResultType generateAuthenticationToken() {
        try {
            InitiateAuthResponse initiateAuthResponse = authenticate();
            LOGGER.debug("Authentication Result:");
            LOGGER.debug("Challenge Name: {}", initiateAuthResponse.challengeName());
            LOGGER.debug("Session: {}", initiateAuthResponse.session());

            AuthenticationResultType result = initiateAuthResponse.authenticationResult();
            if (result == null) {
                LOGGER.error("Authentication response failed for user: {}", username);
                throw new AuthenticationException("Authenticated response error. Invalid Username or Password");
            }
            LOGGER.info("Authentication successful for user {}", username);
            LOGGER.debug("Access Token: {}", result.accessToken());
            LOGGER.debug("ID Token: {}", result.idToken());
            LOGGER.debug("Refresh Token: {}", result.refreshToken());
            LOGGER.debug("Token Type: {}", result.tokenType());
            LOGGER.debug("Expires In: {}", result.expiresIn());
            return result;
        } catch (Exception e) {
            throw new AuthenticationException("Authentication failed for user: " + username, e);
        }
    }

    private InitiateAuthResponse authenticate() {
        Map<String, String> authParameters = new HashMap<>();
        authParameters.put("USERNAME", username);
        authParameters.put("PASSWORD", password);

        InitiateAuthRequest authRequest = InitiateAuthRequest.builder()
                .clientId(clientId)
                .authFlow(AuthFlowType.USER_PASSWORD_AUTH)
                .authParameters(authParameters)
                .build();

        return cognitoClient.initiateAuth(authRequest);
    }

    @Override
    public String generate() throws AuthenticationException {
        return generateAuthenticationToken().idToken();
    }

    @Override
    public void close() {
        cognitoClient.close();
    }
}
