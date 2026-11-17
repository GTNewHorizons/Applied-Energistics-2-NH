package appeng.util.p2p;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import appeng.util.target.InventoryTarget;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;

public class ArrayInventoryRouter implements IInventoryRouter {

    private final IRoutedTunnel input;
    private final P2PTransportPolicy policy;
    private final Reference2ObjectMap<IRoutedTunnel, InventoryTarget> members = new Reference2ObjectOpenHashMap<>();

    private final ArrayList<IRoutedTunnel> table = new ArrayList<>(); // input always come first, even if empty
    private final IntArrayList cumSum = new IntArrayList();
    private final Reference2IntMap<IRoutedTunnel> memberIndex = new Reference2IntOpenHashMap<>();
    private final Reference2IntMap<IRoutedTunnel> viewSizes = new Reference2IntOpenHashMap<>();

    private int min1StackLimit = Integer.MAX_VALUE;
    private int min2StackLimit = Integer.MAX_VALUE;
    private int inputStackLimit = Integer.MAX_VALUE;
    private IRoutedTunnel minMember;
    private boolean stackLimitChanged;

    public ArrayInventoryRouter(IRoutedTunnel input, P2PTransportPolicy policy) {
        this.input = input;
        this.policy = policy;
        this.members.put(input, null);
    }

    @Override
    public void addMember(IRoutedTunnel output) {
        members.putIfAbsent(output, null);
        stackLimitChanged = true;
    }

    @Override
    public void removeMember(IRoutedTunnel output) {
        if (output == input) {
            return;
        }
        members.remove(output);
        stackLimitChanged = true;
    }

    @Override
    public void clearMembers() {
        InventoryTarget inputTarget = members.get(input);
        members.clear();
        members.put(input, inputTarget);
        stackLimitChanged = true;
    }

    @Override
    public boolean setTarget(IRoutedTunnel member, InventoryTarget target) {
        if (!members.containsKey(member)) {
            return false;
        }
        InventoryTarget oldTarget = members.put(member, target);
        stackLimitChanged = true;
        return getSize(oldTarget) != getSize(target);
    }

    @Override
    public void rebuildLayout() {
        table.clear();
        cumSum.clear();
        memberIndex.clear();

        table.add(input);
        cumSum.add(0);
        memberIndex.put(input, 0);
        int sum = getSize(members.get(input));
        cumSum.add(sum);

        for (Map.Entry<IRoutedTunnel, InventoryTarget> entry : members.entrySet()) {
            IRoutedTunnel member = entry.getKey();
            InventoryTarget target = entry.getValue();
            int mySize = getSize(target);
            if (member == input || mySize == 0) {
                continue;
            }
            memberIndex.put(member, table.size());
            table.add(member);
            cumSum.add(sum += mySize);
        }

        Reference2IntMap<IRoutedTunnel> oldViewSizes = new Reference2IntOpenHashMap<>(viewSizes);
        viewSizes.clear();
        List<IRoutedTunnel> changed = new ArrayList<>();
        for (Map.Entry<IRoutedTunnel, InventoryTarget> entry : members.entrySet()) {
            IRoutedTunnel member = entry.getKey();
            InventoryTarget target = entry.getValue();
            int mySize = getSize(target);
            int newViewSize = 0;
            if (member == input) {
                if (policy.inputReachesOutputs()) {
                    newViewSize = cumSum.getInt(members.size()) - mySize;
                }
            } else {
                if (policy.outputReachesPeers()) {
                    newViewSize += cumSum.getInt(members.size()) - mySize;
                } else if (policy.outputReachesInput()) {
                    newViewSize += cumSum.getInt(1);
                }
            }
            viewSizes.put(member, newViewSize);
            if (newViewSize != oldViewSizes.getInt(member)) {
                changed.add(member);
            }
        }

        for (final Reference2IntMap.Entry<IRoutedTunnel> entry : oldViewSizes.reference2IntEntrySet()) {
            if (!members.containsKey(entry.getKey()) && entry.getIntValue() != 0) {
                changed.add(entry.getKey());
            }
        }

        for (IRoutedTunnel member : changed) {
            member.onViewSizeChange();
        }
    }

    @Override
    public int getViewSize(IRoutedTunnel viewer) {
        return viewSizes.getInt(viewer);
    }

