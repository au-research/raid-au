import { describe, it, expect } from "vitest";
import { render, screen, fireEvent } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { GroupIdCell, ServicePointsTable } from "./ServicePointsTable";
import type { ServicePointWithMembers } from "@/types";

global.ResizeObserver = class {
  observe() {}
  unobserve() {}
  disconnect() {}
};

const makeRow = (overrides: Partial<ServicePointWithMembers> = {}): ServicePointWithMembers => ({
  id: 1,
  name: "Test SP",
  enabled: true,
  identifierOwner: "https://ror.org/123",
  adminEmail: "admin@test.com",
  techEmail: "tech@test.com",
  appWritesEnabled: true,
  members: [],
  ...overrides,
});

describe("GroupIdCell", () => {
  it("shows error icon with 'Group ID is invalid' tooltip when groupId is present but invalid", async () => {
    render(<GroupIdCell row={makeRow({ groupId: "non-existent-group-id", groupIdError: true })} />);

    const icon = screen.getByTestId("ErrorOutlineIcon");
    fireEvent.mouseOver(icon);

    expect(await screen.findByText("Group ID is invalid")).toBeInTheDocument();
  });

  it("shows cancel icon with 'No group ID configured' tooltip when groupId is empty", async () => {
    render(<GroupIdCell row={makeRow({ groupId: undefined })} />);

    const icon = screen.getByTestId("CancelIcon");
    fireEvent.mouseOver(icon);

    expect(await screen.findByText("No group ID configured")).toBeInTheDocument();
  });

  it("shows check icon with groupId value as tooltip when groupId is valid", async () => {
    const validGroupId = "abc-123-valid";
    render(<GroupIdCell row={makeRow({ groupId: validGroupId })} />);

    const icon = screen.getByTestId("CheckCircleOutlineIcon");
    fireEvent.mouseOver(icon);

    expect(await screen.findByText(validGroupId)).toBeInTheDocument();
  });
});

describe("ServicePointsTable", () => {
  it("applies disabled row styling when groupIdError is true", () => {
    const { container } = render(
      <MemoryRouter>
        <ServicePointsTable servicePoints={[makeRow({ groupId: "bad-id", groupIdError: true })]} />
      </MemoryRouter>
    );

    expect(container.querySelector(".row--group-id-error")).toBeInTheDocument();
  });
});
