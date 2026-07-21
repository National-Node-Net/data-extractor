# Installation

**Repository:** `data-extractor`  
**Description:** `This file provides detailed installation steps, including required dependencies and configurations for Data Extractor.`

<!-- SPDX-License-Identifier: OGL-UK-3.0 -->

---

Data Extractor provides an automated approach to running queries against an [IA Node](https://www.github.com/National-Digital-) for the purpose of data extraction.

## Building

### Java

To build this application via Java run the following

```sh
./mvnw clean package
```

### Docker

To build this application via Docker run the following

```sh
docker build -t national-node-net/data-extractor -f Dockerfile .
```

## Running

### Prerequisites

We need the following components to be configured or running.

- [Identity Provider](#identity-providers)
- [Secure Agent](https://github.com/National-Node-Net/secure-agent-graph)
- [Data Dumping Destination](#data-dumping)

#### Identity Providers

The current supported Identity Providers are:
* Cognito
* Keycloak

A compose file is provided as part of [IA Node Access](https://github.com/National-Node-Net/ianode-access) which contains a local setup with test users
- [Keycloak](https://github.com/National-Node-Net/ianode-access/tree/pre/keyclock-local)
- [Cognito-Local](https://github.com/National-Node-Net/ianode-access/tree/pre/cognito-local)

#### Data Dumping

The current supported data dumping options are:

* exporting the data to an S3 bucket
  * you will need to set up an AWS account with access to write to an AWS S3 bucket
* printing the data to the logs

See the [configuration section](#configuration) for more details on the required properties and how to configure them.

### Java

```sh
DATA_EXTRACTOR_PROPERTIES=PATH/TO/YOUR/PROPERTIES.properties \
java -cp 'target/data-extractor-0.1.0.jar:target/lib/*' uk.gov.dbt.ndtp.extractor.DataExtractor
```

### Docker

```sh
docker run --rm \
-v PATH/TO/YOUR/PROPERTIES:/app/config \
-e DATA_EXTRACTOR_PROPERTIES=/app/config/PROPERTIES.properties \
--name data-extractor uk.gov.dbt.ndtp/data-extractor
```

## Configuration

The data extractor reads properties using [avaje-config](https://avaje.io/config/) via a file path defined in the environment variable `DATA_EXTRACTOR_PROPERTIES`.
The file can either be a `.properties` or `.yml` file however we will follow the `.properties` format for the rest of the documentation.
Examples of this configuration can be seen in [config directory](src/config), these examples are based on the setup of Cognito-Local from IA Node Access.

### Environment variables

|    Environment Variable     | Required |                                                        Description                                                         |
|-----------------------------|----------|----------------------------------------------------------------------------------------------------------------------------|
| `DATA_EXTRACTOR_PROPERTIES` | true     | Path to the configuration file defining all the properties.                                                                |
| `ENV`                       | false    | Controls the level of logging, a value of `local` will turn on `DEBUG` logging, any other value will be considered `INFO`. |

### Properties

### Authentication

#### General

|    property     |          description           |
|-----------------|--------------------------------|
| `auth.provider` | Either `keycloak` or `cognito` |

#### KeyCloak

These properties only need setting when `auth.provider` is `keycloak`

|       property        |               description               |
|-----------------------|-----------------------------------------|
| `keycloak.client.url` | The url for keycloak                    |
| `keycloak.client.id`  | The client id for keycloak              |
| `keycloak.grant.type` | The grant type for keycloak             |
| `keycloak.username`   | The username of the user to log in with |
| `keycloak.password`   | The password of the user to log in with |

#### Cognito

These properties only need setting when `auth.provider` is `cognito`

> [!NOTE]
> Both the S3 and Cognito properties require the `aws.region` property, it only needs to be included once

|       property       |               description               |
|----------------------|-----------------------------------------|
| `cognito.client.url` | The url for Cognito                     |
| `cognito.client.id`  | The client id for Cognito               |
| `cognito.username`   | The username of the user to log in with |
| `cognito.password`   | The password of the user to log in with |
| `aws.region`         | The region hosting AWS                  |

### Secure Agent Graph

|      property       |               description               |
|---------------------|-----------------------------------------|
| `graph.service.url` | The url of the secure agent             |
| `query.location`    | The path to a file containing the query |

### Data Dumping

#### General

|   property    |     description      |
|---------------|----------------------|
| `data.dumper` | Either `log` or `s3` |

#### S3

These properties only need setting when `data.dumper` is `s3`

> [!NOTE]
> Both the S3 and Cognito properties require the `aws.region` property, it only needs to be included once

|        property         |                      description                       |
|-------------------------|--------------------------------------------------------|
| `aws.region`            | The region hosting AWS                                 |
| `aws.s3.bucket.name`    | The AWS bucket name where S3 file needs to be uploaded |
| `aws.access.key.id`     | The AWS key id                                         |
| `aws.secret.access.key` | The AWS secret access key                              |

© Crown Copyright 2025. This work has been developed by the National Digital Twin Programme and is legally attributed to the Department for Business and Trade (UK) as the governing entity.
