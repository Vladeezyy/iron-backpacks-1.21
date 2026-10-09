package gr8pefish.ironbackpacks.api.backpack;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import gr8pefish.ironbackpacks.api.IronBackpacksAPI;
import gr8pefish.ironbackpacks.api.backpack.inventory.BackpackInventory;
import gr8pefish.ironbackpacks.api.backpack.variant.BackpackSpecialty;
import gr8pefish.ironbackpacks.api.backpack.variant.BackpackType;
import gr8pefish.ironbackpacks.api.backpack.variant.BackpackVariant;
import gr8pefish.ironbackpacks.api.upgrade.BackpackUpgrade;
import gr8pefish.ironbackpacks.core.RegistrarIronBackpacks;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.NonNullList;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

/**
 * The main class to hold info about a backpack.
 *
 * Contains {@link BackpackVariant}, a {@code List<{@link BackpackUpgrade}>},
 * an inventory and an {@link UUID} of the owner.
 *
 * Also contains simple getters/setters for the above.
 *
 * Finally, has some additional methods, such as serialization to the backpack's data components (1.12: the
 * "packInfo" and "packInv" NBT tags; now {@link RegistrarIronBackpacks#PACK_INFO} and {@link RegistrarIronBackpacks#PACK_INVENTORY}).
 */
public class BackpackInfo {


    // Constants

    public static final int NO_COLOR = -1;


    // Fields

    @Nonnull
    private final List<BackpackUpgrade> upgrades;
    @Nonnull
    private BackpackVariant backpackVariant;
    private BackpackInventory inventory;
    @Nullable
    private UUID owner;
    private int rgbColor;


    // Constructors

    private BackpackInfo(@Nonnull BackpackVariant backpackVariant, @Nonnull List<BackpackUpgrade> upgrades) {
        Preconditions.checkNotNull(backpackVariant, "Backpack variant cannot be null");
        Preconditions.checkNotNull(upgrades, "Upgrade list cannot be null");

        this.backpackVariant = backpackVariant;
        this.upgrades = upgrades;

        this.rgbColor = NO_COLOR; //uncolored by default
    }

    public BackpackInfo(@Nonnull BackpackVariant backpackVariant) {
        this(backpackVariant, Lists.newArrayList());
    }

    private BackpackInfo() {
        //noinspection ConstantConditions - null/null is automatically registered, so we know it's always there.
        this(new BackpackVariant(IronBackpacksAPI.getBackpackType(IronBackpacksAPI.NULL), BackpackSpecialty.NONE), Lists.newArrayList());
    }


    // Getters/Setters

    /**
     * Gets the variant of the backpack.
     *
     * @return - {@link BackpackVariant}
     */
    @Nonnull
    public BackpackVariant getVariant() {
        return backpackVariant;
    }

    /**
     * Sets the {@link BackpackVariant} of the backpack.
     *
     * @param variant - The backpack variant to set it too
     * @return - The updated backpack info
     */
    @Nonnull
    public BackpackInfo setVariant(@Nonnull BackpackVariant variant) {
        Preconditions.checkNotNull(variant, "Backpack variant cannot be null");
        backpackVariant = variant;
        return this;
    }

    /**
     * Gets an immutable list of the {@link BackpackUpgrade}s on the backpack.
     *
     * @return - An immutable list of the backpack's upgrades
     */
    @Nonnull
    public List<BackpackUpgrade> getUpgrades() {
        return ImmutableList.copyOf(upgrades);
    }

    /**
     * Adds a {@link BackpackUpgrade} to the backpack's upgrade list.
     * IMPORTANT: DOES NO CHECKING (other than nonnull)
     *
     * @param upgrade - The upgrade to add
     * @return - The updated backpack info
     */
    @Nonnull
    public BackpackInfo addUpgrade(@Nonnull BackpackUpgrade upgrade) {
        Preconditions.checkNotNull(upgrade, "Upgrade cannot be null");
        upgrades.add(upgrade);
        return this;
    }

