package ch.epfl.gameboj.bits;

/**
 * Interface Bit.
 *
 * @author Asfoury.(289473)
 * @autor Karim.(269647)
 * 
 */
public interface Bit {
    /**
     * @return the index of the an enum type *
     */
    public int ordinal();

    /**
     * @return the same value of the method ordinal *
     */
    public default int index() {
        return ordinal();
    }

    /**
     * @return 8 bits that have a 1 at the index of the bit *
     */
    public default int mask() {
        return Bits.mask(this.index());

    }

}
