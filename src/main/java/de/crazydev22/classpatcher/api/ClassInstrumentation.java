package de.crazydev22.classpatcher.api;

import javassist.ClassClassPath;
import javassist.ClassPool;
import lombok.NonNull;
import lombok.SneakyThrows;

import java.lang.instrument.Instrumentation;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ClassInstrumentation {
    private static final CountDownLatch latch = new CountDownLatch(1);
    private static volatile Instrumentation instrumentation;
    private static PatchTransformer patchTransformer;

    public static ClassPool buildPool() {
        ClassPool pool = new ClassPool();
        pool.appendSystemPath();
        for (Class<?> clazz : getInstrumentation().getAllLoadedClasses())
            pool.appendClassPath(new ClassClassPath(clazz));
        return pool;
    }

    public static PatchTransformer getPatchTransformer() {
        if (patchTransformer != null)
            return patchTransformer;

        var instrumentation = getInstrumentation();
        var transformer = new PatchTransformer();
        instrumentation.addTransformer(transformer, true);
        patchTransformer = transformer;
        return transformer;
    }

    @SneakyThrows
    public static Instrumentation getInstrumentation() {
        if (instrumentation == null) {
            var current = ProcessHandle.current();
            String command = current.info().command().orElseThrow();
            String jarFile = ClassInstrumentation.class.getProtectionDomain().getCodeSource().getLocation().getFile();
            Process process = new ProcessBuilder()
                    .command(command, "-jar", jarFile, String.valueOf(current.pid()), jarFile)
                    .inheritIO()
                    .start();
            boolean done = latch.await(3, TimeUnit.SECONDS);
            if (process.isAlive())
                process.destroy();
            if (!done)
                throw new TimeoutException("Timeout while injecting agent");
        }
        return instrumentation;
    }

    @SuppressWarnings("unused")
    public static void setInstrumentation(@NonNull Instrumentation instrumentation) {
        if (ClassInstrumentation.instrumentation != null)
            throw new IllegalStateException("Instrumentation is already defined!");
        ClassInstrumentation.instrumentation = instrumentation;
        latch.countDown();
    }
}
