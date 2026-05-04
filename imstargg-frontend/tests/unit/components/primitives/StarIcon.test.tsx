import { describe, it, expect } from "vitest";
import { render, screen } from "@testing-library/react";
import StarIcon from "@/components/primitives/StarIcon";

describe("StarIcon", () => {
  it("기본 aria-label '스타플레이어' 가 노출된다", () => {
    render(<StarIcon />);
    expect(screen.getByLabelText("스타플레이어")).toBeInTheDocument();
  });

  it("label prop 로 커스텀 라벨을 지정한다", () => {
    render(<StarIcon label="MVP" />);
    expect(screen.getByLabelText("MVP")).toBeInTheDocument();
  });
});
