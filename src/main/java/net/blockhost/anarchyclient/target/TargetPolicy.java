package net.blockhost.anarchyclient.target;

import java.util.EnumSet;
import java.util.Set;

public record TargetPolicy(Set<TargetKind> kinds, boolean invisibles, boolean dead, boolean ignoreFriends,
                           String friends, boolean ignoreTeams, boolean antiBot) {

    public TargetPolicy {
        kinds = kinds.isEmpty() ? Set.of() : Set.copyOf(kinds);
        friends = friends == null ? "" : friends;
    }

    public static TargetPolicy of(final boolean players, final boolean hostiles, final boolean passives,
                                  final boolean invisibles, final boolean ignoreFriends, final String friends,
                                  final boolean ignoreTeams, final boolean antiBot) {
        EnumSet<TargetKind> kinds = EnumSet.noneOf(TargetKind.class);
        if (players) {
            kinds.add(TargetKind.PLAYER);
        }
        if (hostiles) {
            kinds.add(TargetKind.HOSTILE);
        }
        if (passives) {
            kinds.add(TargetKind.PASSIVE);
            kinds.add(TargetKind.WATER_CREATURE);
        }
        return new TargetPolicy(kinds, invisibles, false, ignoreFriends, friends, ignoreTeams, antiBot);
    }

    public boolean allows(final TargetKind kind) {
        return this.kinds.contains(kind);
    }
}
