import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { PlayerAvatar } from "@/components/primitives/PlayerAvatar";

describe("PlayerAvatar", () => {
  it("이름의 첫 글자를 이니셜로 노출한다", () => {
    render(<PlayerAvatar name="惡鬼滅殺" />);
    expect(screen.getByText("惡")).toBeInTheDocument();
  });
});
