export interface ClientModule {
  id: string;
  name: string;
  category: string;
  description: string;
}

export interface ModuleCategory {
  id: string;
  name: string;
  modules: ClientModule[];
}

/** Reads the categories and their display names from `ModuleCategory.java`, in menu order. */
export function parseCategories(source: string) {
  return [...source.matchAll(/^\s*([A-Z_]+)\("((?:[^"\\]|\\.)*)"\)[,;]/gmu)].flatMap(
    ([, id, name]) =>
      id === undefined || name === undefined ? [] : [{ id, name: unescape(name) }],
  );
}

/** Collects the module classes that the `*Modules.java` registration files construct. */
export function parseRegistrations(sources: readonly string[]) {
  return new Set(
    sources.flatMap((source) => [...source.matchAll(/new (\w+)\(/gu)].flatMap((m) => m[1] ?? [])),
  );
}

/** Reads one module class. Returns `undefined` for helpers and abstract bases. */
export function parseModule(source: string): (ClientModule & { className: string }) | undefined {
  const declaration = /\bfinal class (\w+) extends (Module|HudElementModule)\b/u.exec(source);
  if (!declaration) return undefined;
  const [, className, base] = declaration;
  if (!className) return undefined;
  const call = source.indexOf("super(", declaration.index);
  if (call < 0) return undefined;
  const args = splitArguments(source, call + "super(".length);
  if (base === "HudElementModule") {
    const [id, name, description] = args.map((arg) => stringValue(arg));
    if (id === undefined || name === undefined || description === undefined) return undefined;
    return { className, id, name, category: "HUD", description };
  }
  const [id, name, description] = [args[0], args[1], args[3]].map((arg) => stringValue(arg));
  const category = /^ModuleCategory\.([A-Z_]+)$/u.exec(args[2] ?? "")?.[1];
  if (id === undefined || name === undefined || description === undefined || !category)
    return undefined;
  return { className, id, name, category, description };
}

/** Groups registered modules by category, keeping the in-game category order. */
export function buildCatalog(
  categorySource: string,
  registrationSources: readonly string[],
  moduleSources: readonly string[],
): ModuleCategory[] {
  const registered = parseRegistrations(registrationSources);
  const modules = moduleSources
    .flatMap((source) => parseModule(source) ?? [])
    .filter((module) => registered.has(module.className))
    .map(({ id, name, category, description }): ClientModule => ({
      id,
      name,
      category,
      description,
    }));
  return parseCategories(categorySource)
    .map(({ id, name }) => ({
      id: id.toLowerCase(),
      name,
      modules: modules
        .filter((module) => module.category === id)
        .map((module) => ({ ...module, category: id.toLowerCase() }))
        .toSorted((a, b) => a.name.localeCompare(b.name, "en")),
    }))
    .filter((category) => category.modules.length > 0);
}

/** The anchor of a module on the module list page. */
export function moduleAnchor(module: Pick<ClientModule, "id">) {
  return `module-${module.id.replaceAll("_", "-")}`;
}

function splitArguments(source: string, start: number) {
  const args: string[] = [];
  let depth = 0;
  let current = "";
  for (let index = start; index < source.length; index++) {
    const char = source.charAt(index);
    if (char === '"') {
      const end = stringEnd(source, index);
      current += source.slice(index, end + 1);
      index = end;
    } else if (char === "(") {
      depth++;
      current += char;
    } else if (char === ")" && depth > 0) {
      depth--;
      current += char;
    } else if (char === ")" || (char === "," && depth === 0)) {
      args.push(current.trim());
      current = "";
      if (char === ")") break;
    } else {
      current += char;
    }
  }
  return args;
}

function stringEnd(source: string, start: number) {
  for (let index = start + 1; index < source.length; index++) {
    if (source[index] === "\\") index++;
    else if (source[index] === '"') return index;
  }
  return source.length;
}

/** Evaluates a Java string literal or a `+` concatenation of string literals. */
function stringValue(expression: string | undefined) {
  if (!expression) return;
  const parts = expression.split(/"\s*\+\s*"/u);
  const joined = parts.join("");
  if (!/^"(?:[^"\\]|\\.)*"$/su.test(joined)) return;
  return unescape(joined.slice(1, -1)).replaceAll(/\s+/gu, " ").trim();
}

function unescape(value: string) {
  return value.replaceAll(/\\(u[0-9a-fA-F]{4}|.)/gu, (_, escaped: string) => {
    if (escaped.startsWith("u") && escaped.length === 5)
      return String.fromCodePoint(Number.parseInt(escaped.slice(1), 16));
    return { n: "\n", t: "\t" }[escaped] ?? escaped;
  });
}