    /**
     * Removes a {@link BackpackUpgrade} from the backpack's upgrade list.
     * IMPORTANT: DOES NO CHECKING (other than nonnull)
     *
     * @param upgrade - The upgrade to remove
     * @return - The updated backpack info
     */
    @Nonnull
    public BackpackInfo removeUpgrade(@Nonnull BackpackUpgrade upgrade) {
        Preconditions.checkNotNull(upgrade, "Upgrade cannot be null");
        upgrades.remove(upgrade);
        return this;
    }

    public BackpackInventory getInventory() {
        return inventory;
    }

    public BackpackInfo setInventory(@Nonnull BackpackInventory inventory) {
        this.inventory = inventory;
        return this;
    }

    /**
     * Returns the {@link UUID} of the owner.
     * WARNING: May be NULL if no owner is assigned.
     *
     * @return - The UUID of the owner
     */
    @Nullable
    public UUID getOwner() {
        return owner;
    }

    /**
     * Sets the {@link UUID} of the owner to this backpack.
     * WARNING: May be NULL for no owner.
     *
     * @param owner - The UUID to set
     * @return - The updated backpack info
     */
    @Nonnull
    public BackpackInfo setOwner(@Nullable UUID owner) {
        this.owner = owner;
        return this;
    }

    /**
     * Gets if the backpack is dyed/colored or not.
     *
     * @return - {@link boolean}
     */
    public boolean getIsColored() {
        return rgbColor != NO_COLOR;
    }

    /**
     * Gets the dyed color of the backpack, as the {@link int} representation of a RGB color value.
     *
     * @return - The RGB color
     */
    public int getRGBColor() {
        return rgbColor;
    }

    /**
     * Sets the dyed color of the backpack, using a {@link int} representation of a RGB color value.
     * Set to NO_COLOR (-1) to have no additional color.
     *
     * @param rgbColor - the integer representation of a RGB colo value
     * @return - the updated {@link BackpackInfo}
     */
    public BackpackInfo setRGBColor(int rgbColor) {
        this.rgbColor = rgbColor;
        return this;
    }

    // Serialization (1.12 INBTSerializable "packInfo": type, spec, own, color, upgrade)

