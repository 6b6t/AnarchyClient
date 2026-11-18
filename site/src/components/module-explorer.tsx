import { Link } from "@tanstack/react-router";
import { ArrowRight, Search, SlidersVertical } from "lucide-react";
import { useDeferredValue, useState } from "react";
import { moduleAnchor, type ClientModule, type ModuleCategory } from "../lib/modules";
import { CategoryIcon } from "./category-icon";

export function searchModules(categories: readonly ModuleCategory[], query: string) {
  const terms = query.toLowerCase().split(/\s+/).filter(Boolean);
  return categories
    .flatMap((category) => category.modules)
    .filter((module) => {
      const text = `${module.name} ${module.description}`.toLowerCase();
      return terms.every((term) => text.includes(term));
    })
    .toSorted(
      (a, b) =>
        Number(!a.name.toLowerCase().includes(terms[0] ?? "")) -
          Number(!b.name.toLowerCase().includes(terms[0] ?? "")) ||
        a.name.localeCompare(b.name, "en"),
    );
}

/** A browsable copy of the in-game menu: category dock, module grid, and inspector. */
export function ModuleExplorer({ categories }: { categories: readonly ModuleCategory[] }) {
  const [categoryId, setCategoryId] = useState(categories[0]!.id);
  const [query, setQuery] = useState("");
  const deferredQuery = useDeferredValue(query.trim());
  const category = categories.find((item) => item.id === categoryId) ?? categories[0]!;
  const modules = deferredQuery ? searchModules(categories, deferredQuery) : category.modules;
  const [selected, setSelected] = useState<ClientModule>(category.modules[0]!);
  const selectedCategory = categories.find((item) => item.id === selected.category);

  return (
    <div className="explorer">
      <div className="explorer-backdrop" aria-hidden="true" />
      <nav className="dock glass" aria-label="Module categories">
        {categories.map((item) => (
          <button
            key={item.id}
            type="button"
            className="dock-button"
            aria-pressed={!deferredQuery && item.id === category.id}
            aria-label={`${item.name}, ${item.modules.length} modules`}
            title={item.name}
            onClick={() => {
              setQuery("");
              setCategoryId(item.id);
              setSelected(item.modules[0]!);
            }}
          >
            <CategoryIcon category={item.id} size={20} />
          </button>
        ))}
      </nav>
      <section className="module-panel glass" aria-labelledby="explorer-heading">
        <div className="panel-header">
          <h3 id="explorer-heading">{deferredQuery ? "Search" : category.name}</h3>
          <span className="accent mono">{modules.length}</span>
        </div>
        <label className="module-search">
          <Search size={16} aria-hidden="true" />
          <span className="sr-only">Search all modules</span>
          <input
            type="search"
            placeholder="Search all modules…"
            value={query}
            onChange={(event) => setQuery(event.target.value)}
          />
        </label>
        <ul className="module-grid" aria-label="Modules">
          {modules.map((module) => (
            <li key={module.id}>
              <button
                type="button"
                className="module-card"
                aria-pressed={module.id === selected.id}
                onClick={() => setSelected(module)}
              >
                <CategoryIcon category={module.category} size={16} />
                <span>{module.name}</span>
                <SlidersVertical className="card-sliders" size={15} aria-hidden="true" />
              </button>
            </li>
          ))}
          {modules.length === 0 && <li className="module-empty">No module matches “{query}”.</li>}
        </ul>
      </section>
      <aside className="inspector glass" aria-live="polite" aria-label="Selected module">
        <h3>{selected.name}</h3>
        <p className="inspector-category">
          <CategoryIcon category={selected.category} size={16} />
          {selectedCategory?.name}
        </p>
        <p className="inspector-description">{selected.description}</p>
        <Link
          className="inspector-link"
          to="/modules/"
          hash={moduleAnchor(selected)}
          hashScrollIntoView
        >
          Open in the module list
          <ArrowRight size={16} aria-hidden="true" />
        </Link>
      </aside>
    </div>
  );
}
