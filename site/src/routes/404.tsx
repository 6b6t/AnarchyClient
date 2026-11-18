import { createFileRoute } from "@tanstack/react-router";
import { NotFoundPage } from "../components/not-found";

export const Route = createFileRoute("/404")({
  head: () => ({
    meta: [{ title: "Page not found · AnarchyClient" }, { name: "robots", content: "noindex" }],
  }),
  component: NotFoundPage,
});
