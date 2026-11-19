import type { Root } from "fumadocs-core/page-tree";
import { pageUrl, type PageMetadata } from "./site";

const metadata = import.meta.glob<{ frontmatter: PageMetadata }>("../../content/docs/*.mdx", {
  eager: true,
  query: "?only=frontmatter",
});

export const documents = Object.entries(metadata).map(([path, { frontmatter }]) =>
  Object.assign({}, frontmatter, { path, slug: path.slice(path.lastIndexOf("/") + 1, -4) }),
);

export function getDocument(slug: string) {
  return documents.find((page) => page.slug === slug);
}

/** Returns a documentation page that the site cannot work without. */
export function requireDocument(slug: string) {
  const page = getDocument(slug);
  if (!page) throw new Error(`Missing documentation: ${slug}`);
  return page;
}

/** Sidebar labels that differ from the page title. */
const labels: Record<string, string> = { "getting-started": "Install", releases: "Downloads" };

export const pageTree: Root = {
  name: "Documentation",
  children: [
    { type: "separator", name: "Use" },
    item("getting-started"),
    item("releases"),
    { type: "page", name: "Modules", url: "/modules/" },
    { type: "separator", name: "Develop" },
    item("build-from-source"),
    item("contributing"),
  ],
};

function item(slug: string) {
  const page = requireDocument(slug);
  return { type: "page" as const, name: labels[slug] ?? page.title, url: pageUrl(slug) };
}
