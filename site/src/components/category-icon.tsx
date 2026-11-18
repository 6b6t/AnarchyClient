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
  type LucideIcon,
  type LucideProps,
} from "lucide-react";

// The same Lucide glyphs as the in-game dock (ModuleIcons.java).
const icons: Partial<Record<string, LucideIcon>> = {
  combat: Target,
  render: Eye,
  movement: Move,
  world: Hammer,
  player: User,
  hud: LayoutList,
  misc: Settings,
  fun: Gamepad2,
};

export function CategoryIcon({ category, ...props }: { category: string } & LucideProps) {
  const Icon = icons[category] ?? Shapes;
  return <Icon aria-hidden="true" {...props} />;
}
