package dev.khloeleclair.create.additionallogistics.common.registries;

import net.minecraftforge.fml.loading.LoadingModList;

public enum CALMods {

    FACTORY_LOGISTICS("create_factory_logistics")
    ;

    private final String id;

    CALMods(String id) {
        this.id = id;
    }

    public String getId() { return id; }

    public boolean isLoaded() {
        return LoadingModList.get().getModFileById(id) != null;
    }

}
