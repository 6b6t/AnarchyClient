# AnarchyClient website

The source for [client.6b6t.org](https://client.6b6t.org) lives here.
The site uses TanStack Start, Fumadocs, and Vite. GitHub Pages serves the static build.

## Run the site locally

Install Bun and Node.js 22.12 or later. From this directory, run:

```bash
bun install --frozen-lockfile
bun run dev
```

Open `http://localhost:3000`. The development server reloads when you edit a page or component.

## Edit a guide

Edit the MDX files in `content/docs/`. Each file needs `title` and `description` fields in its front matter.
Fumadocs supplies code highlighting, copy buttons, callouts, the page contents list, and search.
Keep existing filenames and heading anchors when you update published guides.

Guide URLs use the filename at the site root. For example, `getting-started.mdx` publishes at `/getting-started/`.
Add new guides to the sidebar in `src/lib/documents.ts`.
TanStack Start generates `src/routeTree.gen.ts` when you run or build the site. Do not edit that file.

Edit the homepage in `src/routes/index.tsx`, the downloads page in `src/routes/releases.tsx`, and the module list in `src/routes/modules.tsx`.
Shared site details live in `src/lib/site.ts`. Colors, layout, and animations live in `src/styles/app.css`.

## Module list

The homepage menu and `/modules/` read the module classes in `src/main/java/net/blockhost/anarchyclient/module/` at build time.
The build lists every module that a `*Modules.java` file registers, with the name, category, and description from its constructor.
A new module appears after the next site build. You do not need to edit the site.

## Check the static build

```bash
bun run check
bun run test
bun run build
bun run preview
```

The preview serves `dist/client/` at `http://localhost:3000`. It uses only exported files, including the 404 page.
Set `PORT` to use another preview port.

To run the desktop and mobile browser tests, install their browser once, then test the exported site:

```bash
bunx --no-install playwright install chromium --only-shell
bun run test:e2e
```

Run `bun run build` again before these tests if you change the source.
Use `bun run format` to format website changes.

The build checks published pages, internal links, heading anchors, canonical URLs, Discord previews, the module list, the sitemap, and the search index.
Search runs in the browser from `search-index.json`.

## Refresh downloads and publish

Each build fetches the latest public GitHub release and offers its mod JAR.
If the release has no single matching JAR, or GitHub is unavailable, the site links to the GitHub release instead.
Set `GITHUB_TOKEN` to authenticate the build request. The token stays in the build process and does not enter the exported site.

The **Publish website** workflow deploys after website or module changes on `main` and after each published release.
You can also run it manually in GitHub Actions. Pull requests that change the site run the same checks without a deploy.
Place files that need stable public URLs in `public/`.
