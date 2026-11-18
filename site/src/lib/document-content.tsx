import type { FC } from "react";
import type { MDXProps } from "mdx/types";
import type { TOCItemType } from "fumadocs-core/toc";
import type { StructuredData } from "fumadocs-core/mdx-plugins/remark-structure";
import { createClientLoader } from "fumadocs-mdx/runtime/browser";
import {
  DocsBody,
  DocsDescription,
  DocsPage,
  DocsTitle,
  EditOnGitHub,
} from "fumadocs-ui/layouts/docs/page";
import mdxComponents from "fumadocs-ui/mdx";
import { site, type PageMetadata } from "./site";

interface DocumentModule {
  frontmatter: PageMetadata;
  default: FC<MDXProps>;
  toc: TOCItemType[];
  structuredData: StructuredData;
}

const modules = import.meta.glob<DocumentModule>("../../content/docs/*.mdx");

export const documentLoader = createClientLoader<
  DocumentModule,
  { slug: string; details?: boolean }
>(modules, {
  id: "anarchyclient-docs",
  component({ default: Content, toc, frontmatter }, props) {
    if (props.details)
      return (
        <DocsBody>
          <Content components={mdxComponents} />
        </DocsBody>
      );
    return (
      <DocsPage toc={toc}>
        <DocsTitle id={frontmatter.title.toLowerCase().replaceAll(/\s+/g, "-")}>
          {frontmatter.title}
        </DocsTitle>
        <DocsDescription>{frontmatter.description}</DocsDescription>
        <DocsBody>
          <Content components={mdxComponents} />
        </DocsBody>
        <EditOnGitHub href={`${site.github}/edit/main/site/content/docs/${props.slug}.mdx`}>
          Improve this page
        </EditOnGitHub>
      </DocsPage>
    );
  },
});
