import { createFileRoute, Link } from "@tanstack/react-router";
import release from "virtual:anarchyclient-release";
import { Suspense } from "react";
import { DownloadCard } from "../components/download-card";
import { InstallSteps } from "../components/install-steps";
import { documentLoader } from "../lib/document-content";
import { requireDocument } from "../lib/documents";
import { pageHead } from "../lib/metadata";
import { site } from "../lib/site";

const page = requireDocument("releases");

export const Route = createFileRoute("/releases")({
  loader: async () => {
    await (await import("../lib/document-content")).documentLoader.preload(page.path);
  },
  head: () => pageHead(page, "releases", release),
  component: Downloads,
});

function Downloads() {
  const Content = documentLoader.getComponent(page.path);
  return (
    <main id="main-content" className="downloads">
      <div className="page-heading shell-width">
        <h1 id="download-anarchyclient">Download AnarchyClient</h1>
        <p>
          One JAR for Fabric. Put it in your mods folder next to Fabric API and press{" "}
          <kbd>Right Shift</kbd> in game.
        </p>
      </div>
      <div className="shell-width download-layout">
        <DownloadCard release={release} />
        <section className="download-steps" aria-labelledby="after-download">
          <h2 id="after-download">After the download</h2>
          <InstallSteps minecraft={release?.minecraft} />
          <p>
            <Link to="/$slug/" params={{ slug: "getting-started" }}>
              Read the install guide
            </Link>{" "}
            or <a href={site.discord}>ask on the 6b6t Discord</a>.
          </p>
        </section>
      </div>
      <section
        className="download-details shell-width"
        aria-label="Verification and older releases"
      >
        <Suspense>
          <Content slug="releases" details />
        </Suspense>
      </section>
    </main>
  );
}
