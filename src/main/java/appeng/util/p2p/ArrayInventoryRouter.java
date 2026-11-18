package appeng.util.p2p;

import java.util.ArrayList;
import java.util.Map;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;

public class ArrayInventoryRouter<T extends IRoutedTunnel> implements IInventoryRouter<T> {

    private enum ViewType {
        ALL,
        OTHERS,
        OUTPUTS,
        OTHER_OUTPUTS,
        INPUT,
        NONE
    }

    private static class ViewInfo {

        int tableIndex = -1;
        ViewType type = ViewType.NONE;
        int size = 0;
    }

    private final T input;
    private final P2PTransportPolicy policy;
    private final Reference2ObjectMap<T, ViewInfo> members = new Reference2ObjectOpenHashMap<>();

    private final ArrayList<T> table = new ArrayList<>(); // input always come first, even if empty
    private final IntArrayList cumSum = new IntArrayList();

    private int min1StackLimit = Integer.MAX_VALUE;
    private int min2StackLimit = Integer.MAX_VALUE;
    private int inputStackLimit = Integer.MAX_VALUE;
    private T minMember;
    private boolean stackLimitChanged;

    public ArrayInventoryRouter(T input, P2PTransportPolicy policy) {
        this.input = input;
        this.policy = policy;
        this.members.put(input, new ViewInfo());
    }

    @Override
    public void addMember(T output) {
        members.putIfAbsent(output, new ViewInfo());
        stackLimitChanged = true;
    }

    @Override
    public void removeMember(T output) {
        if (output == input) {
            return;
        }
        members.remove(output);
        stackLimitChanged = true;
    }

    @Override
    public void rebuildLayout() {
        table.clear();
        cumSum.clear();
        table.add(input);
        cumSum.add(0);

        int inputSize = input.getTargetInventorySize();
        int sum = inputSize;
        cumSum.add(sum);

        for (Map.Entry<T, ViewInfo> entry : members.entrySet()) {
            T member = entry.getKey();
            ViewInfo view = entry.getValue();
            if (member == input) {
                view.tableIndex = 0;
                continue;
            }
            int mySize = member.getTargetInventorySize();
            if (mySize == 0) {
                view.tableIndex = -1;
                continue;
            }
            view.tableIndex = table.size();
            table.add(member);
            cumSum.add(sum += mySize);
        }

        for (Map.Entry<T, ViewInfo> entry : members.entrySet()) {
            T member = entry.getKey();
            ViewInfo view = entry.getValue();

            int mySize = getSnapshotTargetSize(view);
            int newViewSize = 0;
            if (member == input) {
                if (policy.inputReachesOutputs()) {
                    newViewSize = cumSum.getInt(table.size()) - mySize;
                }
            } else {
                if (policy.outputReachesPeers()) {
                    newViewSize += cumSum.getInt(table.size()) - mySize;
                } else if (policy.outputReachesInput()) {
                    newViewSize += inputSize;
                }
            }
            member.setViewSize(newViewSize);
            view.size = newViewSize;
            view.type = getViewType(member, mySize > 0);
        }

        stackLimitChanged = true;
    }

    @Override
    public int getViewStackLimit(T viewer) {
        if (stackLimitChanged) {
            refreshStackLimits();
            stackLimitChanged = false;
        }
        final int limit;
        if (viewer == input) {
            limit = policy.inputReachesOutputs() ? (viewer == minMember ? min2StackLimit : min1StackLimit) : 64;
        } else {
            if (policy.outputReachesPeers()) {
                limit = viewer == minMember ? min2StackLimit : min1StackLimit;
            } else if (policy.outputReachesInput()) {
                limit = inputStackLimit;
            } else {
                limit = 64;
            }
        }
        return limit == Integer.MAX_VALUE ? 64 : limit;
    }

    private void refreshStackLimits() {
        min1StackLimit = Integer.MAX_VALUE;
        min2StackLimit = Integer.MAX_VALUE;
        inputStackLimit = Integer.MAX_VALUE;
        minMember = null;
        for (final T member : members.keySet()) {
            final int limit = member.getTargetInventoryStackLimit();
            if (member == input) {
                inputStackLimit = limit;
            }
            if (limit < min1StackLimit) {
                min2StackLimit = min1StackLimit;
                min1StackLimit = limit;
                minMember = member;
            } else if (limit < min2StackLimit) {
                min2StackLimit = limit;
            }
        }
    }

