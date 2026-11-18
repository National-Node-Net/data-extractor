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

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class LoggingDataDumperTest {

    private final LoggingDataDumper underTest = new LoggingDataDumper();
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void setUp() {
        Logger logger = (Logger) LoggerFactory.getLogger(LoggingDataDumper.class);

        appender = new ListAppender<>();
        appender.start();

        logger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        Logger logger = (Logger) LoggerFactory.getLogger(LoggingDataDumper.class);
        logger.detachAppender(appender);
        appender.stop();
    }

    @Test
    void upload() throws IOException, DataDumperException {
        String data =
                """
                <?xml version="1.0"?>
                <sparql xmlns="http://www.w3.org/2005/sparql-results#">
                  <head>
                    <variable name="s"/>
                    <variable name="p"/>
                    <variable name="o"/>
                  </head>
                  <results>
                  </results>
                </sparql>
                """;

        try (InputStream in = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8))) {
            underTest.upload(in);
        }

        List<String> expected = data.lines().toList();

        Assertions.assertEquals(expected.size(), appender.list.size());
        for (int i = 0; i < expected.size(); i++) {
            Assertions.assertEquals(
                    "Response line: " + expected.get(i), appender.list.get(i).getFormattedMessage());
        }
    }
}
