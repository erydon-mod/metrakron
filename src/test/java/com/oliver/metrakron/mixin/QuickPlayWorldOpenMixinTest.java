package com.oliver.metrakron.mixin;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Runs against each adapter's actual mapped Minecraft classes, without starting a game. */
class QuickPlayWorldOpenMixinTest {
    @Test
    void worldOpenInjectionMatchesTheExactMinecraftMethodAndArguments() throws IOException {
        ClassNode mixin = readClass("com/oliver/metrakron/mixin/QuickPlayWorldOpenMixin");
        AnnotationNode targetAnnotation = annotation(mixin.visibleAnnotations, mixin.invisibleAnnotations,
                "Lorg/spongepowered/asm/mixin/Mixin;");
        List<?> targets = (List<?>) value(targetAnnotation, "value");
        assertEquals(1, targets.size());
        ClassNode target = readClass(((Type) targets.get(0)).getInternalName());
        MethodNode handler = mixin.methods.stream()
                .filter(method -> method.name.equals("metrakron$observeQuickPlayWorld"))
                .findFirst().orElseThrow();
        AnnotationNode injection = annotation(handler.visibleAnnotations, handler.invisibleAnnotations,
                "Lorg/spongepowered/asm/mixin/injection/Inject;");
        List<?> selectors = (List<?>) value(injection, "method");
        assertEquals(1, selectors.size());
        String selector = (String) selectors.get(0);
        List<MethodNode> matches = target.methods.stream()
                .filter(method -> (method.name + method.desc).equals(selector)).toList();
        assertEquals(1, matches.size(), "Missing or ambiguous hook in " + target.name + ": " + selector);

        MethodNode method = matches.get(0);
        assertEquals(0, method.access & Opcodes.ACC_STATIC);
        assertEquals(0, handler.access & Opcodes.ACC_STATIC);
        Type[] handlerArguments = Type.getArgumentTypes(handler.desc);
        assertArrayEquals(Type.getArgumentTypes(method.desc),
                Arrays.copyOf(handlerArguments, handlerArguments.length - 1));
        assertEquals("org/spongepowered/asm/mixin/injection/callback/CallbackInfo",
                handlerArguments[handlerArguments.length - 1].getInternalName());
        List<?> injectionPoints = (List<?>) value(injection, "at");
        assertEquals("HEAD", value((AnnotationNode) injectionPoints.get(0), "value"));
    }

    private static ClassNode readClass(String name) throws IOException {
        try (InputStream stream = QuickPlayWorldOpenMixinTest.class.getClassLoader()
                .getResourceAsStream(name + ".class")) {
            assertNotNull(stream, "Missing class resource: " + name);
            ClassNode node = new ClassNode();
            new ClassReader(stream).accept(node, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG);
            return node;
        }
    }

    private static AnnotationNode annotation(List<AnnotationNode> visible, List<AnnotationNode> invisible,
                                             String descriptor) {
        List<AnnotationNode> annotations = new ArrayList<>();
        if (visible != null) annotations.addAll(visible);
        if (invisible != null) annotations.addAll(invisible);
        return annotations.stream().filter(item -> item.desc.equals(descriptor)).findFirst().orElseThrow();
    }

    private static Object value(AnnotationNode annotation, String key) {
        for (int index = 0; index < annotation.values.size(); index += 2) {
            if (key.equals(annotation.values.get(index))) return annotation.values.get(index + 1);
        }
        throw new AssertionError("Missing annotation value: " + key);
    }
}
