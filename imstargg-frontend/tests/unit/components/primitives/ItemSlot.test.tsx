import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import ItemSlot from "@/components/primitives/ItemSlot";

describe("ItemSlot", () => {
  it("owned + imageUrl 이 있으면 해당 이미지를 렌더한다", () => {
    render(<ItemSlot kind="gadget" imageUrl="https://cdn/x.webp" owned />);
    expect(screen.getByRole("img", { hidden: true })).toHaveAttribute("src", "https://cdn/x.webp");
  });

  it("owned + imageUrl null 이면 base placeholder 를 렌더한다", () => {
    render(<ItemSlot kind="gadget" imageUrl={null} owned />);
    expect(screen.getByRole("img", { hidden: true })).toHaveAttribute(
      "src",
      "/assets/gadget_base_empty.webp",
    );
  });

  it("gadget 미보유는 base 를 opacity + grayscale 로 렌더한다", () => {
    render(<ItemSlot kind="gadget" imageUrl={null} owned={false} />);
    const img = screen.getByRole("img", { hidden: true });
    expect(img).toHaveAttribute("src", "/assets/gadget_base_empty.webp");
    expect(img.style.opacity).toBe("0.3");
    expect(img.style.filter).toBe("grayscale(1)");
  });

  it("hyperCharge 미보유는 base 를 opacity + grayscale 로 렌더한다", () => {
    render(<ItemSlot kind="hyperCharge" imageUrl={null} owned={false} />);
    const img = screen.getByRole("img", { hidden: true });
    expect(img).toHaveAttribute("src", "/assets/hypercharge_base_empty.webp");
    expect(img.style.opacity).toBe("0.3");
    expect(img.style.filter).toBe("grayscale(1)");
  });

  it("buffy 는 imageUrl 이 없으면 lucide 아이콘 fallback 을 렌더한다", () => {
    render(<ItemSlot kind="buffy" imageUrl={null} owned ariaLabel="버피 가젯" />);
    expect(screen.getByLabelText("버피 가젯")).toBeInTheDocument();
    expect(screen.queryByRole("img", { hidden: true })).toBeNull();
  });

  it("gear EPIC 의 base placeholder 는 epic_super 자산이다", () => {
    render(<ItemSlot kind="gear" imageUrl={null} owned gearRarity="EPIC" />);
    expect(screen.getByRole("img", { hidden: true })).toHaveAttribute(
      "src",
      "/assets/gear_epic_super_base_empty.webp",
    );
  });

  it("name prop 이 있으면 title 속성을 렌더한다", () => {
    render(<ItemSlot kind="gadget" imageUrl="https://cdn/x.webp" owned name="가젯이름" />);
    expect(screen.getByRole("img", { hidden: true })).toHaveAttribute("title", "가젯이름");
  });

  it("buffy 미보유는 lucide 아이콘 + opacity 0.3 으로 렌더한다", () => {
    render(<ItemSlot kind="buffy" imageUrl={null} owned={false} ariaLabel="버피 가젯 미보유" />);
    expect(screen.getByLabelText("버피 가젯 미보유")).toHaveStyle({ opacity: "0.3" });
  });
});
