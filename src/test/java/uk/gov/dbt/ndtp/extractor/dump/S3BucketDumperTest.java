// SPDX-License-Identifier: Apache-2.0
// © Crown Copyright 2025. This work has been developed by the National Digital Twin Programme
// and is legally attributed to the Department for Business and Trade (UK) as the governing entity.

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
package uk.gov.dbt.ndtp.extractor.dump;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.adobe.testing.s3mock.testcontainers.S3MockContainer;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Object;

class S3BucketDumperTest {

    private static final String BUCKET = "test-bucket";

    private static S3MockContainer s3Mock;

    private S3BucketDumper underTest;
    private S3AsyncClient client;

    @BeforeAll
    static void beforeAll() {
        s3Mock = new S3MockContainer("latest").withInitialBuckets(BUCKET);
        s3Mock.start();
    }

    @AfterAll
    static void afterAll() {
        s3Mock.close();
    }

    @BeforeEach
    void setUp() {
        client = S3AsyncClient.crtBuilder()
                .endpointOverride(URI.create(s3Mock.getHttpsEndpoint()))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("123", "123")))
                .region(Region.EU_WEST_1)
                .httpConfiguration(builder -> builder.trustAllCertificatesEnabled(true))
                .forcePathStyle(true)
                .futureCompletionExecutor(Executors.newVirtualThreadPerTaskExecutor())
                .retryConfiguration(builder -> builder.numRetries(1))
                .build();

        underTest = new S3BucketDumper(BUCKET, client);
    }

    @AfterEach
    void tearDown() {
        underTest.close();
    }

    @Test
    void upload_failure_with_client() {
        ByteArrayInputStream input = new ByteArrayInputStream("test-data".getBytes(StandardCharsets.UTF_8));

        CompletableFuture<Void> task = CompletableFuture.runAsync(
                () -> {
                    DataDumperException e = assertThrows(DataDumperException.class, () -> underTest.upload(input));
                    assertTrue(e.getMessage().startsWith("Failed to upload file to S3 bucket test-bucket with key "));
                },
                CompletableFuture.delayedExecutor(10, TimeUnit.MILLISECONDS));

        client.close();

        assertDoesNotThrow(task::join);
    }

    @Test
    void upload_null_data() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> underTest.upload(null));

        assertEquals("Data stream cannot be null", e.getMessage());
    }
}