    /**
     * The stored form of a backpack's info: the {@link RegistrarIronBackpacks#PACK_INFO} data component.
     */
    public record Data(ResourceLocation type, BackpackSpecialty spec, Optional<UUID> owner, Optional<Integer> color,
                       List<ResourceLocation> upgrades) {
        public static final Codec<Data> CODEC = RecordCodecBuilder.create(i -> i.group(
                ResourceLocation.CODEC.fieldOf("type").forGetter(Data::type),
                BackpackSpecialty.CODEC.fieldOf("spec").forGetter(Data::spec),
                UUIDUtil.CODEC.optionalFieldOf("own").forGetter(Data::owner),
                Codec.INT.optionalFieldOf("color").forGetter(Data::color),
                ResourceLocation.CODEC.listOf().optionalFieldOf("upgrade", List.of()).forGetter(Data::upgrades)
        ).apply(i, Data::new));
        public static final StreamCodec<ByteBuf, Data> STREAM_CODEC = StreamCodec.composite(
                ResourceLocation.STREAM_CODEC, Data::type,
                BackpackSpecialty.STREAM_CODEC, Data::spec,
                ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC), Data::owner,
                ByteBufCodecs.optional(ByteBufCodecs.INT), Data::color,
                ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list()), Data::upgrades,
                Data::new);
    }

    @Nonnull
    public Data serialize() {
        List<ResourceLocation> installedUpgrades = Lists.newArrayList();
        for (BackpackUpgrade backpackUpgrade : upgrades)
            installedUpgrades.add(backpackUpgrade.getIdentifier());

        return new Data(backpackVariant.getBackpackType().getIdentifier(), backpackVariant.getBackpackSpecialty(), Optional.ofNullable(owner),
                getIsColored() ? Optional.of(rgbColor) : Optional.empty(), List.copyOf(installedUpgrades));
    }

    public void deserialize(@Nonnull Data data) {
        // Deserialize backpack info
        backpackVariant = new BackpackVariant(IronBackpacksAPI.getBackpackType(data.type()), data.spec());
        owner = data.owner().orElse(null);
        data.color().ifPresent(color -> rgbColor = color);

        // Deserialize upgrade
        for (ResourceLocation identifier : data.upgrades()) {
            BackpackUpgrade backpackUpgrade = IronBackpacksAPI.getUpgrade(identifier);
            if (!backpackUpgrade.isNull())
                upgrades.add(backpackUpgrade);
        }
    }

    public boolean conflicts(@Nullable BackpackUpgrade upgrade) {
        if (upgrade == null)
            return false;

        for (BackpackUpgrade installed : upgrades)
            if (upgrade.isConflicting(installed))
                return true;

        return false;
    }

    public boolean hasUpgrade(@Nullable BackpackUpgrade backpackUpgrade) {
        return upgrades.contains(backpackUpgrade);
    }

    public int getPointsUsed() {
        int used = 0;
        for (BackpackUpgrade backpackUpgrade : upgrades)
            used += backpackUpgrade.getApplicationCost();

        return used;
    }

    public int getMaxPoints() {
        return backpackVariant.getBackpackType().getBaseMaxUpgradePoints() + (backpackVariant.getBackpackSpecialty() == BackpackSpecialty.UPGRADE ? 5 : 0);
    }

    // Helper methods

    @Nonnull
    public static BackpackInfo fromStack(@Nonnull ItemStack stack) {
        Preconditions.checkNotNull(stack, "ItemStack cannot be null");

        Data data = stack.isEmpty() ? null : stack.get(RegistrarIronBackpacks.PACK_INFO.get());
        if (data == null)
            return new BackpackInfo();

        BackpackInfo tagged = fromData(data);

        BackpackInventory stackHandler = new BackpackInventory(tagged.backpackVariant.getBackpackSize().getTotalSize());
        ItemContainerContents contents = stack.getOrDefault(RegistrarIronBackpacks.PACK_INVENTORY.get(), ItemContainerContents.EMPTY);
        NonNullList<ItemStack> items = NonNullList.withSize(stackHandler.getSlots(), ItemStack.EMPTY);
        contents.copyInto(items);
        for (int i = 0; i < items.size(); i++)
            stackHandler.setStackInSlot(i, items.get(i));

        return tagged.setInventory(stackHandler);
    }

    @Nonnull
    public static BackpackInfo fromData(@Nullable Data data) {
        BackpackInfo backpackInfo = new BackpackInfo();
        if (data == null)
            return backpackInfo;

        backpackInfo.deserialize(data);
        return backpackInfo;
    }

    @Nonnull
    public static BackpackInfo upgradeTo(@Nonnull BackpackInfo toUpgrade, @Nonnull BackpackType newType, @Nonnull BackpackSpecialty newSpecialty) {
        return new BackpackInfo(new BackpackVariant(newType, newSpecialty), toUpgrade.upgrades)
                .setOwner(toUpgrade.getOwner())
                .setInventory(toUpgrade.inventory);
    }

    /**
     * Gets the color from the backpack's data directly
     * (i.e. inexpensive lookup for rendering purposes)
     *
     * @param stack - The backpack item stack to check
     * @return - The color in RGB, -1 if none
     */
    public static int getColor(@Nonnull ItemStack stack) {
        Preconditions.checkNotNull(stack, "ItemStack cannot be null");

        Data data = stack.get(RegistrarIronBackpacks.PACK_INFO.get());
        if (data != null && data.color().isPresent())
            return data.color().get();

        return -1; // no color
    }
}
