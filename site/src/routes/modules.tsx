import { createFileRoute, Link } from "@tanstack/react-router";
import modules from "virtual:anarchyclient-modules";
import { CategoryIcon } from "../components/category-icon";
import { pageHead } from "../lib/metadata";
import { moduleAnchor } from "../lib/modules";
import { modulesPage, site } from "../lib/site";

export const Route = createFileRoute("/modules")({
  head: () => pageHead(modulesPage, "modules"),
  component: ModuleList,
});

const total = modules.reduce((sum, category) => sum + category.modules.length, 0);

function ModuleList() {
  return (
    <main id="main-content" className="module-list">
      <div className="page-heading shell-width">
        <h1 id="module-list">{modulesPage.title}</h1>
        <p>
          AnarchyClient has {total} modules in {modules.length} categories. Open the menu with{" "}
          <kbd>Right Shift</kbd> and type a name in its search field to find one in game.{" "}
          <Link to="/$slug/" params={{ slug: "getting-started" }} hash="open-the-menu">
            How the menu works
          </Link>
        </p>
        <nav className="category-jump" aria-label="Categories">
          {modules.map((category) => (
            <a key={category.id} href={`#${category.id}`}>
              <CategoryIcon category={category.id} size={16} />
              {category.name}
              <span className="mono">{category.modules.length}</span>
            </a>
          ))}
        </nav>
      </div>
      <div className="shell-width category-sections">
        {modules.map((category) => (
          <section key={category.id} className="category-section" aria-labelledby={category.id}>
            <h2 id={category.id}>
              <CategoryIcon category={category.id} size={22} />
              {category.name}
              <span className="category-count mono">{category.modules.length}</span>
            </h2>
            <ul className="module-entries">
              {category.modules.map((module) => (
                <li key={module.id} id={moduleAnchor(module)} className="module-entry">
                  <h3>
                    <a href={`#${moduleAnchor(module)}`}>{module.name}</a>
                  </h3>
                  <p>{module.description}</p>
                </li>
              ))}
            </ul>
          </section>
        ))}
        <p className="module-source muted">
          This list comes from the module classes in the{" "}
          <a
            href={`${site.github}/tree/main/src/main/java/net/blockhost/anarchyclient/module/impl`}
          >
            source code
          </a>{" "}
          and updates with every site build. It can list modules that are newer than the latest
          release.
        </p>
      </div>
    </main>
  );
}
