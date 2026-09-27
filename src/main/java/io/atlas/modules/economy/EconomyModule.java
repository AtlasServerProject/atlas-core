package io.atlas.modules.economy;

import io.atlas.module.AtlasModule;

public class EconomyModule implements AtlasModule {

    @Override
    public String getName() {
        return "Economy";
    }

    @Override
    public void enable() {

        System.out.println("Economia iniciada.");

    }

    @Override
    public void disable() {

    }

}