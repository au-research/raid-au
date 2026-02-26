# Raid Registration Agency

## Table of Contents
1. [Getting Started](#getting-started)
2. [Installation](#installation)
3. [Running the Application](#running-the-application)
4. [Project Structure](#project-structure)
5. [API Documentation](#api-documentation)
6. [Contributing](#contributing)
7. [License](#license)
## Getting Started
To get started you'll need the following tools installed in your machine:
- Node.js >= 18
- Java >= 17
- Docker >= 27
## Installation
1. Clone the repository
```bash
git clone  https://github.com/au-research/raid-au.git
```
2. Build the API
```bash
cd raid-au
./gradlew build
```
3. Install dependencies for the app
```bash
cd raid-agency-app
npm install
```
## Running the Application
1. Start the API
```bash
./gradlew dockerComposeUp bootRun -D spring.profiles.active=dev
```
2. Start the app
```bash
cd raid-agency-app
npm run dev
```
## Project Structure
|                      |                                                                                                |
| -------------------- | ---------------------------------------------------------------------------------------------- |
| `api-svc/db`         | The database migrations (handled by flyway) and generated database classes (handled by JOOQ)   |
| `api-svc/idl-raid-v2`| OpenApi specs and code generation for API controllers                                          |
| `api-svc/raid-api`   | API Spring Boot application                                                                    | 
| `iam/realms`         | Keycloak configuration used in local environment/integration tests. Loaded at startup          |
| `iam/src`            | Keycloak SPIs to handle group/raid permissions.                                                |
| `sso/`               | Docker config for Satosa. This allows SAML authentication between AAF and Keycloak for eduGAIN |

## API Documentation
* [Swagger](https://api.demo.raid.org.au/swagger-ui/index.html#/raid/findRaidByName)
* [RAiD Metadata Schema](https://metadata.raid.org/en/latest/index.html)

# Contributing
We welcome contributions from the community! Whether you're fixing bugs, adding new features, improving documentation, or suggesting ideas, we'd love to have your input. Feel free to raise a pull request (include tests).
# License
This project is licensed under the Apache 2.0 License - see the license.txt file for details.
 