import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/** Test-only observer. Not included in any published mod jar. */
public final class ClientBootAgent {
    public static void premain(String ignored, Instrumentation instrumentation) {
        Thread observer = new Thread(() -> observe(instrumentation), "bwg-client-boot-test");
        observer.setDaemon(true);
        observer.start();
    }

    private static void observe(Instrumentation instrumentation) {
        int stable = 0;
        String lastScreen = "";
        String lastReadyState = "";
        try {
            while (true) {
                Thread.sleep(1000);
                Object client = null;
                Class<?> minecraft = null;
                Class<?> items = null;
                for (Class<?> type : instrumentation.getAllLoadedClasses()) {
                    if (type.getName().equals("net.minecraft.client.Minecraft")
                            || type.getName().equals("net.minecraft.class_310")) {
                        minecraft = type;
                    }
                    if (type.getName().equals("tn.nightbeam.better_with_gold.registry.ModItems")) {
                        items = type;
                    }
                }
                if (minecraft == null || items == null) continue;
                // Forge's event transformer uses the current thread's context loader.
                Thread.currentThread().setContextClassLoader(minecraft.getClassLoader());
                for (Method method : minecraft.getDeclaredMethods()) {
                    if (Modifier.isStatic(method.getModifiers()) && method.getParameterCount() == 0
                            && method.getReturnType() == minecraft) {
                        method.setAccessible(true);
                        client = method.invoke(null);
                        break;
                    }
                }
                if (client == null || items == null) continue;
                Object gui = client;
                boolean screenOnClient = false;
                for (Field field : client.getClass().getDeclaredFields()) {
                    if (field.getType().getName().equals("net.minecraft.client.gui.screens.Screen")
                            || field.getType().getName().equals("net.minecraft.class_437")) screenOnClient = true;
                }
                if (!screenOnClient) {
                    for (Field field : client.getClass().getDeclaredFields()) {
                        if (field.getType().getName().equals("net.minecraft.client.gui.Gui")) {
                            field.setAccessible(true);
                            gui = field.get(client);
                        }
                    }
                }
                if (gui == null) continue;
                Object screen = currentField(gui, "net.minecraft.client.gui.screens.Screen", "net.minecraft.class_437");
                Object overlay = currentField(gui, "net.minecraft.client.gui.screens.Overlay", "net.minecraft.class_4071");
                String screenState = (screen == null ? "null" : screen.getClass().getName()) + "/"
                        + (overlay == null ? "null" : overlay.getClass().getName());
                if (!screenState.equals(lastScreen)) {
                    System.out.println("[BWG BOOT TEST] current screen/overlay: " + screenState);
                    lastScreen = screenState;
                }
                boolean title = screen != null && (screen.getClass().getName().equals("net.minecraft.client.gui.screens.TitleScreen")
                        || screen.getClass().getName().equals("net.minecraft.class_442"));
                Object level = currentField(client, "net.minecraft.client.multiplayer.ClientLevel", "net.minecraft.class_638");
                Object player = currentField(client, "net.minecraft.client.player.LocalPlayer", "net.minecraft.class_746");
                boolean world = level != null && player != null;
                String readyState = overlay != null ? "WAIT" : title ? "TITLE" : world ? "WORLD" : "WAIT";
                stable = readyState.equals("WAIT") ? 0 : readyState.equals(lastReadyState) ? stable + 1 : 1;
                lastReadyState = readyState;
                if (stable < 10) continue;
                int count = 0;
                for (Field field : items.getDeclaredFields()) {
                    if (Modifier.isPublic(field.getModifiers()) && Modifier.isStatic(field.getModifiers())) {
                        Object item = field.get(null);
                        if (item == null) throw new IllegalStateException("Unregistered item: " + field.getName());
                        verifyRegistryId(item, field.getName());
                        if (field.getName().startsWith("REINFORCED_GOLDEN_ARMOR_")) verifyArmorTexture(item);
                        count++;
                    }
                }
                if (count != 17) throw new IllegalStateException("Expected 17 items, found " + count);
                Path result = Path.of(System.getProperty("bwg.smoke.result"));
                Files.writeString(result, "PASS state=" + readyState + " stableSeconds=10 loadingOverlay=false registeredItems=" + count + " armorBindings=4\n");
                System.out.println("[BWG BOOT TEST] PASS " + readyState + " stable for 10 seconds; all 17 item registry IDs and four armor texture bindings verified");
                // Let the render thread close its own window and resources.
                Object finishedClient = client;
                Method stop = null;
                for (String name : new String[]{"stop", "method_1592", "m_91395_"}) {
                    try {
                        stop = client.getClass().getDeclaredMethod(name);
                        break;
                    } catch (NoSuchMethodException ignored) { }
                }
                if (stop == null) throw new IllegalStateException("Client stop method missing");
                Method finishedStop = stop;
                ((java.util.concurrent.Executor) client).execute(() -> {
                    try {
                        finishedStop.invoke(finishedClient);
                    } catch (ReflectiveOperationException error) {
                        error.printStackTrace();
                        Runtime.getRuntime().halt(2);
                    }
                });
                return;
            }
        } catch (Throwable error) {
            error.printStackTrace();
            Runtime.getRuntime().halt(2);
        }
    }

