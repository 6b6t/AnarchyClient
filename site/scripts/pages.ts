import { readdir } from "node:fs/promises";
import { pageUrl } from "../src/lib/site";

/** Every public HTML page of the static site. */
export async function publicPages() {
  const docs = (await readdir(new URL("../content/docs", import.meta.url)))
    .filter((file) => file.endsWith(".mdx"))
    .map((file) => pageUrl(file.slice(0, -4)));
  return ["/", "/modules/", ...docs].toSorted();
}
