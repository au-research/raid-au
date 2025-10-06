# RAiD Agency Application

The RAiD Agency Application is a React-based web application for managing Research Activity Identifiers (RAiDs). This application allows users to create, view, edit, and manage RAiDs through an intuitive user interface.

## Overview

The RAiD Agency Application provides a comprehensive interface for researchers and administrators to:

- Create and mint new RAiDs
- Edit existing RAiD metadata
- View RAiD history and changes
- Manage service points
- Collaborate with other contributors
- Handle organization lookups and integrations

## Prerequisites

- Node.js (version 16 or higher)
- npm (version 8 or higher)
- Access to a Keycloak authentication server

## Getting Started

### First steps

#### Prerequisites

- Clone the repository from https://github.com/au-research/raid-au
- Ensure Java 17 is installed:
  - Linux: `sudo apt install openjdk-17-jre-headless`
  - macOS: `brew install openjdk@17`
- Ensure Docker is installed and running

#### Backend Setup

1. Navigate to the root directory of the project
2. Execute the following command to start all backend services:
   ```
   ./gradlew clean dockerComposeUp bootRun flywayClean flywayMigrate
   ```

#### Frontend Setup

1. Open a new terminal window
2. Navigate to the `raid-au/raid-agency-app` directory (the frontend component)
3. Install the frontend dependencies:
   ```
   npm i
   ```
4. Duplicate `.env.template` as `.env` and set `VITE_KEYCLOAK_E2E_PASSWORD` to "password"

#### Keycloak Configuration

1. Access Keycloak at http://localhost:8001 and log in with credentials: username "admin", password "admin"
2. Change the realm from "master" to "raid" using the dropdown menu in the upper left corner
3. Navigate to "Client scopes" in the left navigation menu and create a client scope named "web-origins"
4. Within the newly created "web-origins" client scope, go to the "Mappers" tab
5. Click "Add mapper" → "From predefined mappers" and select "allowed web origins"
6. Navigate to "Clients" in the left sidebar and select the "raid-api" client
7. Access the "Client scopes" tab and add the "web-origins" scope

#### Starting the Application

1. Ensure you are in the `raid-agency-app` directory
2. Start the React application:
   ```
   npm run dev
   ```
3. Access the application at http://localhost:7080
4. To log in, click any login button and use the credentials: username "raid-test-user", password "password"

### Build for Production

Build the application for production:

```bash
npm run build
```

This will create a production-ready build in the `dist` directory.

## Testing

Run tests with:

```bash
npm run test
```

End-to-end tests can be run with:

```bash
npm run test:e2e
```

## Project Structure

- `src/`: Source code
  - `auth/`: Authentication logic
  - `components/`: Reusable UI components
  - `constants/`: Application constants
  - `containers/`: Logic heavy components
  - `contexts/`: React contexts for state management
  - `entities/`: Entity-specific components and logic
  - `error/`:  Error boundary component and logic
  - `generated/`: Generated TypeScript interfaces
  - `mapping/`: Mapping context
  - `pages/`: Page components
  - `references/`: Reference data
  - `routes/`: Application routes
  - `services/`: API services
  - `utils/`: Utility functions

## Authentication

The application uses Keycloak for authentication and authorization. Users can log in with:

- Username/password
- ORCID OAuth

## Key Features

- **Service Point Management**: Create and manage service points
- **RAID Creation**: Mint new RAIDs with comprehensive metadata
- **RAID Management**: View, edit, and track RAIDs
- **Collaboration**: Invite others to contribute to RAIDs
- **Organization Integration**: Look up organizations using ROR identifiers

## API Documentation

API documentation is available in the [postman/api-documentation.md](postman/api-documentation.md) file.

## License

This project is licensed under the [Apache 2.0 License](https://github.com/au-research/raid-au?tab=Apache-2.0-1-ov-file#readme).
