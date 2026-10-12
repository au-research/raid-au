import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import type { ServicePointMember, ServicePointWithMembers } from "@/types";
import { ServicePointUsersList } from "./ServicePointUsersList";

const mutations = vi.hoisted(() => ({
  modifyUserAccess: vi.fn(),
  removeUserFromServicePoint: vi.fn(),
  removeUserFromGroupAdmins: vi.fn(),
  addUserToGroupAdmins: vi.fn(),
}));

vi.mock("@/contexts/keycloak-context", () => ({
  useKeycloak: () => ({ token: "t" }),
}));

vi.mock("@/components/snackbar", () => ({
  useSnackbar: () => ({ openSnackbar: vi.fn() }),
}));

vi.mock("./useServicePointMutation", () => ({
  useModifyUserAccess: () => ({ mutate: mutations.modifyUserAccess }),
  useRemoveUserFromServicePoint: () => ({ mutate: mutations.removeUserFromServicePoint }),
  useRemoveUserFromGroupAdmins: () => ({ mutate: mutations.removeUserFromGroupAdmins }),
  useAddUserToGroupAdmins: () => ({ mutate: mutations.addUserToGroupAdmins }),
}));

const GROUP_ID = "group-a";
const DEFAULTS = ["default-roles-raid", "offline_access", "uma_authorization"];

const makeMember = (id: string, firstName: string, lastName: string, roles: string[]): ServicePointMember => ({
  id,
  groupId: GROUP_ID,
  roles: [...DEFAULTS, ...roles],
  attributes: {
    username: [`${firstName}.${lastName}`.toLowerCase()],
    firstName: [firstName],
    lastName: [lastName],
    email: [`${firstName}.${lastName}@example.edu.au`.toLowerCase()],
    activeGroupId: [GROUP_ID],
  },
});

const MEMBERS = [
  makeMember("u1", "Avery", "Lin", [
    "service-point-user",
    "group-admin",
    `service-point-admin:${GROUP_ID}`,
    "service-point-admin:group-b",
    "service-point-admin:group-c",
  ]),
  makeMember("u2", "Sam", "Okafor", ["service-point-user"]),
  makeMember("u3", "Priya", "Natarajan", []),
  // Administers another service point only.
  makeMember("u4", "Jordan", "Ellis", ["service-point-user", "group-admin", "service-point-admin:group-b"]),
  // Operators are not listed.
  makeMember("u5", "Riley", "Operations", ["service-point-user", "operator"]),
];

const renderList = (members: ServicePointMember[] = MEMBERS) =>
  render(
    <ServicePointUsersList
      servicePointWithMembers={
        { id: 1, name: "Example University Library", groupId: GROUP_ID, members } as unknown as ServicePointWithMembers
      }
    />
  );

const rowFor = (name: string) => screen.getByText(name).closest("li") as HTMLElement;

const openMenuFor = (name: string) =>
  fireEvent.click(screen.getByRole("button", { name: `Actions for ${name}` }));

