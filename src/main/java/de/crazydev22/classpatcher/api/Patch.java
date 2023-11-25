package de.crazydev22.classpatcher.api;

import com.google.gson.*;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;
import lombok.NonNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public record Patch(@NonNull TargetType target, @NonNull EditType edit, @NonNull String name, @NonNull List<@NonNull String> args, @NonNull List<@NonNull String> body) {

    public enum TargetType {
        CONSTRUCTOR,
        INITIALIZER,
        METHOD,
    }

    public enum EditType {
        BEFORE,
        AFTER,
        REPLACE
    }

    public CtClass[] getArgs(ClassPool pool) throws NotFoundException {
        Map<String, CtClass> map = new HashMap<>();
        args.forEach(className -> map.put(className, getCtClass(className, pool)));

        if (map.containsValue(null)) {
            var missing = map.keySet();
            missing.removeIf(key -> map.get(key) != null);
            throw new NotFoundException("Could not find classes for [" + String.join(", ", missing) + "]");
        }
        return map.values().toArray(CtClass[]::new);
    }

    private CtClass getCtClass(String className, ClassPool pool) {
        try {
            return pool.getCtClass(className);
        } catch (NotFoundException ignored) {}
        return null;
    }

    public static class Adapter implements JsonSerializer<Patch>, JsonDeserializer<Patch> {
        @Override
        public Patch deserialize(JsonElement element, java.lang.reflect.Type type, JsonDeserializationContext context) throws JsonParseException {
            JsonObject object = element.getAsJsonObject();
            return new Patch(
                    valueOf(object, "target", TargetType.METHOD),
                    valueOf(object, "edit", EditType.REPLACE),
                    object.get("name").getAsString(),
                    asList(object, "args"),
                    asList(object, "body")
            );
        }

        private <T extends Enum<T>> T valueOf(JsonObject object, String name, T fallback) {
            try {
                return Enum.valueOf(fallback.getDeclaringClass(), object.get(name).getAsString().toUpperCase());
            } catch (Throwable e) {
                return fallback;
            }
        }

        @Override
        public JsonElement serialize(Patch patch, java.lang.reflect.Type type, JsonSerializationContext context) {
            JsonObject object = new JsonObject();
            object.addProperty("target", patch.target().name());
            object.addProperty("name", patch.name());
            object.add("args", toArray(patch.args()));
            object.add("body", toArray(patch.body()));
            return object;
        }

        private JsonArray toArray(List<String> list) {
            JsonArray array = new JsonArray();
            list.forEach(array::add);
            return array;
        }

        private List<String> asList(JsonObject object, String name) {
            try {
                return object.getAsJsonArray(name)
                        .asList()
                        .stream()
                        .map(JsonElement::getAsString)
                        .toList();
            } catch (Throwable e) {
                return List.of();
            }
        }
    }
}
