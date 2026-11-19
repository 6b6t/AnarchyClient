import { createFileRoute, notFound } from "@tanstack/react-router";
import { DocsLayout } from "fumadocs-ui/layouts/docs";
import { Suspense } from "react";
import { DocumentContent } from "../lib/document-content";
import { getDocument, pageTree } from "../lib/documents";
import { pageHead } from "../lib/metadata";
import { site } from "../lib/site";

export const Route = createFileRoute("/$slug")({
  loader: async ({ params }) => {
    const page = getDocument(params.slug);
    if (!page) throw notFound();
    await (await import("../lib/document-content")).documentLoader.preload(page.path);
    return { path: page.path, slug: page.slug, title: page.title, description: page.description };
  },
  head: ({ loaderData }) => (loaderData ? pageHead(loaderData, loaderData.slug) : {}),
  component: DocumentPage,
});

function DocumentPage() {
  const page = Route.useLoaderData();
  return (
    <div id="main-content" className="docs-shell" tabIndex={-1}>
      <DocsLayout
        tree={pageTree}
        nav={{ title: "Documentation", url: "/getting-started/" }}
        themeSwitch={{ enabled: false }}
        searchToggle={{ enabled: false }}
        links={[]}
        sidebar={{
          footer: (
            <a className="sidebar-community" href={site.discord}>
              Stuck on a step?
              <span>Ask on the 6b6t Discord</span>
            </a>
          ),
        }}
      >
        <Suspense fallback={<p className="content-width">Loading guide…</p>}>
          <DocumentContent path={page.path} slug={page.slug} />
        </Suspense>
      </DocsLayout>
    </div>
  );
}
