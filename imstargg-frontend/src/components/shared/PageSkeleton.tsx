export function PageSkeleton({ rows = 6 }: { rows?: number }) {
  return (
    <main className="mx-auto w-full max-w-7xl space-y-6 px-4 py-6">
      <div className="card" style={{ padding: 24 }}>
        <div className="skel" style={{ height: 96, borderRadius: 12 }} />
      </div>
      <div style={{ display: "flex", flexDirection: "column", gap: 8 }}>
        {Array.from({ length: rows }).map((_, i) => (
          <div key={i} className="card" style={{ height: 64, padding: 12 }}>
            <div className="skel" style={{ width: "100%", height: "100%", borderRadius: 6 }} />
          </div>
        ))}
      </div>
    </main>
  );
}
