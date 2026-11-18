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

import static uk.gov.dbt.ndtp.extractor.auth.AuthTokenGenerator.cognito;
import static uk.gov.dbt.ndtp.extractor.auth.AuthTokenGenerator.keycloak;

import io.avaje.config.Config;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.dbt.ndtp.extractor.auth.AuthTokenGenerator;
import uk.gov.dbt.ndtp.extractor.dump.DataDumper;
import uk.gov.dbt.ndtp.extractor.dump.DataDumperException;

public class DataExtractor implements Runnable, AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataExtractor.class);
    private static final String DATA_EXTRACTOR_PROPERTIES = "DATA_EXTRACTOR_PROPERTIES";
    private static final String AUTH_PROVIDER = "auth.provider";
    private static final String DATA_DUMPER = "data.dumper";

    private final String query;
    private final DataExtractorClient client;
    private final DataDumper dataDumper;

    DataExtractor(String query, DataExtractorClient client, DataDumper dataDumper) {
        this.query = query;
        this.client = client;
        this.dataDumper = dataDumper;
    }

    public static void main(String[] args) {
        validateEnvironment();

        AuthTokenGenerator tokenGenerator = tokenGenerator();
        DataDumper dataDumper = dataDumper();
        String query = query();

        try (DataExtractor extractor = new DataExtractor(query, new DataExtractorClient(tokenGenerator), dataDumper)) {
            extractor.run();
        } catch (Exception e) {
            LOGGER.atDebug().setCause(e).setMessage("Error running extractor").log();
            LOGGER.error("Error running extractor: {}", e.getMessage());
        }
    }

    private static void validateEnvironment() {
        String propertyLocation = System.getenv(DATA_EXTRACTOR_PROPERTIES);
        if (propertyLocation == null) {
            LOGGER.error("{} environment variable not set", DATA_EXTRACTOR_PROPERTIES);
            System.exit(1);
        }
        if (!Files.exists(Path.of(propertyLocation))) {
            LOGGER.error("{} does not exist", propertyLocation);
            System.exit(1);
        }
    }

    @SuppressWarnings("resource")
    private static AuthTokenGenerator tokenGenerator() {
        String authProvider = Config.get(AUTH_PROVIDER, "keycloak");
        AuthTokenGenerator tokenGenerator =
                switch (authProvider) {
                    case "keycloak" -> keycloak();
                    case "cognito" -> cognito();
                    default -> throw new IllegalStateException(
                            "Unexpected value for auth provider(" + AUTH_PROVIDER + "): " + authProvider);
                };
        LOGGER.info("Authenticator configured: {}", authProvider);
        return tokenGenerator;
    }

    @SuppressWarnings("resource")
    private static DataDumper dataDumper() {
        String dataDumperProvider = Config.get(DATA_DUMPER, "log");
        DataDumper dataDumper =
                switch (dataDumperProvider) {
                    case "log" -> DataDumper.log();
                    case "s3" -> DataDumper.s3();
                    default -> throw new IllegalStateException(
                            "Unexpected value for data dumper(" + DATA_DUMPER + "): " + dataDumperProvider);
                };
        LOGGER.info("Data dumper configured: {}", dataDumperProvider);
        return dataDumper;
    }

    private static String query() throws DataExtractorException {
        try {
            return Files.readString(Config.getAs("query.location", Paths::get), StandardCharsets.UTF_8);
        } catch (InvalidPathException | IOException e) {
            throw new DataExtractorException("Could not read query file", e);
        }
    }

    @Override
    public void run() {
        try (InputStream result = client.extractData(query)) {
            dataDumper.upload(result);
        } catch (DataExtractorClient.DataExtractionException | DataDumperException e) {
            throw new DataExtractorException(e.getMessage(), e);
        } catch (Exception e) {
            throw new DataExtractorException("Failed to extract data", e);
        }
    }

    @Override
    public void close() {
        try {
            client.close();
        } catch (Exception e) {
            LOGGER.warn("Failed to close data extractor client", e);
        }
        try {
            dataDumper.close();
        } catch (Exception e) {
            LOGGER.warn("Failed to close data dumper", e);
        }
    }

    private static class DataExtractorException extends RuntimeException {
        public DataExtractorException(String message, Exception e) {
            super(message, e);
        }
    }
}
