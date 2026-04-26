export default function NotFound() {
  return (
    <div className="main">
      <div className="card" style={{ padding: 60, textAlign: "center" }}>
        <div style={{ fontSize: 48, marginBottom: 12, opacity: 0.3 }}>🚧</div>
        <h2
          style={{
            fontFamily: "var(--font-display)",
            margin: "0 0 8px 0",
            letterSpacing: "0.02em",
          }}
        >
          404
        </h2>
        <p style={{ color: "var(--color-neutral-500)", margin: 0 }}>해당 경로를 찾을 수 없어요.</p>
      </div>
    </div>
  );
}
