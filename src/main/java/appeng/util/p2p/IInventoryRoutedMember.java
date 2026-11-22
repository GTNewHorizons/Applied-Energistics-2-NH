package appeng.util.p2p;

public interface IInventoryRoutedMember {

    int getTargetInventorySize();

    int getTargetInventoryStackLimit();

    void setViewSize(int size);
}
