package dev.khloeleclair.create.additionallogistics.common.content.kinetics.verticalBelt;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class VerticalBeltHelper {

    public static BlockPos getPositionForOffset(VerticalBeltBlockEntity belt, int offset) {
        return belt.getController().above(offset);
    }

    public static class Positions implements Collection<BlockPos>, List<BlockPos> {

        private final LevelReader world;
        private final BlockPos origin;
        private int height = -1;

        public Positions(LevelReader world, BlockPos origin) {
            this.world = world;
            this.origin = origin;
        }

        private void load() {
            if (height >= 0)
                return;

            BlockPos currentPos = origin;
            while(height < 1000) {
                if (!(world.getBlockState(currentPos).getBlock() instanceof AbstractVerticalBeltBlock))
                    break;

                currentPos = currentPos.above();
                height++;
            }
        }

        @Override
        public boolean addAll(int index, @NotNull Collection<? extends BlockPos> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public BlockPos get(int index) {
            load();
            if (index < 0 || index >= height)
                throw new IndexOutOfBoundsException();

            if (index == 0)
                return origin;
            return origin.above(index);
        }

        @Override
        public BlockPos set(int index, BlockPos element) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void add(int index, BlockPos element) {
            throw new UnsupportedOperationException();
        }

        @Override
        public BlockPos remove(int index) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int indexOf(Object o) {
            if (!(o instanceof BlockPos opos))
                return -1;

            if (opos.getX() != origin.getX() || opos.getZ() != origin.getZ())
                return -1;

            load();
            int offset = opos.getY() - origin.getY();

            if (offset < 0 || offset >= height)
                return -1;

            return offset;
        }

        @Override
        public int lastIndexOf(Object o) {
            return indexOf(o);
        }

        @Override
        public @NotNull ListIterator<BlockPos> listIterator() {
            return null;
        }

        @Override
        public @NotNull ListIterator<BlockPos> listIterator(int index) {
            return null;
        }

        @Override
        public @NotNull List<BlockPos> subList(int fromIndex, int toIndex) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int size() {
            load();
            return height;
        }

        @Override
        public boolean isEmpty() {
            load();
            return height == 0;
        }

        @Override
        public boolean contains(Object o) {
            return indexOf(o) != -1;
        }

        @Override
        public @NotNull Iterator<BlockPos> iterator() {
            return null;
        }

        @Override
        public @NotNull Object[] toArray() {
            load();
            Object[] result = new Object[height];
            for(int i = 0; i < height; i++) {
                result[i] = i == 0 ? origin : origin.above(i);
            }
            return result;
        }

        @Override
        public @NotNull <T> T[] toArray(@NotNull T[] a) {
            load();
            if (a.length < height)
                // Make a new array of a's runtime type, but my contents:
                return (T[]) Arrays.copyOf(toArray(), height, a.getClass());

            for(int i = 0; i < height; i++)
                a[i] = (T) (i == 0 ? origin : origin.above(i));

            if (a.length > height)
                a[height] = null;

            return a;
        }

        @Override
        public boolean add(BlockPos pos) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean remove(Object o) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean containsAll(@NotNull Collection<?> c) {
            for(var entry : c) {
                if (indexOf(entry) == -1)
                    return false;
            }
            return true;
        }

        @Override
        public boolean addAll(@NotNull Collection<? extends BlockPos> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean removeAll(@NotNull Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean retainAll(@NotNull Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }
    }

    public static class PositionIterator implements Iterator<BlockPos> {

        private final LevelReader world;
        private BlockPos currentPos;
        private boolean hasNext;
        @Nullable
        private BlockPos nextPos;

        public PositionIterator(LevelReader world, BlockPos startingPos) {
            this.world = world;
            currentPos = startingPos;
        }

        @Nullable
        private BlockPos readNext() {
            if (!hasNext) {
                var next = currentPos.above();
                if (world.getBlockState(next).getBlock() instanceof AbstractVerticalBeltBlock)
                    nextPos = next;
                else
                    nextPos = null;
                hasNext = true;
            }

            return nextPos;
        }

        @Override
        public boolean hasNext() {
            return readNext() != null;
        }

        @Override
        public BlockPos next() {
            var next = readNext();
            if (next == null)
                throw new NoSuchElementException();

            currentPos = next;
            hasNext = false;
            return currentPos;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

}
