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

import io.avaje.config.ConfigurationLog;
import java.text.MessageFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

public class AvajeLogbackBridge implements ConfigurationLog {
    private static final Logger log = LoggerFactory.getLogger(AvajeLogbackBridge.class);

    @Override
    public void log(System.Logger.Level level, String message, Throwable thrown) {
        Level slf4jLevel = map(level);
        if (slf4jLevel != null && log.isEnabledForLevel(slf4jLevel)) {
            log.atLevel(slf4jLevel).setMessage(message).setCause(thrown).log();
        }
    }

    @Override
    public void log(System.Logger.Level level, String message, Object... args) {
        Level slf4jLevel = map(level);
        if (slf4jLevel != null && log.isEnabledForLevel(slf4jLevel)) {
            log.atLevel(slf4jLevel)
                    .setMessage(() -> MessageFormat.format(message, args))
                    .log();
        }
    }

    private Level map(System.Logger.Level level) {
        return switch (level) {
            case ALL, TRACE -> Level.TRACE;
            case DEBUG -> Level.DEBUG;
            case INFO -> Level.INFO;
            case WARNING -> Level.WARN;
            case ERROR -> Level.ERROR;
            case OFF -> null;
        };
    }
}
