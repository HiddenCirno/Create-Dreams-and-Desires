package uwu.lopyluna.create_dd.infrastructure.porting_lib_classes;

import it.unimi.dsi.fastutil.ints.IntAVLTreeSet;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntSortedSet;
import it.unimi.dsi.fastutil.objects.ObjectAVLTreeSet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import uwu.lopyluna.create_dd.infrastructure.fabric_classes.*;

import java.util.*;
import java.util.function.Predicate;

public class ItemStackHandler extends SnapshotParticipant<ItemStackHandlerSnapshot> implements Storage<ItemVariant>, ExtendedStorage<ItemVariant>, INBTSerializable<CompoundTag>, SlotExposedStorage {
    private static final ItemVariant blank = ItemVariant.blank();

    /**
     * Do not directly access this array. It must be kept in sync with the others. Restricting access may break existing mods.
     */
    @Deprecated
    public ItemStack[] stacks;
    protected ItemVariant[] variants;
    protected Map<Item, IntSortedSet> lookup;
    protected ItemStackHandlerSlotView[] views;
    protected SortedSet<ItemStackHandlerSlotView> nonEmptyViews;

    public ItemStackHandler() {
        this(1);
    }

    public ItemStackHandler(int stacks) {
        this(emptyStackArray(stacks));
    }

    public ItemStackHandler(ItemStack[] stacks) {
        this.stacks = stacks;
        this.variants = new ItemVariant[stacks.length];
        this.lookup = new HashMap<>();
        this.views = new ItemStackHandlerSlotView[stacks.length];
        this.nonEmptyViews = makeViewSet(null);
        for (int i = 0; i < stacks.length; i++) {
            ItemStack stack = stacks[i];
            this.variants[i] = ItemVariant.of(stack);
            getIndices(stack.getItem()).add(i);
            ItemStackHandlerSlotView view = new ItemStackHandlerSlotView(this, i);
            this.views[i] = view;
            if (!stack.isEmpty())
                nonEmptyViews.add(view);
        }
    }

    @Override
    public long insert(ItemVariant resource, long maxAmount, TransactionContext transaction) {
        StoragePreconditions.notBlankNotNegative(resource, maxAmount);
        long inserted = 0;
        for (int i = 0; i < stacks.length; i++) {
            if (!isItemValid(i, resource, maxAmount))
                continue;
            ItemStack stack = stacks[i];
            if (!stack.isEmpty()) { // add to an existing stack
                inserted += insertToExistingStack(i, stack, resource, maxAmount - inserted, transaction);
            } else { // create a new stack
                inserted += insertToNewStack(i, resource, maxAmount - inserted, transaction);
            }
            if (maxAmount - inserted <= 0)
                break; // fully inserted
        }
        return inserted;
    }

    protected long insertToExistingStack(int index, ItemStack stack, ItemVariant resource, long maxAmount, TransactionContext ctx) {
        int space = getSpace(index, resource, stack);
        if (space <= 0)
            return 0; // no room? skip
        if (!resource.matches(stack))
            return 0; // can't stack? skip
        int toInsert = (int) Math.min(space, maxAmount);
        updateSnapshots(ctx);
        stack = ItemHandlerHelper.growCopy(stack, toInsert);
        contentsChangedInternal(index, stack, ctx);
        // no types were changed, only counts. Lookup is unchanged.
        return toInsert;
    }

    protected long insertToNewStack(int index, ItemVariant resource, long maxAmount, TransactionContext ctx) {
        int maxSize = getStackLimit(index, resource);
        int toInsert = (int) Math.min(maxSize, maxAmount);
        ItemStack stack = resource.toStack(toInsert);
        updateSnapshots(ctx);
        contentsChangedInternal(index, stack, ctx);
        return toInsert;
    }

    protected int getSpace(int index, ItemVariant resource, ItemStack stack) {
        int maxSize = getStackLimit(index, resource);
        int size = stack.getCount();
        return maxSize - size;
    }

    @Override
    public long insertSlot(int slot, ItemVariant resource, long maxAmount, TransactionContext transaction) {
        if (slot < 0 || slot > getSlots())
            return 0;
        ItemStack stack = stacks[slot];
        if (!isItemValid(slot, resource, maxAmount))
            return 0;
        if (!stack.isEmpty()) { // add to an existing stack
            return insertToExistingStack(slot, stack, resource, maxAmount, transaction);
        } else { // create a new stack
            return insertToNewStack(slot, resource, maxAmount, transaction);
        }
    }

