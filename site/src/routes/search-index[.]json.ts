import { createFileRoute } from "@tanstack/react-router";
import { createFromSource } from "fumadocs-core/search/server";
import { loader } from "fumadocs-core/source";
import modules from "virtual:anarchyclient-modules";
import { docs } from "../../.source/server";
import { moduleAnchor } from "../lib/modules";
import { homePage, modulesPage, site } from "../lib/site";

const content = docs.toFumadocsSource();
const search = createFromSource(
  loader(
    {
      files: [
        ...content.files,
        {
          type: "page",
          path: "index.mdx",
          data: {
            title: site.title,
            description: homePage.description,
            structuredData: {
              headings: [],
              contents: [{ heading: undefined, content: homePage.description }],
            },
          },
        },
        {
          // Each module is a section of the module list, so results open its anchor.
          type: "page",
          path: "modules.mdx",
          data: {
            title: modulesPage.title,
            description: modulesPage.description,
            structuredData: {
              headings: modules.flatMap((category) =>
                category.modules.map((module) => ({
                  id: moduleAnchor(module),
                  content: module.name,
                })),
              ),
              contents: modules.flatMap((category) =>
                category.modules.map((module) => ({
                  heading: moduleAnchor(module),
                  content: module.description,
                })),
              ),
            },
          },
        },
      ],
    },
    { baseUrl: "/" },
  ),
);

export const Route = createFileRoute("/search-index.json")({
  server: { handlers: { GET: () => search.staticGET() } },
});
