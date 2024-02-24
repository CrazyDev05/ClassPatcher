package de.crazydev22.classpatcher;

import com.sun.tools.attach.VirtualMachine;
import lombok.SneakyThrows;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.util.Arrays;

@SuppressWarnings({"unused", "unchecked"})
public class ClassAgent {

    @SneakyThrows
    public static void main(String[] args) {
        if (args.length != 2)
            return;
        File file = new File(args[1]);
        if (!file.exists())
            return;

        VirtualMachine jvm = VirtualMachine.attach(args[0]);
        jvm.loadAgent(file.getAbsolutePath());
        jvm.detach();
        System.exit(0);
    }

    public static void agentmain(String agentArgs, Instrumentation instrumentation) {
        Arrays.stream(instrumentation.getAllLoadedClasses())
                .filter(clazz -> clazz.getName().equals("de.crazydev22.classpatcher.api.ClassInstrumentation"))
                .findAny()
                .ifPresent(clazz -> {
                    try {
                        clazz.getMethod("setInstrumentation", Instrumentation.class).invoke(null, instrumentation);
                    } catch (Throwable e) {
                        throw new RuntimeException(e);
                    }
                });
    }
}