    @Override
    public long extract(ItemVariant resource, long maxAmount, TransactionContext transaction) {
        StoragePreconditions.notBlankNotNegative(resource, maxAmount);
        Item item = resource.getItem();
        IntSortedSet indices = lookup.get(item);
        if (indices == null || indices.isEmpty())
            return 0; // no slots hold this item
        long extracted = 0;
        for (IntIterator itr = indices.intIterator(); itr.hasNext();) {
            int i = itr.nextInt();
            ItemStack stack = stacks[i];
            if (stack.hasTag() && !resource.matches(stack))
                continue; // nbt doesn't allow stacking? skip
            int size = stack.getCount();
            int toExtract = (int) Math.min(size, maxAmount - extracted);
            extracted += toExtract;
            int newSize = size - toExtract;
            boolean empty = newSize <= 0;
            stack = empty ? ItemStack.EMPTY : ItemHandlerHelper.copyStackWithSize(stack, newSize);
            updateSnapshots(transaction);
            contentsChangedInternal(i, stack, transaction);
        }
        return extracted;
    }

    @Override
    public long extractSlot(int slot, ItemVariant resource, long maxAmount, TransactionContext transaction) {
        if (slot < 0 || slot > getSlots())
            return 0;
        ItemStack stack = stacks[slot];
        if (stack.isEmpty() || !resource.matches(stack))
            return 0;
        int count = stack.getCount();
        int extracted = (int) Math.min(maxAmount, count);
        boolean empty = extracted >= count;
        ItemStack newStack = empty ? ItemStack.EMPTY : ItemHandlerHelper.copyStackWithSize(stack, count - extracted);
        updateSnapshots(transaction);
        contentsChangedInternal(slot, newStack, transaction);
        return extracted;
    }

    @Override
    @Nullable
    public ResourceAmount<ItemVariant> extractMatching(Predicate<ItemVariant> predicate, long maxAmount, TransactionContext transaction) {
        if (nonEmptyViews.isEmpty())
            return null;
        ItemVariant variant = null;
        for (ItemStackHandlerSlotView view : nonEmptyViews) {
            ItemVariant resource = view.getResource();
            if (predicate.test(resource)) {
                variant = resource;
                break;
            }
        }
        if (variant == null)
            return null;
        long extracted = extract(variant, maxAmount, transaction);
        if (extracted == 0)
            return null;
        return new ResourceAmount<>(variant, extracted);
    }

    @Override
    @Nullable
    public StorageView<ItemVariant> exactView(ItemVariant resource) {
        StoragePreconditions.notBlank(resource);
        IntSortedSet indices = lookup.get(resource.getItem());
        if (indices == null || indices.isEmpty())
            return null;
        for (IntIterator itr = indices.intIterator(); itr.hasNext();) {
            int i = itr.nextInt();
            ItemStack stack = stacks[i];
            if (resource.matches(stack)) {
                return new ItemStackHandlerSlotView(this, i);
            }
        }
        return null;
    }

    protected void contentsChangedInternal(int slot, ItemStack newStack, @Nullable TransactionContext ctx) {
        ItemStack oldStack = stacks[slot];
        stacks[slot] = newStack;
        variants[slot] = ItemVariant.of(newStack);
        if (!oldStack.sameItem(newStack)) {
            // item changed, update the lookup
            updateLookup(oldStack.getItem(), newStack.getItem(), slot);
        }
        boolean oldEmpty = oldStack.isEmpty();
        boolean newEmpty = newStack.isEmpty();
        // if empty status changed, the non-empty views must be updated
        if (oldEmpty && !newEmpty) {
            nonEmptyViews.add(views[slot]);
        } else if (!oldEmpty && newEmpty) {
            nonEmptyViews.remove(views[slot]);
        }
        if (ctx != null) TransactionCallback.onSuccess(ctx, () -> onContentsChanged(slot));
    }

    @Override
    public Iterator<StorageView<ItemVariant>> iterator() {
        return new StorageViewArrayIterator<>(views);
    }

    @Override
    public Iterator<? extends StorageView<ItemVariant>> nonEmptyViews() {
        return nonEmptyViews.iterator();
    }

    @Override
    protected ItemStackHandlerSnapshot createSnapshot() {
        return SnapshotData.of(this);
    }

    @Override
    protected void readSnapshot(ItemStackHandlerSnapshot snapshot) {
        snapshot.apply(this);
    }

    @Override
    public String toString() {
        return  getClass().getSimpleName() + '{' + "stacks=" + Arrays.toString(stacks) + ", variants=" + Arrays.toString(variants) + '}';
    }

    public int getSlots() {
        return stacks.length;
    }

    public void setStackInSlot(int slot, ItemStack stack) {
        contentsChangedInternal(slot, stack, null);
        onContentsChanged(slot);
    }

    /**
     * This stack should never be modified.
     */
    public ItemStack getStackInSlot(int slot) {
        return stacks[slot];
    }

    public ItemVariant getVariantInSlot(int slot) {
        return variants[slot];
    }

    public int getSlotLimit(int slot) {
        return getStackInSlot(slot).getMaxStackSize();
    }

    protected int getStackLimit(int slot, ItemVariant resource) {
        return Math.min(getSlotLimit(slot), resource.getItem().getMaxStackSize());
    }

    public boolean isItemValid(int slot, ItemVariant resource, long amount) {
        return true;
    }