    @Override
    public int getViewStackLimit(IRoutedTunnel viewer) {
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
        for (Entry<IRoutedTunnel, InventoryTarget> entry : members.entrySet()) {
            IRoutedTunnel member = entry.getKey();
            InventoryTarget target = entry.getValue();

            if (target == null || !target.isValid() || target.size() == 0) {
                continue;
            }

            final int limit = target.inv.getInventoryStackLimit();
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
    public boolean resolve(IRoutedTunnel viewer, int slot, long offset, SlotView out) {
        if (!members.containsKey(viewer)) {
            return false;
        }
        int viewSize = viewSizes.getInt(viewer);
        if (slot < 0 || slot >= viewSize) {
            return false;
        }

        int slotIndex = -1;
        int myIndex = memberIndex.getInt(viewer);
        int inputSize = cumSum.getInt(1);
        int myCumSum = cumSum.getInt(myIndex);
        int mySize = cumSum.getInt(myIndex + 1) - myCumSum;

        if (viewsAll(viewer)) {

            int invOffset = (int) Math.floorMod(offset, table.size());
            int slotOffset = cumSum.getInt(invOffset);
            slotIndex = (slotOffset + slot) % viewSize;

        } else if (viewsOutputs(viewer)) {

            int invOffset = (int) Math.floorMod(offset, table.size() - 1);
            int slotOffset = cumSum.getInt(invOffset + 1) - inputSize;
            int slotTarget = (slotOffset + slot) % viewSize;
            slotIndex = slotTarget + inputSize;

        } else if (viewsOthers(viewer)) {

            int invOffset = (int) Math.floorMod(offset, table.size() - 1);
            int slotOffset = invOffset >= myIndex ? cumSum.getInt(invOffset + 1) - mySize : cumSum.getInt(invOffset);
            int slotTarget = (slotOffset + slot) % viewSize;
            slotIndex = slotTarget >= myCumSum ? slotTarget + mySize : slotTarget;

        } else if (viewsOtherOutputs(viewer)) {

            int invOffset = (int) Math.floorMod(offset, table.size() - 2);
            int slotOffset = invOffset + 1 >= myIndex ? cumSum.getInt(invOffset + 2) - inputSize - mySize
                    : cumSum.getInt(invOffset + 1) - inputSize;
            int slotTarget = (slotOffset + slot) % viewSize;
            slotIndex = slotTarget + inputSize >= myCumSum ? slotTarget + inputSize + mySize : slotTarget + inputSize;

        } else if (viewsInput(viewer)) {
            slotIndex = slot;
        }

        if (slotIndex == -1) {
            return false;
        }
        int tunnelIndex = slotToTunnel(slotIndex);
        IRoutedTunnel viewed = table.get(tunnelIndex);
        InventoryTarget target = members.get(viewed);
        if (target == null || target.size() != getSnapshotTargetSize(viewed)) {
            return false;
        }
        if (!target.isValid()) {
            viewed.onInvalidTarget();
            return false;
        }
        out.inv = target.inv;
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

    private boolean viewsAll(IRoutedTunnel viewer) {
        int inputSize = cumSum.getInt(1);
        return viewer != input && inputSize > 0 && policy.outputReachesPeers() && !memberIndex.containsKey(viewer);
    }

    private boolean viewsOthers(IRoutedTunnel viewer) {
        int inputSize = cumSum.getInt(1);
        return viewer != input && inputSize > 0 && policy.outputReachesPeers() && memberIndex.containsKey(viewer);
    }

    private boolean viewsOutputs(IRoutedTunnel viewer) {
        if (viewer == input) {
            return policy.inputReachesOutputs();
        }
        int inputSize = cumSum.getInt(1);
        return inputSize == 0 && policy.outputReachesPeers() && !memberIndex.containsKey(viewer);
    }

    private boolean viewsOtherOutputs(IRoutedTunnel viewer) {
        int inputSize = cumSum.getInt(1);
        return viewer != input && inputSize == 0 && policy.outputReachesPeers() && memberIndex.containsKey(viewer);
    }

    private boolean viewsInput(IRoutedTunnel viewer) {
        return viewer != input && policy.outputReachesInput();
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

    private static int getSize(final InventoryTarget target) {
        return target == null ? 0 : target.size();
    }

    private int getSnapshotTargetSize(final IRoutedTunnel t) {
        final int index = memberIndex.getInt(t);
        return cumSum.getInt(index + 1) - cumSum.getInt(index);
    }
}
