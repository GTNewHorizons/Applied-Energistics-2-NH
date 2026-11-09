package appeng.api.config;

/**
 * Which way a stack moves relative to the ME network.
 */
public enum TransferDirection {

    /** Into the network. */
    IMPORT,

    /** Out of the network. */
    EXPORT,

    /** Either way. */
    BOTH
}
