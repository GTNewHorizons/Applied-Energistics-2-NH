package appeng.util.p2p;

public interface IRoutedTunnel {

    int getTargetInventorySize();

    int getTargetInventoryStackLimit();

    void setViewSize(int size);
}
