import { expect, test } from "bun:test";
import { serializeDiscordEmbed } from "../src/lib/discord-embed";
import { pageEmbed, pageHead } from "../src/lib/metadata";

test("download previews name the release and its Minecraft version", () => {
  const component = pageEmbed(
    { title: "Downloads", description: "Get the JAR" },
    "releases",
    { title: "AnarchyClient", url: "https://example.com", description: "A client" },
    { version: "9.8.7", minecraft: "26.4" },
  );
  if (!component) throw new Error("Expected a download preview");
  const content = serializeDiscordEmbed(component);
  expect(content).toContain("Release 9.8.7 · Minecraft 26.4");
  expect(content).toContain("https://example.com/releases/");
});

test("page heads keep the published canonical URLs", () => {
  const head = pageHead(
    { title: "Build from source", description: "Build it" },
    "build-from-source",
  );
  expect(head.links).toContainEqual({
    rel: "canonical",
    href: "https://client.6b6t.org/build-from-source/",
  });
  expect(head.meta).toContainEqual({ title: "Build from source · AnarchyClient" });
  expect(head.meta).toContainEqual({
    property: "og:image",
    content: "https://client.6b6t.org/assets/images/demo.png",
  });
  expect(pageHead().links).toContainEqual({ rel: "canonical", href: "https://client.6b6t.org/" });
  expect(pageHead().meta).toContainEqual({ title: "AnarchyClient" });
});

test("Discord preview JSON cannot close its containing script element", () => {
  const head = pageHead(
    { title: "</script><script>alert(1)</script>", description: "A guide" },
    "contributing",
  );
  const [script] = head.scripts;
  expect(script?.children).not.toContain("</script>");
  expect(() => {
    JSON.parse(script?.children ?? "");
  }).not.toThrow();
});
