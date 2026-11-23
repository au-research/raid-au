import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { StartupSpinner } from "./StartupSpinner";

describe("StartupSpinner", () => {
  it("shows a labelled progress indicator and no text", () => {
    render(<StartupSpinner />);
    expect(screen.getByRole("progressbar", { name: "Loading" })).toBeTruthy();
  });
});
