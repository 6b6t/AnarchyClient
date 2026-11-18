import { createDiscordEmbed, serializeDiscordEmbed } from "./discord-embed";
import type { Release } from "./releases";
import { homePage, pageUrl, site, type PageMetadata } from "./site";

/** The accent color of the in-game menu, used for link previews. */
const accent = 0x00ba94;

export function pageEmbed(
  page: { title: string; description?: string },
  slug: string,
  website: { title: string; url: string; description: string } = site,
  release?: { version: string; minecraft?: string | undefined },
) {
  return createDiscordEmbed({
    title: page.title === homePage.title ? website.title : `${page.title} · ${website.title}`,
    description: [
      page.description ?? website.description,
      ...(slug === "releases" && release
        ? [
            `Release ${release.version}${release.minecraft ? ` · Minecraft ${release.minecraft}` : ""}`,
          ]
        : []),
    ].join("\n"),
    url: new URL(pageUrl(slug), website.url).href,
    label: slug === "releases" ? "View downloads" : slug === "index" ? "Open site" : "Read page",
    color: accent,
    links: [
      { label: "Latest release", url: site.latestRelease },
      { label: "Install guide", url: new URL("/getting-started/", website.url).href },
    ],
  });
}

export function pageHead(
  page: PageMetadata = homePage,
  slug = "index",
  release: Release | null = null,
) {
  const title = page.title === homePage.title ? site.title : `${page.title} · ${site.title}`;
  const url = new URL(pageUrl(slug), site.url).href;
  const image = new URL("/assets/images/demo.png", site.url).href;
  const component = pageEmbed(
    page,
    slug,
    site,
    release ? { version: release.tag, minecraft: release.minecraft } : undefined,
  );
  return {
    meta: [
      { title },
      { name: "description", content: page.description },
      { property: "og:type", content: "website" },
      { property: "og:site_name", content: site.title },
      { property: "og:title", content: title },
      { property: "og:description", content: page.description },
      { property: "og:url", content: url },
      { property: "og:image", content: image },
      { name: "twitter:card", content: "summary_large_image" },
      { name: "twitter:title", content: title },
      { name: "twitter:description", content: page.description },
      { name: "twitter:image", content: image },
    ],
    links: [{ rel: "canonical", href: url }],
    scripts: component
      ? [
          {
            id: "discord:component-embed",
            type: "application/json",
            children: serializeDiscordEmbed(component),
          },
        ]
      : [],
  };
}
