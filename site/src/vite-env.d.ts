/// <reference types="vite/client" />

declare module "virtual:anarchyclient-release" {
  const release: import("./lib/releases").Release | null;
  export default release;
}

declare module "virtual:anarchyclient-modules" {
  const categories: import("./lib/modules").ModuleCategory[];
  export default categories;
}
