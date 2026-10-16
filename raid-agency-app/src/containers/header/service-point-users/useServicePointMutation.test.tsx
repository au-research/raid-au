import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { act, renderHook, waitFor } from "@testing-library/react";
import type { ReactNode } from "react";
import { describe, expect, it, vi } from "vitest";
import { useAddUserToGroupAdmins, useModifyUserAccess, useRemoveUserFromGroupAdmins } from "./useServicePointMutation";

vi.mock("@/services/service-points", () => ({
  updateUserServicePointUserRole: vi.fn().mockResolvedValue({}),
  addUserToGroupAdmins: vi.fn().mockResolvedValue({}),
  removeUserFromGroupAdmins: vi.fn().mockResolvedValue({}),
  removeUserFromServicePoint: vi.fn().mockResolvedValue(undefined),
}));

const wrapper = ({ children }: { children: ReactNode }) => (
  <QueryClientProvider client={new QueryClient()}>{children}</QueryClientProvider>
);

// RAID-919: success messages use the same wording as the Users tab.
describe("service point mutation success messages", () => {
  it.each([
    ["grant", "Approved as Service Point User"],
    ["revoke", "Revoked Service Point User"],
  ] as const)("says what %s did", async (operation, message) => {
    const snackbar = { openSnackbar: vi.fn() };
    const { result } = renderHook(() => useModifyUserAccess(snackbar), { wrapper });

    act(() => result.current.mutate({ userId: "u1", userGroupId: "g1", operation, token: "t" }));

    await waitFor(() => expect(snackbar.openSnackbar).toHaveBeenCalledWith(message, 3000, "success"));
  });

  it("names the Service Point Admin changes", async () => {
    const snackbar = { openSnackbar: vi.fn() };
    const { result: add } = renderHook(() => useAddUserToGroupAdmins(snackbar), { wrapper });
    const { result: remove } = renderHook(() => useRemoveUserFromGroupAdmins(snackbar), { wrapper });

    act(() => add.current.mutate({ userId: "u1", groupId: "g1", token: "t" }));
    await waitFor(() => expect(snackbar.openSnackbar).toHaveBeenCalledWith("Made Service Point Admin", 3000, "success"));

    act(() => remove.current.mutate({ userId: "u1", groupId: "g1", token: "t" }));
    await waitFor(() => expect(snackbar.openSnackbar).toHaveBeenCalledWith("Removed Service Point Admin", 3000, "success"));
  });
});
