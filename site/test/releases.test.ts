import { expect, test } from "bun:test";
import { formatSize, parseRelease, releaseLink } from "../src/lib/releases";

const tag = "0.3.0";
function asset(name: string, release = tag) {
  return {
    name,
    browser_download_url: `https://github.com/6b6t/AnarchyClient/releases/download/${release}/${name}`,
    size: 5_131_984,
    state: "uploaded",
  };
}
function release(assets: unknown[]) {
  return {
    tag_name: tag,
    html_url: `https://github.com/6b6t/AnarchyClient/releases/tag/${tag}`,
    published_at: "2026-11-14T12:00:00Z",
    assets,
  };
}

test("the release offers its one mod JAR and reads the Minecraft version from its name", () => {
  const parsed = parseRelease(
    release([
      asset("anarchyclient-mc-26.3-0.3.0.jar"),
      asset("anarchyclient-mc-26.3-0.3.0-sources.jar"),
      asset("checksums.txt"),
    ]),
  );
  expect(parsed?.jar?.name).toBe("anarchyclient-mc-26.3-0.3.0.jar");
  expect(parsed?.minecraft).toBe("26.3");
  if (!parsed?.jar) throw new Error("Expected the release JAR");
  expect(formatSize(parsed.jar.size)).toBe("4.9 MB");
});

test("ambiguous, foreign, or other-tag JARs fall back to the release page", () => {
  const ambiguous = parseRelease(
    release([asset("anarchyclient-mc-26.3-0.3.0.jar"), asset("anarchyclient-mc-26.2-0.3.0.jar")]),
  );
  expect(ambiguous?.jar).toBeUndefined();
  expect(releaseLink(ambiguous)).toBe(`https://github.com/6b6t/AnarchyClient/releases/tag/${tag}`);
  const foreign = parseRelease(
    release([
      {
        ...asset("anarchyclient-mc-26.3-0.3.0.jar"),
        browser_download_url: "https://example.com/a.jar",
      },
      asset("anarchyclient-mc-26.3-0.2.9.jar", "0.2.9"),
    ]),
  );
  expect(foreign?.jar).toBeUndefined();
  expect(foreign?.minecraft).toBeUndefined();
});

test("missing or unpublished release metadata leaves a usable GitHub link", () => {
  expect(parseRelease(null)).toBeNull();
  expect(parseRelease({ ...release([]), draft: true })).toBeNull();
  expect(parseRelease({ ...release([]), prerelease: true })).toBeNull();
  expect(parseRelease({ ...release([]), html_url: "https://example.com/release" })).toBeNull();
  expect(releaseLink(null)).toBe("https://github.com/6b6t/AnarchyClient/releases/latest");
});
