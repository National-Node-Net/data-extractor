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
package uk.gov.dbt.ndtp.extractor.dump;

import io.avaje.config.Config;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.BlockingInputStreamAsyncRequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

class S3BucketDumper implements DataDumper {

    private static final Logger LOGGER = LoggerFactory.getLogger(S3BucketDumper.class);
    private static final String FILE_NAME_PREFIX_FORMAT = "yyyy-MM-dd-HH:mm:ss.SSSS";
    private static final Random RANDOM_ID_GENERATOR = new Random();

    private static final String AWS_S3_BUCKET_NAME_KEY = "aws.s3.bucket.name";
    private static final String AWS_REGION_KEY = "aws.region";
    private static final String AWS_ACCESS_KEY = "aws.access.key.id";
    private static final String AWS_SECRET_KEY = "aws.secret.access.key";

    private final String bucketName;
    private final S3AsyncClient s3AsyncClient;

    public S3BucketDumper() {
        this(Config.get(AWS_S3_BUCKET_NAME_KEY), buildClientFromConfig());
    }

    S3BucketDumper(String bucketName, S3AsyncClient s3AsyncClient) {
        this.bucketName = bucketName;
        this.s3AsyncClient = s3AsyncClient;
    }

    private static S3AsyncClient buildClientFromConfig() {
        AwsBasicCredentials awsCredentials =
                AwsBasicCredentials.create(Config.get(AWS_ACCESS_KEY), Config.get(AWS_SECRET_KEY));
        StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(awsCredentials);

        return S3AsyncClient.crtBuilder()
                .credentialsProvider(credentialsProvider)
                .region(Region.of(Config.get(AWS_REGION_KEY)))
                .targetThroughputInGbps(20.0)
                .minimumPartSizeInBytes(8 * 1024 * 1024L)
                .build();
    }

    @Override
    public void upload(InputStream data) throws DataDumperException {
        if (data == null) {
            throw new IllegalArgumentException("Data stream cannot be null");
        }
        String objectKey = generateUniqueId() + ".rdf";

        BlockingInputStreamAsyncRequestBody body = AsyncRequestBody.forBlockingInputStream(null);

        CompletableFuture<PutObjectResponse> upload =
                s3AsyncClient.putObject(req -> req.key(objectKey).bucket(bucketName), body);

        try {
            body.writeInputStream(data);
            upload.join();
            LOGGER.info("Successfully uploaded file to S3 bucket {} with key {}", bucketName, objectKey);
        } catch (Exception e) {
            Throwable cause = e instanceof CompletionException comp ? comp.getCause() : e;
            throw new DataDumperException(
                    "Failed to upload file to S3 bucket " + bucketName + " with key " + objectKey, cause);
        }
    }

    private String generateUniqueId() {
        LocalDateTime now = LocalDateTime.now();
        String timestamp = now.format(DateTimeFormatter.ofPattern(FILE_NAME_PREFIX_FORMAT));
        int randomNum = RANDOM_ID_GENERATOR.nextInt(1000);
        return String.format("%s_%04d", timestamp, randomNum);
    }

    @Override
    public void close() {
        if (s3AsyncClient != null) {
            s3AsyncClient.close();
        }
    }
}
