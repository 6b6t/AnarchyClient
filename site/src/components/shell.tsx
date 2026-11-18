import { Link } from "@tanstack/react-router";
import { useSearchContext } from "fumadocs-ui/contexts/search";
import { useTheme } from "fumadocs-ui/provider/base";
import { ArrowDownToLine, Menu, Moon, Search, Sun, X } from "lucide-react";
import { useState } from "react";
import { site } from "../lib/site";

/** The wordmark from the in-game menu: "ANARCHY" in white, "client" in the accent. */
export function Brand() {
  return (
    <span className="brand">
      <span className="brand-anarchy">Anarchy</span>
      <span className="brand-client">client</span>
    </span>
  );
}

function NavLinks() {
  return (
    <>
      <Link to="/$slug/" params={{ slug: "getting-started" }}>
        Install
      </Link>
      <Link to="/modules/">Modules</Link>
      <Link to="/releases/">Downloads</Link>
      <Link to="/$slug/" params={{ slug: "build-from-source" }}>
        Developers
      </Link>
    </>
  );
}

export function SiteHeader() {
  const { setOpenSearch } = useSearchContext();
  const { resolvedTheme, setTheme } = useTheme();
  const [menuOpen, setMenuOpen] = useState(false);
  return (
    <header className="site-header">
      <a href="#main-content" className="skip-link">
        Skip to content
      </a>
      <div className="header-inner">
        <Link to="/" aria-label="AnarchyClient home" onClick={() => setMenuOpen(false)}>
          <Brand />
        </Link>
        <nav className="desktop-nav" aria-label="Main navigation">
          <NavLinks />
        </nav>
        <div className="header-tools">
          <button
            className="search-trigger"
            onClick={() => setOpenSearch(true)}
            aria-label="Search the site"
          >
            <Search size={17} aria-hidden="true" />
            <span>Search</span>
            <kbd>Ctrl K</kbd>
          </button>
          <button
            className="icon-button theme-toggle"
            aria-label="Toggle color theme"
            onClick={() => setTheme(resolvedTheme === "dark" ? "light" : "dark")}
          >
            <Sun className="theme-sun" size={19} aria-hidden="true" />
            <Moon className="theme-moon" size={19} aria-hidden="true" />
          </button>
          <button
            className="icon-button mobile-menu-toggle"
            aria-label={menuOpen ? "Close navigation" : "Open navigation"}
            aria-expanded={menuOpen}
            aria-controls="mobile-navigation"
            onClick={() => setMenuOpen(!menuOpen)}
          >
            {menuOpen ? <X size={21} /> : <Menu size={21} />}
          </button>
          <Link
            className="button primary header-download"
            to="/releases/"
            onClick={() => setMenuOpen(false)}
          >
            <ArrowDownToLine size={17} aria-hidden="true" />
            <span className="header-download-label">Download</span>
          </Link>
        </div>
      </div>
      {menuOpen && (
        <nav
          id="mobile-navigation"
          className="mobile-nav"
          aria-label="Mobile navigation"
          onClick={() => setMenuOpen(false)}
        >
          <NavLinks />
          <a href={site.discord}>Discord</a>
          <a href={site.github}>GitHub</a>
        </nav>
      )}
    </header>
  );
}

export function SiteFooter() {
  return (
    <footer className="site-footer">
      <div className="footer-inner shell-width">
        <div className="footer-brand">
          <Brand />
          <p>A free, open-source Fabric utility client for 6b6t.</p>
        </div>
        <nav aria-label="Players">
          <h2>Use</h2>
          <Link to="/releases/">Downloads</Link>
          <Link to="/$slug/" params={{ slug: "getting-started" }}>
            Install guide
          </Link>
          <Link to="/modules/">Module list</Link>
        </nav>
        <nav aria-label="Developers">
          <h2>Develop</h2>
          <Link to="/$slug/" params={{ slug: "build-from-source" }}>
            Build from source
          </Link>
          <Link to="/$slug/" params={{ slug: "contributing" }}>
            Contribute
          </Link>
          <a href={site.issues}>Report a bug</a>
        </nav>
        <nav aria-label="Community links">
          <h2>Community</h2>
          <a href={site.server}>6b6t</a>
          <a href={site.discord}>Discord</a>
          <a href={site.github}>GitHub</a>
        </nav>
        <p className="footer-note">
          AnarchyClient is a side project by the 6b6t team, released under the MIT license. It is
          not affiliated with Mojang or Microsoft.
        </p>
      </div>
    </footer>
  );
}
