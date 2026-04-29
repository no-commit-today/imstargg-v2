const VALID_BODY = /^[0289CGJLPQRUVY]+$/;

function decode(input: string): string {
  let s = input.trim();
  try {
    if (s.includes("%")) s = decodeURIComponent(s);
  } catch {
    // ignore — fall through to validation
  }
  return s.toUpperCase();
}

export function normalizeTag(input: string): string {
  const decoded = decode(input);
  const body = decoded.startsWith("#") ? decoded.slice(1) : decoded;
  return `#${body}`;
}

export function stripHash(input: string): string {
  return normalizeTag(input).slice(1);
}

export function toUrlPath(input: string): string {
  return encodeURIComponent(normalizeTag(input));
}

export function isSameTag(a: string, b: string): boolean {
  return normalizeTag(a) === normalizeTag(b);
}

export function isValidTag(input: string): boolean {
  if (!input) return false;
  const decoded = decode(input);
  if (decoded === "#" || decoded === "") return false;
  const body = decoded.startsWith("#") ? decoded.slice(1) : decoded;
  if (body.length === 0) return false;
  return VALID_BODY.test(body);
}
