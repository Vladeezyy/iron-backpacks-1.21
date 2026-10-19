package gr8pefish.ironbackpacks.util;

import net.minecraft.world.InteractionResult;

/** A result with a value: what 1.21.1's InteractionResultHolder was (26.x removed it). */
public record ResultHolder<T>(InteractionResult result, T object) {

    public T getObject() {
        return object;
    }

    public InteractionResult getResult() {
        return result;
    }

    public static <T> ResultHolder<T> fail(T object) {
        return new ResultHolder<>(InteractionResult.FAIL, object);
    }
}