    protected void onLoad() {
    }

    protected void onContentsChanged(int slot) {
    }

    public void setSize(int size) {
        this.stacks = new ItemStack[size];
        this.variants = new ItemVariant[size];
        this.views = new ItemStackHandlerSlotView[size];
        for (int i = 0; i < this.stacks.length; i++) {
            stacks[i] = ItemStack.EMPTY;
            variants[i] = blank;
            views[i] = new ItemStackHandlerSlotView(this, i);
        }
        lookup.clear();
        nonEmptyViews.clear();
    }

    @Override
    public CompoundTag serializeNBT() {
        ListTag nbtTagList = new ListTag();
        for (int i = 0; i < stacks.length; i++) {
            ItemStack stack = stacks[i];
            if (!stack.isEmpty()) {
                CompoundTag itemTag = new CompoundTag();
                itemTag.putInt("Slot", i);
                stack.save(itemTag);
                nbtTagList.add(itemTag);
            }
        }
        CompoundTag nbt = new CompoundTag();
        nbt.put("Items", nbtTagList);
        nbt.putInt("Size", stacks.length);
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        setSize(nbt.contains("Size", Tag.TAG_INT) ? nbt.getInt("Size") : stacks.length);
        ListTag tagList = nbt.getList("Items", Tag.TAG_COMPOUND);
        for (int i = 0; i < tagList.size(); i++) {
            CompoundTag itemTags = tagList.getCompound(i);
            int slot = itemTags.getInt("Slot");

            if (slot >= 0 && slot < stacks.length) {
                ItemStack stack = ItemStack.of(itemTags);
                contentsChangedInternal(slot, stack, null);
            }
        }
        // fill in lookup with deserialized data
        for (int i = 0; i < stacks.length; i++) {
            ItemStack stack = stacks[i];
            getIndices(stack.getItem()).add(i);
        }
        onLoad();
    }

    protected void updateLookup(Item oldItem, Item newItem, int index) {
        getIndices(oldItem).remove(index);
        getIndices(newItem).add(index);
    }

    protected IntSortedSet getIndices(Item item) {
        return getIndices(lookup, item);
    }

    protected static IntSortedSet getIndices(Map<Item, IntSortedSet> lookup, Item item) {
        return lookup.computeIfAbsent(item, ItemStackHandler::makeSet);
    }

    protected static IntSortedSet makeSet(Item item) {
        return new IntAVLTreeSet(Integer::compareTo);
    }

    protected static SortedSet<ItemStackHandlerSlotView> makeViewSet(@Nullable SortedSet<ItemStackHandlerSlotView> original) {
        return original != null
                ? new ObjectAVLTreeSet<>(original)
                : new ObjectAVLTreeSet<>(Comparator.comparingInt(view -> view.index));
    }

    public static ItemStack[] emptyStackArray(int size) {
        ItemStack[] stacks = new ItemStack[size];
        Arrays.fill(stacks, ItemStack.EMPTY);
        return stacks;
    }

    public static class SnapshotData implements ItemStackHandlerSnapshot {
        public final ItemStack[] stacks;
        public final ItemVariant[] variants;
        public final Map<Item, IntSortedSet> lookup;
        public final SortedSet<ItemStackHandlerSlotView> nonEmptyViews;

        protected SnapshotData(ItemStack[] stacks, ItemVariant[] variants, Map<Item, IntSortedSet> lookup, SortedSet<ItemStackHandlerSlotView> nonEmptyViews) {
            this.stacks = stacks;
            this.variants = variants;
            this.lookup = lookup;
            this.nonEmptyViews = nonEmptyViews;
        }

        @Override
        public void apply(ItemStackHandler handler) {
            handler.stacks = stacks;
            handler.variants = variants;
            handler.lookup = lookup;
            handler.nonEmptyViews = nonEmptyViews;
        }

        public static SnapshotData of(ItemStackHandler handler) {
            ItemStack[] stacks = handler.stacks;
            ItemStack[] items = new ItemStack[stacks.length];
            System.arraycopy(stacks, 0, items, 0, stacks.length);

            ItemVariant[] variants = handler.variants;
            ItemVariant[] vars = new ItemVariant[variants.length];
            System.arraycopy(variants, 0, vars, 0, variants.length);

            Map<Item, IntSortedSet> lookup = handler.lookup;
            Map<Item, IntSortedSet> map = new HashMap<>();
            // a deep copy here seems unavoidable
            lookup.forEach((item, set) -> {
                IntSortedSet copy = makeSet(item);
                copy.addAll(set);
                map.put(item, copy);
            });

            SortedSet<ItemStackHandlerSlotView> nonEmptyViews = handler.nonEmptyViews;
            SortedSet<ItemStackHandlerSlotView> views = makeViewSet(nonEmptyViews);
            return new SnapshotData(items, vars, map, views);
        }
    }

    @Override
    protected void onFinalCommit() {
        super.onFinalCommit();
    }
}