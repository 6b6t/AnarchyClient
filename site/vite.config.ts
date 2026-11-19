import { readdirSync, readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import tailwindcss from "@tailwindcss/vite";
import { tanstackStart } from "@tanstack/react-start/plugin/vite";
import react from "@vitejs/plugin-react";
import { fumadocsMdx } from "fumadocs-mdx/vite";
import { defineConfig, type Plugin } from "vite";
import { moduleRoot, readModuleCatalog } from "./scripts/module-catalog.ts";

const docs = readdirSync(new URL("./content/docs", import.meta.url))
  .filter((file) => file.endsWith(".mdx"))
  .map((file) => `/${file.slice(0, -4)}/`);

function virtualModule(
  name: string,
  load: (plugin: { addWatchFile(path: string): void }) => string,
): Plugin {
  const id = `virtual:anarchyclient-${name}`;
  return {
    name: `anarchyclient-${name}`,
    resolveId(source) {
      if (source === id) return `\0${id}`;
      return null;
    },
    load(source) {
      if (source === `\0${id}`) return load(this);
      return null;
    },
  };
}

export default defineConfig({
  resolve: { alias: { "@": fileURLToPath(new URL("./src", import.meta.url)) } },
  plugins: [
    // The latest GitHub release, fetched by scripts/prepare.ts.
    virtualModule("release", () => {
      try {
        return `export default ${readFileSync(new URL("./.generated/release.json", import.meta.url), "utf8")};`;
      } catch (error) {
        if (!(error instanceof Error && "code" in error && error.code === "ENOENT")) throw error;
        return "export default null;";
      }
    }),
    // The module catalog, read from the mod's Java sources.
    virtualModule("modules", (plugin) => {
      plugin.addWatchFile(fileURLToPath(moduleRoot));
      return `export default ${JSON.stringify(readModuleCatalog())};`;
    }),
    fumadocsMdx(),
    tailwindcss(),
    tanstackStart({
      router: { quoteStyle: "double", semicolons: true },
      prerender: {
        enabled: true,
        autoSubfolderIndex: true,
        crawlLinks: false,
        autoStaticPathsDiscovery: false,
        failOnError: true,
      },
      pages: [
        { path: "/" },
        { path: "/modules/" },
        ...docs.map((path) => ({ path })),
        { path: "/search-index.json", prerender: { outputPath: "/search-index.json" } },
        { path: "/404", prerender: { outputPath: "/404.html" } },
      ],
    }),
    react(),
  ],
});
