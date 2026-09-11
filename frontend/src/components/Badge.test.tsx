import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { Badge } from "./Badge";

describe("Badge", () => {
  it("renders the value with underscores replaced by spaces", () => {
    render(<Badge value="NON_COMPLIANT" />);
    expect(screen.getByText("NON COMPLIANT")).toBeInTheDocument();
  });

  it("falls back to a neutral style for an unknown value", () => {
    render(<Badge value="SOMETHING_UNMAPPED" />);
    expect(screen.getByText("SOMETHING UNMAPPED")).toBeInTheDocument();
  });
});
