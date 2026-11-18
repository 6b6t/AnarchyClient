import {
  Eye,
  Gamepad2,
  Hammer,
  LayoutList,
  Move,
  Settings,
  Shapes,
  Target,
  User,
  type LucideProps,
} from "lucide-react";

// The same Lucide glyphs as the in-game dock (ModuleIcons.java).
const icons = {
  combat: Target,
  render: Eye,
  movement: Move,
  world: Hammer,
  player: User,
  hud: LayoutList,
  misc: Settings,
  fun: Gamepad2,
} as const;

export function CategoryIcon({ category, ...props }: { category: string } & LucideProps) {
  const Icon = icons[category as keyof typeof icons] ?? Shapes;
  return <Icon aria-hidden="true" {...props} />;
}
