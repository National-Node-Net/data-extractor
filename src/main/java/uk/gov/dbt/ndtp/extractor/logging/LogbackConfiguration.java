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
package uk.gov.dbt.ndtp.extractor.logging;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.Configurator;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.tyler.TylerConfiguratorBase;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ConsoleAppender;

public class LogbackConfiguration extends TylerConfiguratorBase implements Configurator {

    private final String level;

    public LogbackConfiguration() {
        level = "local".equalsIgnoreCase(System.getenv("ENV")) ? "DEBUG" : "INFO";
    }

    @Override
    public ExecutionStatus configure(LoggerContext context) {
        setContext(context);
        Logger root = setupLogger("ROOT", level, null);
        root.addAppender(console());
        return ExecutionStatus.DO_NOT_INVOKE_NEXT_IF_ANY;
    }

    Appender<ILoggingEvent> console() {
        ConsoleAppender<ILoggingEvent> appender = new ConsoleAppender<>();
        appender.setContext(context);
        appender.setName("CONSOLE");

        PatternLayoutEncoder patternLayoutEncoder = new PatternLayoutEncoder();
        patternLayoutEncoder.setContext(context);
        patternLayoutEncoder.setPattern("%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n");
        patternLayoutEncoder.setParent(appender);
        patternLayoutEncoder.start();
        appender.setEncoder(patternLayoutEncoder);

        appender.start();
        return appender;
    }
}
