export const site = {
  title: "AnarchyClient",
  url: "https://client.6b6t.org",
  description:
    "A free, open-source Fabric utility client for 6b6t with a glass in-game menu and hundreds of modules.",
  github: "https://github.com/6b6t/AnarchyClient",
  latestRelease: "https://github.com/6b6t/AnarchyClient/releases/latest",
  releases: "https://github.com/6b6t/AnarchyClient/releases",
  issues: "https://github.com/6b6t/AnarchyClient/issues",
  discord: "https://6b6t.org/discord",
  server: "https://www.6b6t.org",
} as const;

export const homePage = { title: "Home", description: site.description };

export interface PageMetadata {
  title: string;
  description: string;
}

export function pageUrl(slug: string) {
  return slug === "index" ? "/" : `/${slug}/`;
}

export const modulesPage = {
  title: "Module list",
  description: "Every AnarchyClient module by category, with what it does.",
};
