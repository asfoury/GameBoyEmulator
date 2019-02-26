package ch.epfl.gameboj;

/**
 * Interface Register
 *
 * @author Asfoury.(289473)
 * @autor Karim.(269647)
 * 
 */

public interface Register {
    /**
     * @return the index of the an enum type
     */
    public int ordinal();

    /**
     * @return the same value of the ordinal method
     */
    public default int index() {
        return this.ordinal();
    }

}
