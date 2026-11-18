package net.blockhost.anarchyclient.gametest;

import net.blockhost.anarchyclient.module.DebugValueRegistry;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Runs every scenario and reports all failures at the end, so one broken module does not hide
 * the others. Set {@code ANARCHYCLIENT_GAMETESTS} to a comma-separated list of scenario names to
 * run only those.
 */
public final class AnarchyClientGameTests implements FabricClientGameTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(AnarchyClientGameTests.class);

    @Override
    public void runTest(final ClientGameTestContext context) {
        HandlerErrors.install();
        Set<String> only = filter();
        Map<String, String> failures = new LinkedHashMap<>();
        List<String> passed = new ArrayList<>();
        for (Map.Entry<String, GameScenario> scenario : GameScenarios.all().entrySet()) {
            String name = scenario.getKey();
            if (!only.isEmpty() && !only.contains(name.toLowerCase(Locale.ROOT))) {
                continue;
            }
            LOGGER.info("SCENARIO START {}", name);
            HandlerErrors.drain();
            DebugValueRegistry.clearAll();
            try {
                scenario.getValue().run(context);
                List<String> errors = HandlerErrors.drain();
                if (!errors.isEmpty()) {
                    throw new AssertionError("Module event handlers threw " + errors.size() + " exception(s); first: "
                            + errors.getFirst());
                }
                passed.add(name);
                LOGGER.info("SCENARIO PASS {}", name);
            } catch (Throwable failure) {
                failures.put(name, String.valueOf(failure.getMessage()));
                LOGGER.error("SCENARIO FAIL {}", name, failure);
                LOGGER.error("SCENARIO DEBUG {} {}", name, DebugValueRegistry.snapshot());
                HandlerErrors.drain().stream().limit(3).forEach(error -> LOGGER.error("SCENARIO HANDLER ERROR {} {}", name, error));
                context.setScreen(() -> null);
            }
        }
        LOGGER.info("SCENARIO SUMMARY {} passed, {} failed: {}", passed.size(), failures.size(), failures.keySet());
        if (!failures.isEmpty()) {
            throw new AssertionError(failures.entrySet().stream()
                    .map(entry -> entry.getKey() + ": " + entry.getValue())
                    .collect(Collectors.joining("\n", failures.size() + " scenario(s) failed:\n", "")));
        }
    }

    private static Set<String> filter() {
        String value = System.getenv("ANARCHYCLIENT_GAMETESTS");
        if (value == null || value.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(value.split(","))
                .map(name -> name.trim().toLowerCase(Locale.ROOT))
                .filter(name -> !name.isEmpty())
                .collect(Collectors.toSet());
    }
}
