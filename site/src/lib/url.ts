/** Parses an absolute URL. Returns `null` when the value is not one. */
export function parseUrl(value: string) {
  try {
    return new URL(value);
  } catch {
    return null;
  }
}
