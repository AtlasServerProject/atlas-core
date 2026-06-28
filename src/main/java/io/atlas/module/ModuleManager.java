package io.atlas.module;

import io.atlas.AtlasMod;

import java.util.ArrayList;
import java.util.List;

public class ModuleManager {

    private final List<AtlasModule> modules = new ArrayList<>();

    public void register(AtlasModule module) {
        modules.add(module);
    }

    public void enableModules() {

        for (AtlasModule module : modules) {

            AtlasMod.LOGGER.info("Carregando módulo: {}", module.getName());

            module.enable();

            AtlasMod.LOGGER.info("✔ {} carregado.", module.getName());
        }
    }

    public void disableModules() {

        for (AtlasModule module : modules) {

            AtlasMod.LOGGER.info("Desligando módulo: {}", module.getName());

            module.disable();

        }

    }
}