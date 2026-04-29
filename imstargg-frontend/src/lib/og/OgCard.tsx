export interface OgCardStat {
  label: string;
  value: string;
}

export interface OgCardProps {
  eyebrow: string;
  name: string;
  tag: string;
  stats: OgCardStat[];
}

export function OgCard({ eyebrow, name, tag, stats }: OgCardProps) {
  return (
    <div
      style={{
        width: "100%",
        height: "100%",
        display: "flex",
        flexDirection: "column",
        background: "#14161B",
        padding: "64px 80px",
        position: "relative",
        fontFamily: "KoBrawl Gothic",
      }}
    >
      <div
        style={{
          display: "flex",
          fontFamily: "Lilita One",
          fontSize: 32,
          color: "#FFC60A",
          letterSpacing: "0.08em",
        }}
      >
        IMSTARGG · {eyebrow}
      </div>

      <div
        style={{
          flex: 1,
          display: "flex",
          flexDirection: "column",
          justifyContent: "center",
        }}
      >
        <div
          style={{
            display: "flex",
            fontSize: 96,
            color: "#FFFFFF",
            lineHeight: 1.1,
          }}
        >
          {name}
        </div>
        <div
          style={{
            display: "flex",
            marginTop: 12,
            fontFamily: "monospace",
            fontSize: 32,
            color: "#FFC60A",
          }}
        >
          {tag}
        </div>
        {stats.length > 0 && (
          <div
            style={{
              marginTop: 36,
              display: "flex",
              gap: 32,
              fontSize: 36,
              color: "rgba(255,255,255,0.85)",
            }}
          >
            {stats.map((s, i) => (
              <div key={i} style={{ display: "flex", gap: 12 }}>
                <span style={{ color: "rgba(255,255,255,0.5)" }}>{s.label}</span>
                <span>{s.value}</span>
              </div>
            ))}
          </div>
        )}
      </div>

      <div
        style={{
          display: "flex",
          position: "absolute",
          left: 0,
          right: 0,
          bottom: 0,
          height: 8,
          background: "#FFC60A",
        }}
      />
    </div>
  );
}
