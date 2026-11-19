import { Link } from "@tanstack/react-router";
import { useSearchContext } from "fumadocs-ui/contexts/search";

export function NotFoundPage() {
  const { setOpenSearch } = useSearchContext();
  return (
    <main id="main-content" className="not-found shell-width">
      <p className="not-found-code mono">
        <span className="accent">404</span> · chunk not loaded
      </p>
      <h1>Page not found</h1>
      <p>This page may have moved. Search the site or return to the homepage.</p>
      <div className="actions">
        <button
          className="button primary"
          onClick={() => {
            setOpenSearch(true);
          }}
        >
          Search the site
        </button>
        <Link className="button ghost" to="/">
          Go to homepage
        </Link>
      </div>
    </main>
  );
}
