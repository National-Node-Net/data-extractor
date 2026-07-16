# README

**Repository:** `data-extractor`  
**Description:** `This repository provides an automated approach to running queries against an IA Node for the purpose of data extraction.`

<!-- SPDX-License-Identifier: Apache-2.0 AND OGL-UK-3.0 -->

---

## Overview

This repository contributes to the development of **secure, scalable, and interoperable data-sharing infrastructure**. It supports NDTP’s mission to enable **trusted, federated, and decentralised** data-sharing across organisations.

This repository is one of several open-source components that underpin NDTP’s **Integration Architecture (IA)**—a framework designed to allow organisations to manage and exchange data securely while maintaining control over their own information. The IA is actively deployed and tested across multiple sectors, ensuring its adaptability and alignment with real-world needs.

For a complete overview of the Integration Architecture (IA) project, please see the [Integration Architecture Documentation](https://github.com/National-Node-Net/integration-architecture-documentation).

## Prerequisites

Before using this repository, ensure you have the following dependencies installed:

* **Required Tooling:**
  * Java 21
  * Docker
  * Maven is optional as a [Maven Wrapper](https://maven.apache.org/wrapper/) is provided
* **Pipeline Requirements:**
  * Github Actions
* **Supported Kubernetes Versions:** N/A
* **System Requirements:**
  * Java 21
  * Docker
  * [Secure Agent Graph](https://github.com/National-Node-Net/secure-agent-graph)
  * Identity provider (see the [current supported providers](./INSTALLATION.md#identity-providers))
  * Data Dumping Destination (see the [current supported destinations](./INSTALLATION.md#data-dumping))

## Quick Start

Follow these steps to get started quickly with this repository. For detailed installation, configuration, and deployment, refer to the relevant MD files.

### 1. Download and Build

```sh
git clone https://github.com/National-Node-Net/data-extractor.git  
cd data-extractor
```

### 2. Run Build

```sh
./mvnw clean install
```

### 3. Full Installation

Refer to [INSTALLATION.md](INSTALLATION.md) for detailed installation steps, including required dependencies and setup configurations.

### 4. Uninstallation

For steps to remove this repository and its dependencies, see [UNINSTALL.md](UNINSTALL.md).

## Features

This is a script that can query data from an IA node and upload the result to an AWS S3 bucket.
The data extractor process consists of three parts:
- Acquiring an authentication token from an Identity Provider
- A SPAQRL query to the Secure Agent for data
- The option of uploading the results to an AWS S3 bucket

## Testing Guide

### Running Unit Tests

Navigate to the root of the project and run `mvn test` to run the tests for the repository.

## Public Funding Acknowledgment

This repository has been developed with public funding as part of the National Digital Twin Programme (NDTP), a UK Government initiative. NDTP, alongside its partners, has invested in this work to advance open, secure, and reusable digital twin technologies for any organisation, whether from the public or private sector, irrespective of size.

## License

This repository contains both source code and documentation, which are covered by different licenses:
- **Code:** Licensed under the Apache License 2.0.
- **Documentation:** Licensed under the Open Government Licence v3.0.

See [LICENSE.md](./LICENSE.md), [OGL_LICENSE.md](./OGL_LICENSE.md), and [NOTICE.md](./NOTICE.md) for details.

## Security and Responsible Disclosure

We take security seriously. If you believe you have found a security vulnerability in this repository, please follow our responsible disclosure process outlined in [SECURITY.md](./SECURITY.md).

## Contributing

We welcome contributions that align with the Programme’s objectives. Please read our [CONTRIBUTING.md](CONTRIBUTING.md) guidelines before submitting pull requests.

## Acknowledgements

This repository has benefited from collaboration with various organisations. For a list of acknowledgments, see [ACKNOWLEDGMENTS.md](ACKNOWLEDGMENTS.md).

## Support and Contact

For questions or support, check our Issues or contact the NDTP team on ndtp@businessandtrade.gov.uk.

**Maintained by the National Digital Twin Programme (NDTP).**

© Crown Copyright 2025. This work has been developed by the National Digital Twin Programme and is legally attributed to the Department for Business and Trade (UK) as the governing entity.
