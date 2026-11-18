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
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthFlowType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthenticationResultType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.InitiateAuthRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.InitiateAuthResponse;

class CognitoAuthTokenGeneratorTest {

    /**
     * Test successful authentication with valid username and password
     */
    @Test
    void testAuthenticateWithValidCredentials() {
        // Arrange
        String clientId = "testClientId";
        String username = "testUser";
        String password = "testPassword";

        CognitoIdentityProviderClient mockCognitoClient = Mockito.mock(CognitoIdentityProviderClient.class);
        CognitoAuthTokenGenerator authenticator =
                new CognitoAuthTokenGenerator(clientId, username, password, mockCognitoClient);

        Map<String, String> authParameters = new HashMap<>();
        authParameters.put("USERNAME", username);
        authParameters.put("PASSWORD", password);

        InitiateAuthRequest expectedAuthRequest = InitiateAuthRequest.builder()
                .clientId(clientId)
                .authFlow(AuthFlowType.USER_PASSWORD_AUTH)
                .authParameters(authParameters)
                .build();

        AuthenticationResultType result =
                AuthenticationResultType.builder().idToken("id token").build();
        InitiateAuthResponse mockResponse =
                InitiateAuthResponse.builder().authenticationResult(result).build();

        when(mockCognitoClient.initiateAuth(expectedAuthRequest)).thenReturn(mockResponse);

        // Act
        String actual = authenticator.generate();

        // Assert
        assertEquals("id token", actual);
        Mockito.verify(mockCognitoClient).initiateAuth(expectedAuthRequest);
    }
}
