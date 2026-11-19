import { createRootRoute, HeadContent, Outlet, Scripts } from "@tanstack/react-router";
import { RootProvider } from "fumadocs-ui/provider/tanstack";
import { lazy } from "react";
import { SiteFooter, SiteHeader } from "../components/shell";
import appCss from "../styles/app.css?url";

const SiteSearch = lazy(() => import("../components/search"));

export const Route = createRootRoute({
  head: () => ({
    meta: [
      // oxlint-disable-next-line unicorn/text-encoding-identifier-case -- HTML requires the UTF-8 label.
      { charSet: "utf-8" },
      { name: "viewport", content: "width=device-width, initial-scale=1" },
      { name: "theme-color", content: "#0b0e15" },
    ],
    links: [
      { rel: "stylesheet", href: appCss },
      { rel: "icon", href: "/assets/images/favicon.svg", type: "image/svg+xml" },
    ],
  }),
  component: Root,
});

function Root() {
  return (
    <html lang="en" className="dark" suppressHydrationWarning>
      <head>
        <HeadContent />
      </head>
      <body>
        <RootProvider
          theme={{ defaultTheme: "dark", storageKey: "anarchyclient-theme" }}
          search={{ SearchDialog: SiteSearch, preload: false }}
        >
          <SiteHeader />
          <Outlet />
          <SiteFooter />
        </RootProvider>
        <Scripts />
      </body>
    </html>
  );
}
