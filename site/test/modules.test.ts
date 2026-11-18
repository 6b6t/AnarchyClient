import { expect, test } from "bun:test";
import { searchModules } from "../src/components/module-explorer";
import { buildCatalog, moduleAnchor, parseModule } from "../src/lib/modules";
import { readModuleCatalog } from "../scripts/module-catalog";

const categories = `public enum ModuleCategory {
    COMBAT("Combat"),
    HUD("HUD"),
    FUN("Fun");
}`;

test("module classes yield their id, name, category, and joined description", () => {
  expect(
    parseModule(`public final class AutoTotemModule extends Module {
      public AutoTotemModule() {
        super("auto_totem", "Auto Totem", ModuleCategory.COMBAT,
            "Keeps a totem ready, " + "even with \\"quotes\\".", List.of(helper(1, 2)));
      }
    }`),
  ).toEqual({
    className: "AutoTotemModule",
    id: "auto_totem",
    name: "Auto Totem",
    category: "COMBAT",
    description: 'Keeps a totem ready, even with "quotes".',
  });
  expect(
    parseModule(`public final class CompassHudModule extends HudElementModule {
      public CompassHudModule() {
        super("compass_hud", "Compass HUD",
                "Shows the facing direction.", "Top Left");
      }
    }`),
  ).toMatchObject({
    id: "compass_hud",
    category: "HUD",
    description: "Shows the facing direction.",
  });
  expect(parseModule("abstract class HudElementModule extends Module {}")).toBeUndefined();
  expect(parseModule("final class BlockScan { }")).toBeUndefined();
});

test("the catalog lists only registered modules, sorted, in menu category order", () => {
  const module = (name: string, category: string) =>
    `final class ${name.replaceAll(" ", "")}Module extends Module { ${name}Module() { super("${name.toLowerCase().replaceAll(" ", "_")}", "${name}", ModuleCategory.${category}, "Does ${name}."); } }`;
  const catalog = buildCatalog(
    categories,
    ["modules.register(new KillAuraModule()); modules.register(new AimAssistModule());"],
    [module("Kill Aura", "COMBAT"), module("Aim Assist", "COMBAT"), module("Unused", "FUN")],
  );
  expect(catalog).toEqual([
    {
      id: "combat",
      name: "Combat",
      modules: [
        {
          id: "aim_assist",
          name: "Aim Assist",
          category: "combat",
          description: "Does Aim Assist.",
        },
        { id: "kill_aura", name: "Kill Aura", category: "combat", description: "Does Kill Aura." },
      ],
    },
  ]);
  expect(moduleAnchor({ id: "kill_aura" })).toBe("module-kill-aura");
});

test("the real sources produce a complete catalog with unique anchors", () => {
  const catalog = readModuleCatalog();
  const modules = catalog.flatMap((category) => category.modules);
  expect(catalog.map((category) => category.name)).toContain("Combat");
  expect(modules.length).toBeGreaterThan(100);
  expect(new Set(modules.map(moduleAnchor)).size).toBe(modules.length);
  for (const module of modules) expect(module.description.length).toBeGreaterThan(10);
});

test("search matches every term and ranks name matches first", () => {
  const catalog = buildCatalog(
    categories,
    ["new AModule() new BModule()"],
    [
      `final class AModule extends Module { AModule() { super("a", "Totem Pop Counter", ModuleCategory.HUD, "Counts pops."); } }`,
      `final class BModule extends Module { BModule() { super("b", "Auto Armor", ModuleCategory.COMBAT, "Keeps a totem and armor on."); } }`,
    ],
  );
  expect(searchModules(catalog, "totem").map((module) => module.name)).toEqual([
    "Totem Pop Counter",
    "Auto Armor",
  ]);
  expect(searchModules(catalog, "totem armor").map((module) => module.name)).toEqual([
    "Auto Armor",
  ]);
  expect(searchModules(catalog, "nothing")).toEqual([]);
});
