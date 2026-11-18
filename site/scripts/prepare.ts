import { mkdir, writeFile } from "node:fs/promises";
import { parseRelease } from "../src/lib/releases";
import { site } from "../src/lib/site";
import { publicPages } from "./pages";

async function fetchRelease() {
  try {
    const token = process.env.GITHUB_TOKEN;
    const response = await fetch(
      "https://api.github.com/repos/6b6t/AnarchyClient/releases/latest",
      {
        signal: AbortSignal.timeout(10_000),
        headers: {
          Accept: "application/vnd.github+json",
          ...(token ? { Authorization: `Bearer ${token}` } : {}),
        },
      },
    );
    if (!response.ok) {
      console.warn(`Release snapshot unavailable (HTTP ${response.status}); using GitHub links.`);
      return null;
    }
    const release = parseRelease(await response.json());
    if (!release) console.warn("Release metadata is invalid; using GitHub links.");
    return release;
  } catch {
    console.warn("Release snapshot unavailable; using GitHub links.");
    return null;
  }
}

const release = await fetchRelease();
await mkdir(".generated", { recursive: true });
await writeFile(".generated/release.json", `${JSON.stringify(release, null, 2)}\n`);
const paths = await publicPages();
const sitemap = `<?xml version="1.0" encoding="UTF-8"?>\n<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">\n${paths
  .map((path) => `  <url><loc>${new URL(path, site.url).href}</loc></url>`)
  .join("\n")}\n</urlset>\n`;
await writeFile("public/sitemap.xml", sitemap);
console.info(
  `Prepared ${paths.length} public pages${release ? ` with release ${release.tag}` : " with GitHub release links"}.`,
);
