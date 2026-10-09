package gr8pefish.ironbackpacks.platform;

import java.util.ServiceLoader;

/** The loader's {@link IPlatform}, from META-INF/services of the neoforge / fabric project (MultiLoader template). */
public final class Services {
    public static final IPlatform PLATFORM = ServiceLoader.load(IPlatform.class, IPlatform.class.getClassLoader()).findFirst()
            .orElseThrow(() -> new IllegalStateException("No Iron Backpacks platform implementation found"));

    private Services() {}
}
