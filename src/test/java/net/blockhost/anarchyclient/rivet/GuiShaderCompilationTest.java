package net.blockhost.anarchyclient.rivet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.lwjgl.util.shaderc.Shaderc.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class GuiShaderCompilationTest {

    private static final Pattern INCLUDE = Pattern.compile("^#include <([^:>]+):([^>]+)>$", Pattern.MULTILINE);

    @ParameterizedTest
    @ValueSource(
            strings = {
                "panel.vsh",
                "glass_panel.fsh",
                "sdf_fill.fsh",
                "sdf_outline.fsh",
                "sdf_shadow.fsh",
                "matrix_panel.fsh",
                "caustics_panel.fsh",
                "retro_grid_panel.fsh",
                "fire_panel.fsh",
                "deep_panel.fsh",
                "simplex_flow_panel.fsh"
            })
    void compilesGuiShadersForRenderPearl(final String name) throws IOException {
        String source = resolveIncludes(readResource("/assets/anarchyclient/shaders/core/" + name));
        long compiler = shaderc_compiler_initialize();
        long options = shaderc_compile_options_initialize();
        try {
            shaderc_compile_options_set_target_env(options, shaderc_target_env_vulkan, shaderc_env_version_vulkan_1_2);
            shaderc_compile_options_set_auto_bind_uniforms(options, true);
            int kind = name.endsWith(".vsh") ? shaderc_vertex_shader : shaderc_fragment_shader;
            long result = shaderc_compile_into_spv(compiler, source, kind, name, "main", options);
            try {
                assertEquals(
                        shaderc_compilation_status_success,
                        shaderc_result_get_compilation_status(result),
                        () -> shaderc_result_get_error_message(result));
            } finally {
                shaderc_result_release(result);
            }
        } finally {
            shaderc_compile_options_release(options);
            shaderc_compiler_release(compiler);
        }
    }

    private static String resolveIncludes(final String source) throws IOException {
        var matcher = INCLUDE.matcher(source);
        StringBuilder resolved = new StringBuilder();
        while (matcher.find()) {
            String include = readResource("/assets/" + matcher.group(1) + "/shaders/include/" + matcher.group(2));
            matcher.appendReplacement(resolved, Matcher.quoteReplacement(resolveIncludes(include)));
        }
        matcher.appendTail(resolved);
        return resolved.toString();
    }

    private static String readResource(final String path) throws IOException {
        try (var stream = Objects.requireNonNull(GuiShaderCompilationTest.class.getResourceAsStream(path), path)) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
