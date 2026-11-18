import { readdirSync, readFileSync } from "node:fs";
import { buildCatalog } from "../src/lib/modules.ts";

export const moduleRoot = new URL(
  "../../src/main/java/net/blockhost/anarchyclient/module/",
  import.meta.url,
);

/** Reads the module catalog from the mod's Java sources. */
export function readModuleCatalog() {
  const read = (name: string, dir = moduleRoot) => readFileSync(new URL(name, dir), "utf8");
  const impl = new URL("impl/", moduleRoot);
  const registrations = readdirSync(moduleRoot)
    .filter((file) => /^[A-Z]\w*Modules\.java$/.test(file))
    .map((file) => read(file));
  const modules = readdirSync(impl)
    .filter((file) => file.endsWith(".java"))
    .map((file) => read(file, impl));
  return buildCatalog(read("ModuleCategory.java"), registrations, modules);
}
