import { site } from "./site";
import { parseUrl } from "./url";

export interface ReleaseAsset {
  name: string;
  url: string;
  size: number;
}

export interface Release {
  tag: string;
  url: string;
  publishedAt: string;
  /** The single mod JAR in the release, if the asset list has exactly one. */
  jar?: ReleaseAsset;
  /** The Minecraft version from the JAR name, such as `26.3`. */
  minecraft?: string;
}

const repository = "/6b6t/AnarchyClient/releases/";
const jarPattern = /^anarchyclient-mc-([\d.]+)-(.+)\.jar$/u;

function githubUrl(value: unknown, prefix: string) {
  if (typeof value !== "string") return;
  const url = parseUrl(value);
  return url?.origin === "https://github.com" &&
    url.pathname.startsWith(prefix) &&
    !url.username &&
    !url.password
    ? url.href
    : undefined;
}

/** Reads the GitHub API response for the latest release. Returns `null` for unusable data. */
export function parseRelease(value: unknown): Release | null {
  if (!value || typeof value !== "object") return null;
  if (
    !("tag_name" in value) ||
    typeof value.tag_name !== "string" ||
    !("html_url" in value) ||
    !("assets" in value) ||
    !Array.isArray(value.assets) ||
    ("draft" in value && value.draft) ||
    ("prerelease" in value && value.prerelease)
  )
    return null;
  const url = githubUrl(value.html_url, `${repository}tag/`);
  if (!url) return null;
  const tag = value.tag_name;
  // A release snapshot must never offer a file from a different release tag.
  const prefix = `${repository}download/${encodeURIComponent(tag)}/`;
  const jars = value.assets.flatMap((asset: unknown): ReleaseAsset[] => {
    if (!asset || typeof asset !== "object") return [];
    if (
      !("name" in asset) ||
      typeof asset.name !== "string" ||
      !jarPattern.test(asset.name) ||
      /-(?:sources|javadoc|dev)\.jar$/u.test(asset.name) ||
      !("size" in asset) ||
      typeof asset.size !== "number" ||
      asset.size < 0 ||
      ("state" in asset && asset.state !== "uploaded")
    )
      return [];
    const download = githubUrl(
      "browser_download_url" in asset ? asset.browser_download_url : undefined,
      prefix,
    );
    return download ? [{ name: asset.name, url: download, size: asset.size }] : [];
  });
  const jar = jars.length === 1 ? jars[0] : undefined;
  const minecraft = jar ? jarPattern.exec(jar.name)?.[1] : undefined;
  return {
    tag,
    url,
    publishedAt:
      "published_at" in value && typeof value.published_at === "string" ? value.published_at : "",
    ...(jar ? { jar } : {}),
    ...(minecraft ? { minecraft } : {}),
  };
}

export function releaseLink(release: Release | null) {
  return release?.url ?? site.latestRelease;
}

export function formatSize(bytes: number) {
  return `${(bytes / 1024 / 1024).toFixed(bytes >= 10 * 1024 * 1024 ? 0 : 1)} MB`;
}
