import { describe, expect, test } from "bun:test";
import { ButtonStyle, ComponentType } from "discord-api-types/v10";
import { createDiscordEmbed, serializeDiscordEmbed } from "../src/lib/discord-embed";

type EmbedOptions = Parameters<typeof createDiscordEmbed>[0];

function discordEmbedScript(options: EmbedOptions) {
  const component = createDiscordEmbed(options);
  return component ? [{ children: serializeDiscordEmbed(component) }] : [];
}

/** Builds the embed and checks that its script JSON decodes to the same component. */
function serializedEmbed(options: EmbedOptions) {
  const component = createDiscordEmbed(options);
  if (!component) throw new Error("Expected a component embed");
  const [script] = discordEmbedScript(options);
  const payload: unknown = JSON.parse(script?.children ?? "");
  expect(payload).toEqual({ component });
  return { component, children: script?.children ?? "" };
}

describe("Discord component embeds", () => {
  test("renders current page data and link-only actions", () => {
    const { component } = serializedEmbed({
      title: "A new post",
      description: "By Alice",
      url: "https://example.com/post/42",
      links: [{ label: "Author", url: "https://example.com/user/alice" }],
      images: ["https://example.com/art.png"],
    });
    expect(component.type).toBe(ComponentType.Container);
    const [text, gallery] = component.components;
    const actions = component.components.at(-1);
    if (text?.type !== ComponentType.TextDisplay) throw new Error("Expected a text display");
    expect(text.content).toContain("A new post");
    expect(text.content).toContain("By Alice");
    if (gallery?.type !== ComponentType.MediaGallery) throw new Error("Expected a media gallery");
    expect(gallery.items[0]?.media.url).toBe("https://example.com/art.png");
    if (actions?.type !== ComponentType.ActionRow) throw new Error("Expected an action row");
    expect(actions.components).toEqual([
      {
        type: ComponentType.Button,
        style: ButtonStyle.Link,
        label: "View page",
        url: "https://example.com/post/42",
      },
      {
        type: ComponentType.Button,
        style: ButtonStyle.Link,
        label: "Author",
        url: "https://example.com/user/alice",
      },
    ]);
  });

  test("escapes script terminators without changing the JSON data", () => {
    const { component, children } = serializedEmbed({
      title: "</script><script>alert(1)</script>",
      url: "https://example.com",
    });
    expect(children).not.toContain("<");
    const [text] = component.components;
    if (text?.type !== ComponentType.TextDisplay) throw new Error("Expected a text display");
    expect(text.content).toContain("script");
  });

  test("keeps Unicode and long media URLs within the serialized byte limit", () => {
    const card = createDiscordEmbed({
      title: "🌍".repeat(200),
      description: "🌍</script>".repeat(1000),
      url: "https://example.com",
      images: Array.from(
        { length: 20 },
        (_, index) => `https://example.com/${index}/${"x".repeat(1900)}.png`,
      ),
      links: Array.from({ length: 5 }, (_, index) => ({
        label: "🌍".repeat(80),
        url: `https://example.com/${index}/${"x".repeat(450)}`,
      })),
    });
    expect(card).toBeDefined();
    if (!card) throw new Error("Expected a component embed");
    expect(new TextEncoder().encode(serializeDiscordEmbed(card)).byteLength).toBeLessThanOrEqual(
      3000,
    );
    const gallery = card.components.find(
      (component) => component.type === ComponentType.MediaGallery,
    );
    if (gallery?.type === ComponentType.MediaGallery)
      expect(gallery.items.length).toBeLessThanOrEqual(4);
  });

  test("rejects unsafe URLs and omits invalid media and buttons", () => {
    // oxlint-disable-next-line eslint/no-script-url -- This fixture exercises rejection of script URLs.
    const scriptUrl = "javascript:alert(1)";
    expect(discordEmbedScript({ title: "Private", url: scriptUrl })).toEqual([]);
    expect(
      discordEmbedScript({ title: "Private", url: "https://user:password@example.com" }),
    ).toEqual([]);
    const { component } = serializedEmbed({
      title: "Safe",
      url: "https://example.com",
      images: ["file:///tmp/image.png"],
      links: [{ label: "Bad", url: scriptUrl }],
    });
    expect(component.components).toHaveLength(2);
  });
});
