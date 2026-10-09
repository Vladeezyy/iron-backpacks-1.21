package gr8pefish.ironbackpacks.api.backpack.variant;

import java.util.Locale;
import java.util.Set;

import javax.annotation.Nonnull;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Codec;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

/**
 * An enum to hold the possible specialties of a backpack.
 *
 * These are assumed to be mutually exclusive, at least when the backpack has a specialty (i.e. not NONE).
 */
public enum BackpackSpecialty implements StringRepresentable {
    NONE, //No specialty on the backpack (e.g. Basic backpack)
    STORAGE, //Increases raw storage capacity
    UPGRADE //Increases max upgrade points
    ;

    /** Lower case name in JSON ("storage"); the backpack data component stores {@link #name()} like the 1.12 NBT did. */
    public static final Codec<BackpackSpecialty> CODEC = StringRepresentable.fromEnum(BackpackSpecialty::values);
    public static final StreamCodec<ByteBuf, BackpackSpecialty> STREAM_CODEC = ByteBufCodecs.VAR_INT.map(i -> values()[i], Enum::ordinal);

    /**
     * Gets the name of the specialty.
     *
     * @return - a String representation of the specialty.
     */
    @Nonnull
    public String getName() {
        return name().toLowerCase(Locale.ENGLISH);
    }

    @Override
    @Nonnull
    public String getSerializedName() {
        return getName();
    }

    /**
     * Gets the specialty given the name of it.
     * The name can be obtained from {@link BackpackSpecialty#getName()}
     *
     * @param name - the name of the specialty to return.
     * @return - the specialty if found, NONE otherwise.
     */
    @Nonnull
    public static BackpackSpecialty getBackpackSpecialty(@Nonnull String name) {
        Preconditions.checkNotNull(name, "Name cannot be null");

        //Loop through possible specialties
        for (BackpackSpecialty backpackSpecialty : BackpackSpecialty.values())
            //Check if it is the one we want
            if (backpackSpecialty.getName().equalsIgnoreCase(name))
                //if so, return it
                return backpackSpecialty;

        //No specialty found, return NONE (should be unreachable)
        return NONE;
    }

    /**
     * Gets the list of specialties that excludes {@link BackpackSpecialty#NONE}
     *
     * @return - an immutable set of Strings
     */
    @Nonnull
    public static Set<String> getNonNoneNames() {
        return ImmutableSet.of(STORAGE.getName(), UPGRADE.getName());
    }

}
