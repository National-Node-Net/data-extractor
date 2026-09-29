# Uninstall

**Repository:** `data-extractor`  
**Description:** `This file provides detailed steps to remove this repository, including any dependencies and configurations for Data Extractor.`

<!-- SPDX-License-Identifier: OGL-UK-3.0 -->

---

Uninstalling and removing the repository involves 3 parts:
- Delete installed JAR file
- Docker configuration and containers
- Delete repository clone

## Deleting installed JAR file

```sh
./mvnw dependency:purge-local-repository -DmanualInclude="uk.gov.dbt.ndtp.extractor"
```

## Deleting Docker containers

1. List all containers to find the container ID or name:

   ```sh
   docker ps -a
   ```
2. Stop the Docker container by ID or name if it is running:

   ```sh
   docker stop <container_id_or_name>
   ```
3. Delete the Docker container by ID or name:

   ```sh
   docker rm <container_id_or_name>
   ```

   You can also use the -f flag to forcefully remove a running Docker container:

   ```sh
   docker rm -f <container_id_or_name>
   ```
4. Confirm Docker has been deleted by listing all Docker containers:

   ```sh
   docker ps -a
   ```

## Deleting repository clone

Simply delete the cloned repository files from working location.

© Crown Copyright 2026. This work has been developed by the National Digital Twin Programme and is legally attributed to the UK's Department for Business, Innovation, Science and Trade (BIST) as the governing entity.
