package de.crazydev22.classpatcher;

import lombok.NonNull;
import lombok.SneakyThrows;

import java.lang.instrument.Instrumentation;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ClassInstrumentation {
    private static final CountDownLatch latch = new CountDownLatch(1);
    private static volatile Instrumentation instrumentation;

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
