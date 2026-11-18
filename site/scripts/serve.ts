import { resolve, sep } from "node:path";

// Serve the exported files without an application server, matching GitHub Pages.
const root = resolve("dist/client");
const port = Number(process.env.PORT ?? 3000);
const server = Bun.serve({
  hostname: "0.0.0.0",
  port,
  async fetch(request) {
    const url = new URL(request.url);
    let pathname: string;
    try {
      pathname = decodeURIComponent(url.pathname);
    } catch {
      return new Response("Invalid URL", { status: 400 });
    }
    const path = resolve(root, `.${pathname}`);
    if (path !== root && !path.startsWith(`${root}${sep}`))
      return new Response("Not found", { status: 404 });
    const file = Bun.file(path);
    if (await file.exists()) return new Response(file);
    const index = Bun.file(resolve(path, "index.html"));
    if (await index.exists()) {
      if (!url.pathname.endsWith("/")) {
        url.pathname += "/";
        return Response.redirect(url.href, 301);
      }
      return new Response(index);
    }
    return new Response(Bun.file(resolve(root, "404.html")), {
      status: 404,
      headers: { "Content-Type": "text/html; charset=utf-8" },
    });
  },
});
console.info(`Static site preview: ${server.url}`);
