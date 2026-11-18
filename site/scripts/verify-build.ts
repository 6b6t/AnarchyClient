import { readFile, stat } from "node:fs/promises";
import { join, resolve } from "node:path";
import { moduleAnchor } from "../src/lib/modules";
import { site } from "../src/lib/site";
import { readModuleCatalog } from "./module-catalog";
import { publicPages } from "./pages";

interface HtmlPage {
  path: string;
  html: string;
  ids: Set<string>;
  links: string[];
  h1s: number;
  canonical?: string;
  description?: string | undefined;
  discord?: string;
}

async function inspect(path: string, html: string): Promise<HtmlPage> {
  const page: HtmlPage = { path, html, ids: new Set(), links: [], h1s: 0 };
  const parser = new HTMLRewriter()
    .on("[id]", {
      element(element) {
        const id = element.getAttribute("id");
        if (id !== null) page.ids.add(id);
      },
    })
    .on("a[href]", {
      element(element) {
        const href = element.getAttribute("href");
        if (href !== null) page.links.push(href);
      },
    })
    .on("script[src], img[src]", {
      element(element) {
        const src = element.getAttribute("src");
        if (src !== null) page.links.push(src);
      },
    })
    .on("link[href]", {
      element(element) {
        const href = element.getAttribute("href");
        if (href === null) return;
        page.links.push(href);
        if (element.getAttribute("rel") === "canonical") page.canonical = href;
      },
    })
    .on("h1", {
      element() {
        page.h1s++;
      },
    })
    .on('meta[name="description"]', {
      element(element) {
        page.description = element.getAttribute("content") ?? undefined;
      },
    })
    .on('script[id="discord:component-embed"]', {
      text(chunk) {
        page.discord = (page.discord ?? "") + chunk.text;
      },
    });
  await parser.transform(new Response(html)).text();
  return page;
}

const root = resolve("dist/client");
const paths = await publicPages();
const pages = new Map<string, HtmlPage>();
for (const path of paths) {
  const html = await readFile(join(root, path, "index.html"), "utf8");
  const page = await inspect(path, html);
  if (page.h1s !== 1) throw new Error(`${path}: expected one H1, got ${page.h1s}`);
  if (page.canonical !== new URL(path, site.url).href)
    throw new Error(`${path}: missing or incorrect canonical URL`);
  if (!page.description) throw new Error(`${path}: missing page description`);
  if (!page.discord) throw new Error(`${path}: missing Discord component preview`);
  JSON.parse(page.discord);
  if (html.includes("{{") || html.includes("{:toc}") || html.includes("/_serverFn/"))
    throw new Error(`${path}: unresolved template or application-server dependency`);
  pages.set(path, page);
}
for (const page of pages.values()) {
  for (const href of page.links) {
    const url = new URL(href, new URL(page.path, site.url));
    if (url.origin !== site.url) continue;
    const targetPath = url.pathname.endsWith("/") ? url.pathname : `${url.pathname}/`;
    const targetPage = pages.get(targetPath);
    if (targetPage) {
      if (url.hash && !targetPage.ids.has(decodeURIComponent(url.hash.slice(1))))
        throw new Error(`${page.path}: missing anchor ${href}`);
    } else if (!(await stat(join(root, url.pathname))).isFile()) {
      throw new Error(`${page.path}: missing static asset ${href}`);
    }
  }
}
// Keep anchors that other sites and the mod menu may link to.
const anchors: Record<string, string[]> = {
  "/": ["anarchyclient", "modules", "install", "questions"],
  "/getting-started/": ["what-you-need", "install-the-mod", "open-the-menu", "your-settings"],
  "/releases/": ["download-anarchyclient", "verify-your-download"],
};
for (const [path, ids] of Object.entries(anchors))
  for (const id of ids)
    if (!pages.get(path)?.ids.has(id)) throw new Error(`Missing published anchor: ${path}#${id}`);
const catalog = readModuleCatalog();
const moduleList = pages.get("/modules/");
if (!moduleList) throw new Error("Missing module list page");
for (const module of catalog.flatMap((category) => category.modules))
  if (!moduleList.ids.has(moduleAnchor(module)))
    throw new Error(`Module list omits ${module.name} (#${moduleAnchor(module)})`);
const sitemap = await readFile(join(root, "sitemap.xml"), "utf8");
for (const path of paths)
  if (!sitemap.includes(`<loc>${new URL(path, site.url).href}</loc>`))
    throw new Error(`Sitemap omits ${path}`);
const search: unknown = JSON.parse(await readFile(join(root, "search-index.json"), "utf8"));
if (!search || typeof search !== "object" || !("type" in search))
  throw new Error("Missing static search database");
const notFound = await readFile(join(root, "404.html"), "utf8");
if (!notFound.includes("Page not found") || !notFound.includes("noindex"))
  throw new Error("Missing static 404 page");
for (const asset of ["robots.txt", ".nojekyll", "assets/images/favicon.svg"])
  await stat(join(root, asset));
console.info(
  `Verified ${pages.size} prerendered pages, ${catalog.length} module categories, internal links and anchors, metadata, search, sitemap, and static assets.`,
);
