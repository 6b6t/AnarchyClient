import type { CSSProperties } from "react";

export function installSteps(minecraft?: string) {
  return [
    `Install Fabric Loader${minecraft ? ` for Minecraft ${minecraft}` : ""} and add Fabric API.`,
    "Put the AnarchyClient JAR in the mods folder.",
    "Start Minecraft with the Fabric profile and join a world.",
    "Press Right Shift to open the menu.",
  ];
}

export function InstallSteps({ minecraft }: { minecraft?: string | undefined }) {
  return (
    <ol className="install-steps">
      {installSteps(minecraft).map((step, index) => {
        const style: CSSProperties & { "--step": number } = { "--step": index };
        return (
          <li key={step} style={style}>
            <span className="step-number mono" aria-hidden="true">
              {index + 1}
            </span>
            <p>{step}</p>
          </li>
        );
      })}
    </ol>
  );
}
