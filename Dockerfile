# SPDX-License-Identifier: Apache-2.0
# © Crown Copyright 2025. This work has been developed by the National Digital Twin Programme and is legally attributed to the Department for Business and Trade (UK) as the governing entity.

# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.


FROM maven:3-eclipse-temurin-21-alpine AS builder

WORKDIR /app

ENV MAVEN_CLI_OPTS="--no-transfer-progress --batch-mode --errors --show-version"

COPY pom.xml /app

# resolve maven dependencies and cache them
RUN mvn $MAVEN_CLI_OPTS verify --fail-never && rm -r /app/target

COPY src /app/src

# build without the need for dependency download
RUN mvn $MAVEN_CLI_OPTS package -PdockerBuild

# build the final image
FROM eclipse-temurin:21-jre-alpine
LABEL org.opencontainers.image.source=https://github.com/National-Node-Net/data-extractor
LABEL org.opencontainers.image.url=https://ndtp.co.uk
LABEL org.opencontainers.image.authors="Department for Business and Trade (ndtp@businessandtrade.gov.uk)"
LABEL org.opencontainers.image.title="Data Extractor"
LABEL org.opencontainers.image.description="Data Extractor provides an automated approach to running queries against an IA Node for the purpose of data extraction."
LABEL org.opencontainers.image.licenses="Apache-2.0"

ENV EXTRACTOR_HOME=/app

USER root

RUN mkdir -p $EXTRACTOR_HOME; \
    adduser --system --no-create-home --uid 1000 --ingroup root extractor; \
    apk -U upgrade; \
    chown extractor /app/

COPY --from=builder --chown=root:root --chmod=555 /app/target/*.jar /app/app.jar
COPY --from=builder --chown=root:root --chmod=555 /app/target/lib /app/lib

VOLUME /app/config

WORKDIR $EXTRACTOR_HOME
USER extractor

CMD ["java", "-jar", "/app/app.jar"]
