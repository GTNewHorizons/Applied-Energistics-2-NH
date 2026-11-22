package appeng.util.p2p;

public interface IInventoryRoutedMember {

    int getTargetInventorySize();

    /*
     * Should return Integer.MAX_VALUE if empty
     */
    int getTargetInventoryStackLimit();

    void setViewSize(int size);
}
