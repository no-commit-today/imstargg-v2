import { describe, expect, it } from "vitest";
import { render } from "@testing-library/react";
import { AdUnit } from "@/components/ads/AdUnit";

describe("AdUnit", () => {
  it("slot 이 비어 있으면 아무것도 렌더링하지 않는다", () => {
    const { container } = render(<AdUnit slot="" enabled={true} />);
    expect(container).toBeEmptyDOMElement();
  });

  it("enabled=false 면 slot 이 있어도 렌더링하지 않는다", () => {
    const { container } = render(<AdUnit slot="1234567890" enabled={false} />);
    expect(container).toBeEmptyDOMElement();
  });

  it("enabled + slot 이면 ins.adsbygoogle 를 슬롯·클라이언트 속성과 함께 렌더링한다", () => {
    const { container } = render(<AdUnit slot="1234567890" enabled={true} />);
    const ins = container.querySelector("ins.adsbygoogle");
    expect(ins).not.toBeNull();
    expect(ins).toHaveAttribute("data-ad-client", "ca-pub-5084387143997125");
    expect(ins).toHaveAttribute("data-ad-slot", "1234567890");
    expect(ins).toHaveAttribute("data-ad-format", "auto");
    expect(ins).toHaveAttribute("data-full-width-responsive", "true");
  });
});
