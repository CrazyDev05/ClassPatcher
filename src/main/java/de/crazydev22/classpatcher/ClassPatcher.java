package de.crazydev22.classpatcher;

import de.crazydev22.classpatcher.api.ClassInstrumentation;
import de.crazydev22.classpatcher.api.PatchTransformer;

import java.io.File;

public class ClassPatcher {
    private final PatchTransformer transformer;
    private final File dataFolder;

    public ClassPatcher(File dataFolder) {
        this.dataFolder = dataFolder;
        transformer = ClassInstrumentation.getPatchTransformer();
        loadPatches();
    }

    public void loadPatches() {
        if (!dataFolder.exists() && !dataFolder.mkdirs())
            throw new RuntimeException("Failed to create data folder " + dataFolder);
        transformer.loadPatches(dataFolder)
                .forEach(transformer::put);
    }
}
