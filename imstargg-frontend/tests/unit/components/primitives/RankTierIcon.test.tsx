import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import RankTierIcon from "@/components/primitives/RankTierIcon";

describe("RankTierIcon", () => {
  it("BRONZE_2 는 bronze 스티커 + '2' 를 렌더한다", () => {
    render(<RankTierIcon tier="BRONZE_2" />);
    expect(screen.getByRole("img", { hidden: true })).toHaveAttribute(
      "src",
      "/assets/icon_rank_sticker_bronze.webp",
    );
    expect(screen.getByText("2")).toBeInTheDocument();
  });

  it("PRO 는 pro 스티커 + 숫자 없음", () => {
    render(<RankTierIcon tier="PRO" />);
    expect(screen.getByRole("img", { hidden: true })).toHaveAttribute(
      "src",
      "/assets/icon_rank_sticker_pro.webp",
    );
    expect(screen.queryByText(/^\d$/)).toBeNull();
  });
});
