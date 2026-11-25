# RAID Agency App Testing Strategy

This document outlines a possible testing strategy for the RAID Agency App, including guidelines for what and how to test.

## 1. Testing Tools and Framework

The project uses the following testing tools:

- **Vitest**: Main testing framework for unit and component tests
- **@testing-library/react**: For testing React components
- **@testing-library/jest-dom**: For DOM testing assertions
- **Playwright**: For end-to-end (E2E) testing

## 2. Test Types and Coverage Goals

### 2.1 Unit Tests

**Target**: Core utility functions, helpers, and services

**Examples**:

- `src/utils/string-utils/string-utils.test.ts`
- `src/utils/date-utils/date-utils.test.ts`
- `src/utils/api-utils/api-utils.test.ts`

**Guidelines**:

- Each utility module should have its own test file
- Focus on testing edge cases and input validations
- Keep unit tests fast and isolated

### 2.2 Component Tests

**Target**: React components, especially reusable UI components

**Guidelines**:

- Test component rendering
- Test user interactions (clicks, input changes)
- Test state changes
- Mock external dependencies (services, contexts)
- Use React Testing Library's user-centric approach

**Example Test Structure**:

```typescript
import { render, screen, fireEvent } from "@testing-library/react";
import { MyComponent } from "./MyComponent";

describe("MyComponent", () => {
  it("renders correctly", () => {
    render(<MyComponent />);
    expect(screen.getByText("Expected Text")).toBeInTheDocument();
  });

  it("handles user interaction", () => {
    render(<MyComponent />);
    fireEvent.click(screen.getByRole("button"));
    expect(screen.getByText("Changed Text")).toBeInTheDocument();
  });
});
```

### 2.3 Integration Tests

**Target**: Feature flows involving multiple components

**Coverage Goal**: Key user flows should be covered

**Guidelines**:

- Focus on testing important user flows (e.g., creating/editing a RAID)
- Test form validations and submissions
- Mock API responses but test the integration of components

### 2.4 End-to-End Tests

**Target**: Critical application flows from a user perspective

**Coverage Goal**: Cover critical paths and full application workflows

**Setup**:

- Use Playwright for browser-based testing
- Tests are in the `e2e/` directory
- Can be run with `npm run e2e`

**Guidelines**:

- Focus on key user flows (login, creating/editing RAIDs, service point management)
- Test across supported browsers (currently configured for Chromium)
- Set up test isolation and cleanup to ensure reliability

## 3. Test Organization

### File Structure

- Unit tests should be co-located with the code they test
- Test files should use the naming convention `*.test.ts` or `*.test.tsx`
- E2E tests should be placed in the `e2e/` directory

### Test Grouping

- Use `describe` blocks to group related tests
- Use nested `describe` blocks for complex components
- Use clear, descriptive test names that explain the expected behavior

## 4. Mocking Strategy

### API Mocking

- Use mock functions for API calls in unit and component tests
- Consider using Mock Service Worker (MSW) for more complex API mocking scenarios
- Create fixture files for common API responses

### Context Mocking

- Create test providers for React contexts
- Provide utilities for setting up test contexts with controlled values

## 5. CI Integration

- Tests should run on every PR
- Unit and component tests should run on each commit
- E2E tests should run before merging to main branches

## 6. Test Debugging

- Use `test.only` or `it.only` to run specific tests during development
- Use the built-in debugger in your IDE to step through tests
- For Playwright tests, use the `--debug` flag for visual debugging

## 7. Testing Checklist for New Features

1. Write unit tests for any new utility functions
2. Add component tests for new UI components
3. Update or add integration tests for feature flows
4. Add E2E tests for critical user journeys
5. Test edge cases and error handling
6. Verify accessibility in component tests

## 8. Next Steps for Improving Test Coverage

1. Add component tests for reusable UI components
2. Complete E2E test suite for critical user flows
3. Add integration tests for core features
4. Implement API mocking strategy for more reliable tests
5. Set up code coverage reporting in CI pipeline
