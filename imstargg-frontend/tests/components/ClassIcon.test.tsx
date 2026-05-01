import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import ClassIcon from "@/components/primitives/ClassIcon";

describe("ClassIcon", () => {
  it("DAMAGE_DEALER 일 때 icon_class_damage.svg 이미지를 렌더한다", () => {
    render(<ClassIcon role="DAMAGE_DEALER" />);
    const img = screen.getByRole("img", { name: "대미지 딜러" });
    expect(img.getAttribute("src")).toBe("/assets/icon_class_damage.svg");
  });

  it("TANK 일 때 icon_class_tank.svg 이미지를 렌더한다", () => {
    render(<ClassIcon role="TANK" />);
    const img = screen.getByRole("img", { name: "탱커" });
    expect(img.getAttribute("src")).toBe("/assets/icon_class_tank.svg");
  });
});
