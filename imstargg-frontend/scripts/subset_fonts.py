"""KoBrawl·Pretendard 를 KS X 1001 한글 + Latin 서브셋 WOFF2 로 변환한다.

실행 (imstargg-frontend 디렉토리에서):
    uv run --with fonttools --with brotli python scripts/subset_fonts.py

산출물은 app/fonts/ 에 커밋한다. 원본: scripts/fonts-src/(KoBrawl),
node_modules/pretendard/(Pretendard — devDependency).
"""
from pathlib import Path

from fontTools.subset import Options, Subsetter, load_font, save_font

ROOT = Path(__file__).resolve().parent.parent


def ksx1001_hangul() -> set[int]:
    """KS X 1001 완성형 한글 음절 2,350자.

    Python 의 euc_kr 코덱이 플랫폼에 따라 CP949(전체 11,172자)로 동작하기도 하므로,
    KS X 1001 한글 영역의 EUC-KR 2바이트(lead 0xB0..0xC8, trail 0xA1..0xFE)를 직접
    디코드해 한글 음절만 수집한다 — 이 바이트 영역은 euc-kr/cp949 에서 동일하다.
    """
    result: set[int] = set()
    for lead in range(0xB0, 0xC9):
        for trail in range(0xA1, 0xFF):
            try:
                ch = bytes([lead, trail]).decode("euc_kr")
            except UnicodeDecodeError:
                continue
            cp = ord(ch)
            if 0xAC00 <= cp <= 0xD7A3:
                result.add(cp)
    return result


def target_unicodes() -> list[int]:
    cps = ksx1001_hangul()
    cps.update(range(0x0020, 0x007F))  # Basic Latin
    cps.update(range(0x3131, 0x3164))  # 한글 호환 자모
    cps.update(
        [0x00B7, 0x2013, 0x2014, 0x2018, 0x2019, 0x201C, 0x201D, 0x2026, 0x00D7, 0x2192]
    )  # 자주 쓰는 문장부호
    return sorted(cps)


def subset(src: Path, dst: Path, *, drop_layout: bool = False) -> None:
    options = Options()
    options.flavor = "woff2"
    # 디스플레이 폰트(KoBrawl)는 한글 타이틀 용도라 GPOS/GSUB 레이아웃 피처가
    # 불필요하다 — 제거해 용량을 줄인다. 본문 폰트(Pretendard)는 커닝을 유지한다.
    options.layout_features = [] if drop_layout else ["*"]
    options.name_IDs = ["*"]
    options.notdef_outline = True
    font = load_font(str(src), options)
    subsetter = Subsetter(options)
    subsetter.populate(unicodes=target_unicodes())
    subsetter.subset(font)
    save_font(font, str(dst), options)
    print(f"{dst.name}: {dst.stat().st_size / 1024:.0f} KB")


if __name__ == "__main__":
    hangul_count = len(ksx1001_hangul())
    assert hangul_count == 2350, f"KS X 1001 음절 수가 2,350 이 아님: {hangul_count}"
    out = ROOT / "app" / "fonts"
    out.mkdir(parents=True, exist_ok=True)
    subset(
        ROOT / "scripts" / "fonts-src" / "KoBrawl_Gothic40.otf",
        out / "KoBrawlGothic-subset.woff2",
        drop_layout=True,
    )
    subset(
        ROOT
        / "node_modules"
        / "pretendard"
        / "dist"
        / "web"
        / "variable"
        / "woff2"
        / "PretendardVariable.woff2",
        out / "PretendardVariable-subset.woff2",
    )
