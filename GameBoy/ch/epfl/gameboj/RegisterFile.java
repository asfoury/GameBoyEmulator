package ch.epfl.gameboj;

import ch.epfl.gameboj.bits.Bit;
import ch.epfl.gameboj.bits.Bits;

public final class RegisterFile<E extends Register> {
    /**
     * RegisterFile class. the wires between components
     * 
     * @author Asfoury.(289473)
     * @autor Karim.(269647)
     * 
     * 
     */
    private byte[] banc;

    /**
     * RegisterFile constructor
     *
     * 
     * 
     * 
     * @param an
     *            Enum(collection of registers).
     * 
     * 
     */
    public RegisterFile(E[] allRegs) {
        banc = new byte[allRegs.length];

    }

    /**
     * gets the value of the passed register
     *
     * @param the
     *            register.
     * @return the value of the given register
     * 
     *         *
     */

    public int get(E reg) {
        return Bits.clip(8, banc[reg.index()]);
    }

    /**
     * changes the 8 bit value of the given register
     *
     * @param the
     *            register and the new value.
     * 
     *            *
     */
    public void set(E reg, int newValue) {
        Preconditions.checkBits8(newValue);
        banc[reg.index()] = (byte) newValue;

    }

    /**
     * Tests if the given bit of the register is equal to 1
     *
     * @param the
     *            register and the bit to be tested.
     * @return true if the given bit is equal to 1.
     * 
     *         *
     */

    public boolean testBit(E reg, Bit b) {
        return Bits.test(banc[reg.index()], b);
    }

    /**
     * changes the bit of the given register to 1 if newValue is true and to 0
     * if newValue is False
     *
     * @param the
     *            register and the bit to be changed and newValue.
     * 
     * 
     *            *
     */
    public void setBit(E reg, Bit bit, boolean newValue) {
        Bits.set(banc[reg.index()], bit.index(), newValue);
    }

}
