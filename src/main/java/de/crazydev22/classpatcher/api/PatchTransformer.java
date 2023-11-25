package de.crazydev22.classpatcher.api;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import javassist.ClassPool;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.LoaderClassPath;
import lombok.Data;
import lombok.NonNull;
import lombok.SneakyThrows;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.UnmodifiableClassException;
import java.security.ProtectionDomain;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

@Data
public class PatchTransformer implements ClassFileTransformer {
    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(Patch.class, new Patch.Adapter())
            .setPrettyPrinting()
            .create();
    private final Map<@NonNull String, @NonNull List<@NonNull Patch>> patches = new HashMap<>();

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> clazz, ProtectionDomain domain, byte[] bytes) throws IllegalClassFormatException {
        if (!patches.containsKey(className))
            return bytes;

        try {
            var pool = makePool(loader);
            var ctClass = pool.makeClassIfNew(new ByteArrayInputStream(bytes));
            List<Patch> remove = new ArrayList<>();
            for (Patch patch : patches.get(className)) {
                try {
                    CtClass[] args = patch.getArgs(pool);
                    CtBehavior behavior = switch (patch.target()) {
                        case INITIALIZER -> ctClass.getClassInitializer();
                        case CONSTRUCTOR -> ctClass.getDeclaredConstructor(args);
                        case METHOD -> ctClass.getDeclaredMethod(patch.name(), args);
                    };
                    String body = "{"+String.join("\n", patch.body())+"}";
                    switch (patch.edit()) {
                        case BEFORE -> behavior.insertBefore(body);
                        case AFTER -> behavior.insertAfter(body);
                        case REPLACE -> behavior.setBody(body);
                    }
                } catch (Throwable e) {
                    remove.add(patch);
                    e.printStackTrace();
                }
            }
            patches.get(className).removeAll(remove);
            return ctClass.toBytecode();
        } catch (Throwable e) {
            patches.remove(className);
            e.printStackTrace();
            return bytes;
        }
    }

    private ClassPool makePool(ClassLoader loader) {
        ClassPool pool = new ClassPool(true);
        pool.appendClassPath(new LoaderClassPath(loader));
        return pool;
    }

    @SneakyThrows
    public Map<String, List<Patch>> loadPatches(@NonNull File file) {
        Map<String, List<Patch>> patches = new HashMap<>();
        if (file.isDirectory()) {
            var files = file.listFiles();
            if (files != null)
                for (File sub : files)
                    patches.putAll(loadPatches(sub));
            return patches;
        }

        var obj = gson.fromJson(new FileReader(file), JsonObject.class);
        var name = obj.get("name").getAsString();
        List<Patch> list = patches.getOrDefault(name, new ArrayList<>());
        obj.getAsJsonArray("patches")
                .asList()
                .stream()
                .map(element -> gson.fromJson(element, Patch.class))
                .forEach(list::add);

        patches.put(name, list);
        return patches;
    }

    public void put(@NonNull String className, @NonNull List<@NonNull Patch> patches) {
        this.patches.put(className, patches);

        try {
            var instrumentation = ClassInstrumentation.getInstrumentation();
            Arrays.stream(instrumentation.getAllLoadedClasses())
                    .filter(clazz -> clazz.getName().equals(className))
                    .findAny()
                    .ifPresent(classes -> {
                        try {
                            instrumentation.retransformClasses(classes);
                        } catch (UnmodifiableClassException ignored) {}
                    });
        } catch (Throwable ignored) {}
    }
}
