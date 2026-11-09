import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { UngradedTierDescription } from "@app/_components/statistics/UngradedTierDescription";

describe("UngradedTierDescription", () => {
  it("범례 문구를 그린다", () => {
    render(<UngradedTierDescription />);
    expect(
      screen.getByText("– 표본 부족: 전투 수가 적어 티어를 매기지 않습니다."),
    ).toBeInTheDocument();
  });
});
