interface Props {
  count?: number;
}

export function BattleSkeleton({ count = 25 }: Props) {
  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 8 }}>
      {Array.from({ length: count }).map((_, i) => (
        <div key={i} className="card" style={{ height: 64, padding: 12 }}>
          <div className="skel" style={{ width: "100%", height: "100%", borderRadius: 6 }} />
        </div>
      ))}
    </div>
  );
}
