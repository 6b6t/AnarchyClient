import { expect, test } from "@playwright/test";

test("the module explorer switches categories, searches, and links to the module list", async ({
  page,
}) => {
  const errors: string[] = [];
  const failed: string[] = [];
  page.on("pageerror", (error) => errors.push(error.message));
  page.on("requestfailed", (request) => failed.push(request.url()));
  await page.goto("/");
  await page.getByRole("button", { name: /^Movement, \d+ modules$/u }).click();
  await expect(page.getByRole("heading", { name: "Movement", exact: true })).toBeVisible();
  await page.getByRole("searchbox", { name: "Search all modules" }).fill("totem");
  const card = page.getByRole("button", { name: "Auto Totem", exact: true });
  await card.click();
  await expect(card).toHaveAttribute("aria-pressed", "true");
  await page.getByRole("link", { name: "Open in the module list" }).click();
  await expect(page).toHaveURL(/\/modules\/#module-auto-totem$/u);
  await expect(page.locator("#module-auto-totem")).toBeInViewport();
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
  expect(errors).toEqual([]);
  expect(failed).toEqual([]);
});

test("static search finds guides and modules without a server", async ({ page }) => {
  const errors: string[] = [];
  page.on("pageerror", (error) => errors.push(error.message));
  await page.goto("/");
  await page.getByRole("button", { name: "Search the site", exact: true }).click();
  const input = page.getByRole("combobox", { name: "Search", exact: true });
  await input.fill("Kill Aura");
  await page.getByRole("option").filter({ hasText: "Kill Aura" }).first().click();
  await expect(page).toHaveURL(/\/modules\/#module-kill-aura$/u);
  await page.getByRole("button", { name: "Search the site", exact: true }).click();
  await input.fill("provenance");
  await page.getByRole("option").filter({ hasText: "provenance" }).first().click();
  await expect(page).toHaveURL(/\/releases\/#verify-your-download$/u);
  expect(errors).toEqual([]);
});

test("theme preferences persist and missing pages remain useful", async ({ page }) => {
  const errors: string[] = [];
  page.on("pageerror", (error) => errors.push(error.message));
  await page.goto("/");
  await expect(page.locator("html")).toHaveClass("dark");
  await page.getByRole("button", { name: "Toggle color theme", exact: true }).click();
  await expect(page.locator("html")).toHaveClass("light");
  await page.reload();
  await expect(page.locator("html")).toHaveClass("light");
  const response = await page.goto("/missing-guide/");
  expect(response?.status()).toBe(404);
  await expect(page.getByRole("heading", { name: "Page not found", exact: true })).toBeVisible();
  await expect(page.locator('meta[name="robots"]')).toHaveAttribute("content", "noindex");
  await page.getByRole("link", { name: "Go to homepage", exact: true }).click();
  await expect(page.getByRole("heading", { level: 1 })).toContainText("built for 6b6t");
  expect(errors).toEqual([]);
});