describe("ServicePointUsersList (RAID-919)", () => {
  beforeEach(() => {
    Object.values(mutations).forEach((fn) => fn.mockReset());
  });

  it("shows status for this service point, not raw role names", () => {
    renderList();

    expect(screen.queryByText(/service-point-admin:/)).not.toBeInTheDocument();
    expect(screen.queryByText(/default-roles-raid/)).not.toBeInTheDocument();
    expect(screen.queryByText(/group-admin/)).not.toBeInTheDocument();

    const avery = rowFor("Avery Lin");
    expect(within(avery).getByText("Service Point User")).toBeInTheDocument();
    expect(within(avery).getByText("Service Point Admin")).toBeInTheDocument();
    expect(within(rowFor("Priya Natarajan")).getByText("Pending approval")).toBeInTheDocument();
  });

  it("does not list or count operators", () => {
    renderList();

    expect(screen.queryByText("Riley Operations")).not.toBeInTheDocument();
    expect(screen.queryByText("Operator")).not.toBeInTheDocument();
    expect(screen.getAllByRole("listitem")).toHaveLength(4);
    expect(screen.getByRole("button", { name: /^All\s*4$/ })).toBeInTheDocument();
  });

  it("does not show an admin of another service point as admin here", () => {
    renderList();

    expect(within(rowFor("Jordan Ellis")).queryByText("Service Point Admin")).not.toBeInTheDocument();

    openMenuFor("Jordan Ellis");
    fireEvent.click(screen.getByRole("menuitem", { name: "Make Service Point Admin" }));
    expect(mutations.addUserToGroupAdmins).toHaveBeenCalledWith({ userId: "u4", groupId: GROUP_ID, token: "t" });
  });

  it("lists pending requests first", () => {
    renderList();

    const items = screen.getAllByRole("listitem");
    expect(within(items[0]).getByText("Priya Natarajan")).toBeInTheDocument();
    // Approved members follow alphabetically.
    expect(within(items[1]).getByText("Avery Lin")).toBeInTheDocument();
  });

  it("filters by Service Point Users, pending approval and Service Point Admins, with counts", () => {
    renderList();

    expect(screen.getByRole("button", { name: /^All\s*4$/ })).toHaveAttribute("aria-pressed", "true");

    fireEvent.click(screen.getByRole("button", { name: /^Service Point Users\s*3$/ }));
    expect(screen.getAllByRole("listitem")).toHaveLength(3);
    expect(screen.queryByText("Priya Natarajan")).not.toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: /^Pending approval\s*1$/ }));
    expect(screen.getAllByRole("listitem")).toHaveLength(1);
    expect(screen.getByText("Priya Natarajan")).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: /^Service Point Admins\s*1$/ }));
    expect(screen.getAllByRole("listitem")).toHaveLength(1);
    expect(screen.getByText("Avery Lin")).toBeInTheDocument();
  });

  it("shows a message when a filter matches nobody", () => {
    renderList([MEMBERS[1]]);

    fireEvent.click(screen.getByRole("button", { name: /^Pending approval/ }));
    expect(screen.getByText("No requests waiting for approval.")).toBeInTheDocument();
  });

  it("offers only the actions that apply to a pending member", () => {
    renderList();

    openMenuFor("Priya Natarajan");
    expect(screen.queryByRole("menuitem", { name: /Revoke Service Point User/ })).not.toBeInTheDocument();
    // Not offered until they are a Service Point User.
    expect(screen.queryByRole("menuitem", { name: /Make Service Point Admin/ })).not.toBeInTheDocument();

    fireEvent.click(screen.getByRole("menuitem", { name: "Approve as Service Point User" }));
    expect(mutations.modifyUserAccess).toHaveBeenCalledWith({
      userId: "u3",
      userGroupId: GROUP_ID,
      operation: "grant",
      token: "t",
    });
  });

  it("revokes membership and admin rights for an admin", () => {
    renderList();

    openMenuFor("Avery Lin");
    expect(screen.queryByRole("menuitem", { name: /Make Service Point Admin/ })).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole("menuitem", { name: "Remove Service Point Admin" }));
    expect(mutations.removeUserFromGroupAdmins).toHaveBeenCalledWith({ userId: "u1", groupId: GROUP_ID, token: "t" });

    openMenuFor("Avery Lin");
    fireEvent.click(screen.getByRole("menuitem", { name: "Revoke Service Point User" }));
    expect(mutations.modifyUserAccess).toHaveBeenCalledWith(expect.objectContaining({ userId: "u1", operation: "revoke" }));
  });

  it("asks for confirmation before removing someone", async () => {
    renderList();

    openMenuFor("Sam Okafor");
    fireEvent.click(screen.getByRole("menuitem", { name: /Remove from service point/ }));

    const dialog = screen.getByRole("dialog", { name: "Remove Sam Okafor from this service point?" });
    expect(mutations.removeUserFromServicePoint).not.toHaveBeenCalled();

    fireEvent.click(within(dialog).getByRole("button", { name: "Cancel" }));
    await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument());
    expect(mutations.removeUserFromServicePoint).not.toHaveBeenCalled();

    openMenuFor("Sam Okafor");
    fireEvent.click(screen.getByRole("menuitem", { name: /Remove from service point/ }));
    fireEvent.click(within(screen.getByRole("dialog")).getByRole("button", { name: "Remove" }));
    expect(mutations.removeUserFromServicePoint).toHaveBeenCalledWith({ userId: "u2", groupId: GROUP_ID, token: "t" });
  });
});