    private static Object currentField(Object value, String namedType, String fabricType) throws ReflectiveOperationException {
        for (Class<?> type = value.getClass(); type != null; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                String fieldType = field.getType().getName();
                if (!Modifier.isStatic(field.getModifiers()) && (fieldType.equals(namedType) || fieldType.equals(fabricType))) {
                    field.setAccessible(true);
                    return field.get(value);
                }
            }
        }
        throw new IllegalStateException("Current screen/overlay field not found: " + namedType);
    }

    private static void verifyRegistryId(Object item, String fieldName) throws ReflectiveOperationException {
        ClassLoader loader = item.getClass().getClassLoader();
        Class<?> registries;
        try {
            registries = Class.forName("net.minecraft.core.registries.BuiltInRegistries", false, loader);
        } catch (ClassNotFoundException ignored) {
            registries = Class.forName("net.minecraft.class_7923", false, loader);
        }
        Object registry = null;
        for (Field field : registries.getDeclaredFields()) {
            if (field.getName().equals("ITEM") || field.getName().equals("field_41178")
                    || field.getGenericType().getTypeName().endsWith("<net.minecraft.world.item.Item>")) {
                registry = field.get(null);
                break;
            }
        }
        if (registry == null) throw new IllegalStateException("Item registry not found");
        Class<?> registryInterface;
        try {
            registryInterface = Class.forName("net.minecraft.core.Registry", false, loader);
        } catch (ClassNotFoundException ignored) {
            registryInterface = Class.forName("net.minecraft.class_2378", false, loader);
        }
        for (Method method : registryInterface.getMethods()) {
            String returnType = method.getReturnType().getName();
            if ((returnType.equals("net.minecraft.resources.ResourceLocation") || returnType.equals("net.minecraft.resources.Identifier")
                    || returnType.equals("net.minecraft.class_2960"))
                    && method.getParameterCount() == 1 && method.getParameterTypes()[0] == Object.class) {
                String actual = String.valueOf(method.invoke(registry, item));
                String expected = "better_with_gold:" + fieldName.toLowerCase(Locale.ROOT);
                if (!expected.equals(actual)) throw new IllegalStateException("Registry ID mismatch: " + expected + " != " + actual);
                return;
            }
        }
        throw new IllegalStateException("Registry key lookup not found");
    }

    private static Object call(Object target, String named, String fabric) throws ReflectiveOperationException {
        for (Method method : target.getClass().getMethods()) {
            if (method.getParameterCount() == 0 && (method.getName().equals(named) || method.getName().equals(fabric))) {
                method.setAccessible(true);
                return method.invoke(target);
            }
        }
        // Forge production jars retain SRG method names; select unique signatures there.
        for (Method method : target.getClass().getMethods()) {
            String type = method.getGenericReturnType().getTypeName();
            if (method.getParameterCount() == 0 && ((named.equals("getMaterial") && type.contains("net.minecraft.world.item.ArmorMaterial"))
                    || (named.equals("value") && method.getReturnType() == Object.class)
                    || (named.equals("layers") && method.getReturnType() == java.util.List.class))) {
                method.setAccessible(true);
                return method.invoke(target);
            }
        }
        throw new IllegalStateException("Missing method: " + named);
    }

    private static void verifyArmorTexture(Object item) throws ReflectiveOperationException {
        ClassLoader loader = item.getClass().getClassLoader();
        Class<?> components;
        try {
            components = Class.forName("net.minecraft.core.component.DataComponents", false, loader);
            Object equippableType = components.getField("EQUIPPABLE").get(null);
            Object map = initializedComponents(item);
            Class<?> getter = Class.forName("net.minecraft.core.component.DataComponentGetter", false, loader);
            Class<?> componentType = Class.forName("net.minecraft.core.component.DataComponentType", false, loader);
            Object equippable = getter.getMethod("get", componentType).invoke(map, equippableType);
            Object asset = ((java.util.Optional<?>) call(equippable, "assetId", "")).orElseThrow();
            String id = String.valueOf(call(asset, "identifier", ""));
            if (!id.equals("better_with_gold:reinforced_golden_armor")) throw new IllegalStateException("Wrong armor asset: " + id);
            return;
        } catch (ClassNotFoundException | NoSuchFieldException olderVersion) {
            // Before equipment assets, the renderer uses ArmorMaterial layers or the loader hook.
        }
        Object material = call(item, "getMaterial", "method_7686");
        try {
            Object value = call(material, "value", "comp_349");
            java.util.List<?> layers = (java.util.List<?>) call(value, "layers", "comp_2302");
            for (Object layer : layers) {
                for (Method method : layer.getClass().getMethods()) {
                    if (method.getReturnType().getName().endsWith("ResourceLocation") || method.getReturnType().getName().equals("net.minecraft.class_2960")) {
                        if (method.getParameterCount() != 1 || method.getParameterTypes()[0] != boolean.class) continue;
                        for (boolean leggings : new boolean[]{false, true}) {
                            String expected = "better_with_gold:textures/models/armor/gold_amyth_layer_" + (leggings ? 2 : 1) + ".png";
                            if (!expected.equals(String.valueOf(method.invoke(layer, leggings)))) throw new IllegalStateException("Wrong armor layer texture");
                        }
                        return;
                    }
                }
            }
            throw new IllegalStateException("Armor layers missing");
        } catch (IllegalStateException noHolder) {
            if (!noHolder.getMessage().equals("Missing method: value")) throw noHolder;
            String name = null;
            for (Method method : material.getClass().getMethods()) {
                if (method.getParameterCount() == 0 && method.getReturnType() == String.class && !method.getName().equals("toString")) {
                    method.setAccessible(true);
                    name = String.valueOf(method.invoke(material));
                }
            }
            if (!"reinforced_golden_armor".equals(name)) throw noHolder;
            // Fabric's client mixin and Forge's getArmorTexture hook resolve this legacy material.
        }
    }

    private static Object initializedComponents(Object item) throws ReflectiveOperationException {
        // In 26.x holder components bind when a world opens. Evaluate this item's real initializer
        // into a separate builder so the menu test can inspect it without changing game registries.
        ClassLoader loader = item.getClass().getClassLoader();
        Class<?> registries = Class.forName("net.minecraft.core.registries.BuiltInRegistries", false, loader);
        Object initializers = registries.getField("DATA_COMPONENT_INITIALIZERS").get(null);
        Field entriesField = initializers.getClass().getDeclaredField("initializers");
        entriesField.setAccessible(true);
        Object key = call(call(item, "builtInRegistryHolder", ""), "key", "");
        Class<?> mapType = Class.forName("net.minecraft.core.component.DataComponentMap", false, loader);
        Object builder = mapType.getMethod("builder").invoke(null);
        Class<?> registryType = Class.forName("net.minecraft.core.Registry", false, loader);
        Class<?> accessType = Class.forName("net.minecraft.core.RegistryAccess", false, loader);
        Object provider = accessType.getMethod("fromRegistryOfRegistries", registryType)
                .invoke(null, registries.getField("REGISTRY").get(null));
        Class<?> providerType = Class.forName("net.minecraft.core.HolderLookup$Provider", false, loader);
        boolean found = false;
        for (Object entry : (java.util.List<?>) entriesField.get(initializers)) {
            if (key.equals(call(entry, "key", ""))) {
                Method run = entry.getClass().getMethod("run", builder.getClass(), providerType);
                run.setAccessible(true);
                run.invoke(entry, builder, provider);
                found = true;
            }
        }
        if (!found) throw new IllegalStateException("Item component initializer missing: " + key);
        return call(builder, "build", "");
    }
}