    @Override
    public boolean resolve(T viewer, int slot, long offset, SlotView<T> out) {
        final ViewInfo view = members.get(viewer);
        if (view == null) {
            return false;
        }
        final int viewSize = view.size;
        if (slot < 0 || slot >= viewSize) {
            return false;
        }

        int slotIndex = -1;
        int myIndex = view.tableIndex;
        int mySize = getSnapshotTargetSize(view);
        int inputSize = cumSum.getInt(1);
        int myCumSum = myIndex == -1 ? 0 : cumSum.getInt(myIndex);
        ViewType viewType = view.type;

        int invOffset, slotOffset, slotTarget;

        switch (viewType) {

            case ALL:

                invOffset = (int) Math.floorMod(offset, table.size());
                slotOffset = cumSum.getInt(invOffset);
                slotIndex = (slotOffset + slot) % viewSize;
                break;

            case OUTPUTS:

                invOffset = (int) Math.floorMod(offset, table.size() - 1);
                slotOffset = cumSum.getInt(invOffset + 1) - inputSize;
                slotTarget = (slotOffset + slot) % viewSize;
                slotIndex = slotTarget + inputSize;
                break;

            case OTHERS:

                invOffset = (int) Math.floorMod(offset, table.size() - 1);
                slotOffset = invOffset >= myIndex ? cumSum.getInt(invOffset + 1) - mySize : cumSum.getInt(invOffset);
                slotTarget = (slotOffset + slot) % viewSize;
                slotIndex = slotTarget >= myCumSum ? slotTarget + mySize : slotTarget;
                break;

            case OTHER_OUTPUTS:

                invOffset = (int) Math.floorMod(offset, table.size() - 2);
                slotOffset = invOffset + 1 >= myIndex ? cumSum.getInt(invOffset + 2) - inputSize - mySize
                        : cumSum.getInt(invOffset + 1) - inputSize;
                slotTarget = (slotOffset + slot) % viewSize;
                slotIndex = slotTarget + inputSize >= myCumSum ? slotTarget + inputSize + mySize
                        : slotTarget + inputSize;
                break;

            case INPUT:
                slotIndex = slot;
                break;

            default:
        }

        if (slotIndex == -1) {
            return false;
        }

        int tunnelIndex = slotToTunnel(slotIndex);
        T viewed = table.get(tunnelIndex);

        out.provider = viewed;
        out.size = cumSum.getInt(tunnelIndex + 1) - cumSum.getInt(tunnelIndex);
        out.slot = slotIndex - cumSum.getInt(tunnelIndex);
        if (viewer == input) {
            out.canExtract = policy.inputSide().extract() == P2PRoute.OUTPUTS;
            out.canInsert = policy.inputSide().insert() == P2PRoute.OUTPUTS;
        } else {
            if (viewed == input) {
                out.canExtract = policy.outputSide().extract() != P2PRoute.NONE;
                out.canInsert = policy.outputSide().insert() != P2PRoute.NONE;
            } else {
                out.canExtract = policy.outputSide().extract() == P2PRoute.PEERS;
                out.canInsert = policy.outputSide().insert() == P2PRoute.PEERS;
            }
        }
        return true;
    }

    private ViewType getViewType(T member, boolean hasSlots) {
        if (member == input) {
            return policy.inputReachesOutputs() ? ViewType.OUTPUTS : ViewType.NONE;
        }
        if (policy.outputReachesPeers()) {
            int inputSize = cumSum.getInt(1);
            if (inputSize > 0) {
                return hasSlots ? ViewType.OTHERS : ViewType.ALL;
            }
            return hasSlots ? ViewType.OTHER_OUTPUTS : ViewType.OUTPUTS;
        }
        return policy.outputReachesInput() ? ViewType.INPUT : ViewType.NONE;
    }

    private int slotToTunnel(int slot) {
        int[] arr = cumSum.elements();
        int low = 0;
        int high = cumSum.size();

        while (low < high) {
            int mid = low + (high - low) / 2;
            if (arr[mid] <= slot) {
                low = mid + 1;
            } else {
                high = mid;
            }
        }
        return low - 1;
    }

    private int getSnapshotTargetSize(final ViewInfo view) {
        final int index = view.tableIndex;
        if (index == -1) {
            return 0;
        }
        return cumSum.getInt(index + 1) - cumSum.getInt(index);
    }
}
