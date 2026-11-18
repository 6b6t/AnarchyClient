import { createFileRoute, Link } from "@tanstack/react-router";
import { ArrowDownToLine, BadgeCheck, Cpu, Layers, PanelsTopLeft } from "lucide-react";
import modules from "virtual:anarchyclient-modules";
import release from "virtual:anarchyclient-release";
import { Faq } from "../components/faq";
import { InstallSteps } from "../components/install-steps";
import { ModuleExplorer } from "../components/module-explorer";
import { pageHead } from "../lib/metadata";
import { site } from "../lib/site";

export const Route = createFileRoute("/")({
  head: () => pageHead(undefined, "index", release),
  component: Home,
});

const moduleCount = modules.reduce((sum, category) => sum + category.modules.length, 0);

const highlights = [
  {
    icon: PanelsTopLeft,
    title: "Glass over the game",
    text: "The menu floats as frosted glass panels. The world stays visible behind them, so you never lose sight of what is around you.",
  },
  {
    icon: Cpu,
    title: "Vulkan and OpenGL",
    text: "Every panel, outline, and overlay renders through Blaze3D, so it works on both of Minecraft's graphics backends.",
  },
  {
    icon: Layers,
    title: "Profiles, friends, and themes",
    text: "Save module setups as profiles, keep a friends list that combat modules respect, and choose the accent color and glass density.",
  },
  {
    icon: BadgeCheck,
    title: "Builds you can verify",
    text: "GitHub Actions builds every release from the public source and signs it, so you can check a JAR before you run it.",
  },
];

const questions = [
  {
    question: "Is AnarchyClient free?",
    answer:
      "Yes. AnarchyClient is open source under the MIT license. You can read every line of it on GitHub.",
  },
  {
    question: "Which Minecraft version does it support?",
    answer: release?.minecraft
      ? `The current release, ${release.tag}, is for Minecraft Java Edition ${release.minecraft} with Fabric. Each release supports one Minecraft version.`
      : "Each release supports one Minecraft Java Edition version with Fabric. The JAR name gives the version after mc-.",
  },
  {
    question: "Does it work on servers other than 6b6t?",
    answer:
      "The mod runs on any server, but the 6b6t team builds and tests it for 6b6t. Other servers can have rules against utility mods. Check them before you join.",
  },
  {
    question: "Can I use it with other mods?",
    answer:
      "Performance and visual mods usually work. Other utility clients change the same game behavior, so remove them before you report a problem.",
  },
  {
    question: "Is it an official 6b6t product?",
    answer:
      "No. It is a side project by the 6b6t team with no support guarantee. Bug reports and pull requests on GitHub are welcome.",
  },
];

function Home() {
  return (
    <main id="main-content" className="home">
      <section className="hero">
        <div className="hero-backdrop" aria-hidden="true" />
        <div className="hero-inner shell-width">
          <div className="hero-copy">
            <p className="eyebrow mono">
              Fabric{release?.minecraft ? ` · Minecraft ${release.minecraft}` : ""} · 6b6t
            </p>
            <h1 id="anarchyclient">
              The utility client <span className="accent">built for 6b6t</span>
            </h1>
            <p className="lead">
              {moduleCount} modules for combat, movement, building, and awareness, behind one glass
              menu that keeps the game in view. Free and open source.
            </p>
            <div className="actions">
              <Link className="button primary large" to="/releases/">
                <ArrowDownToLine size={20} aria-hidden="true" />
                Download{release ? ` ${release.tag}` : ""}
              </Link>
              <Link
                className="button ghost large"
                to="/$slug/"
                params={{ slug: "getting-started" }}
              >
                Install guide
              </Link>
            </div>
          </div>
          <figure className="hero-shot glass">
            <img
              src="/assets/images/demo.png"
              alt="The AnarchyClient menu in game: a category dock, the Movement modules, and the No Fall settings, over a blurred grass field."
              width={1920}
              height={1080}
            />
          </figure>
        </div>
      </section>

      <section className="explorer-section shell-width" aria-labelledby="modules">
        <div className="section-heading">
          <h2 id="modules">{moduleCount} modules, one menu</h2>
          <p>
            Try the menu here. Pick a category in the dock, search, or select a module to read what
            it does. <Link to="/modules/">See the full list</Link>
          </p>
        </div>
        <ModuleExplorer categories={modules} />
      </section>

      <section className="highlights shell-width" aria-labelledby="highlights">
        <h2 id="highlights" className="sr-only">
          Highlights
        </h2>
        <ul className="highlight-grid">
          {highlights.map(({ icon: Icon, title, text }) => (
            <li key={title} className="highlight glass">
              <Icon size={22} aria-hidden="true" />
              <h3>{title}</h3>
              <p>{text}</p>
            </li>
          ))}
        </ul>
      </section>

      <section className="steps-section shell-width" aria-labelledby="install">
        <div className="section-heading">
          <h2 id="install">Set up in four steps</h2>
          <p>
            You need Fabric Loader and Fabric API.{" "}
            <Link to="/$slug/" params={{ slug: "getting-started" }}>
              Read the full install guide
            </Link>
          </p>
        </div>
        <InstallSteps minecraft={release?.minecraft} />
      </section>

      <section className="faq-section shell-width" aria-labelledby="questions">
        <div className="section-heading">
          <h2 id="questions">Questions</h2>
          <p>
            Something else? <a href={site.discord}>Ask on the 6b6t Discord</a>.
          </p>
        </div>
        <Faq items={questions} />
      </section>

      <section className="finale" aria-labelledby="latest-release">
        <div className="shell-width finale-inner">
          <h2 id="latest-release">
            See you on <span className="accent">6b6t</span>
          </h2>
          <p>
            {release ? `${release.tag} is the latest release.` : "The latest release is on GitHub."}{" "}
            Download it, drop it in your mods folder, and press Right Shift.
          </p>
          <div className="actions">
            <Link className="button primary large" to="/releases/">
              <ArrowDownToLine size={20} aria-hidden="true" />
              Download AnarchyClient
            </Link>
            <a className="button ghost large" href={site.github}>
              View the source
            </a>
          </div>
        </div>
      </section>
    </main>
  );
}
