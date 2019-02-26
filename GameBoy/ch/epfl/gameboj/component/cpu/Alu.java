package ch.epfl.gameboj.component.cpu;

import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.bits.Bit;
import ch.epfl.gameboj.bits.Bits;

public final class Alu {
    /**
     * Alu
     * 
     * @author Asfoury.(289473)
     * @autor Karim.(269647)
     * 
     */

    public enum Flag implements Bit {
        UNUSED_3, UNUSED_2, UNUSED_1, UNUSED_0, C, H, N, Z
    }

    public enum RotDir{
        RIGHT, LEFT
    }
    
    private final static int flagsOrValueSize = 8; // this represents the size of the value or the flag
    
    private Alu() {}

    /**
     * Returns 8 bits, first 4 are unused, second 4 composed of Flags ( Z, N, H,
     * C)
     * 
     *
     * @param the
     *            Flags values
     * @return Integer (Flags)
     */
    public static int maskZNHC(boolean Z, boolean N, boolean H, boolean C) {
        int result = 0;
        if (Z)
            result = result | Flag.Z.mask();
        if (N)
            result = result | Flag.N.mask();
        if (H)
            result = result | Flag.H.mask();
        if (C)
            result = result | Flag.C.mask();

        return result;
    }

    /**
     * Returns returns the value inside of the packet Value/Flags
     * 
     *
     * @param packet
     *            Value/Flags
     * @return Integer (Value)
     */
    public static int unpackValue(int valueFlag) {
        
        return valueFlag >>> flagsOrValueSize;
    }

    /**
     * Returns returns the Flags inside of the packet Value/Flags
     * 
     *
     * @param packet
     *            Value/Flags
     * @return Integer (Flags)
     */
    public static int unpackFlags(int valueFlags) {
        valueFlags = valueFlags << (Integer.SIZE - flagsOrValueSize);
        return valueFlags >>> (Integer.SIZE - flagsOrValueSize);
    }

    /**
     * adds two integers with or without an initial Carry
     * 
     *
     * @param the
     *            two integers, and the boolean value to know whether or not add
     *            an initial Carry
     * @return the Value of the addition + flags
     */

    public static int add(int l, int r, boolean c0) {
        Preconditions.checkBits8(l);
        Preconditions.checkBits8(r);
        int FirstFourBitOfL = Bits.clip(4, l);
        int FirstFourBitOfR = Bits.clip(4, r);
        int x = Bits.clip(flagsOrValueSize, l + r + 1);
        int y = Bits.clip(flagsOrValueSize, l + r);
        if (c0) {
            return (x << flagsOrValueSize) | maskZNHC((Bits.clip(flagsOrValueSize, l + r + 1)) == 0, false,
                    (FirstFourBitOfL + FirstFourBitOfR + 1 > 0xF),
                    (l + r + 1) > 0xFF);
            // Bits.extract(result, 0, 16);
        } else {
            return (y << flagsOrValueSize) | maskZNHC((Bits.clip(flagsOrValueSize, l + r)) == 0, false,
                    (FirstFourBitOfL + FirstFourBitOfR > 0xF), (l + r) > 0xFF);

            // return Bits.extract(result, 0, 16);
        }

    }

    /**
     * does the same thing as the previous method but with a boolean value
     * always false
     * 
     *
     * @param the
     *            two integers
     * @return the Value of the addition + flags
     */
    public static int add(int l, int r) {
        Preconditions.checkBits8(l);
        Preconditions.checkBits8(r);
        return add(l, r, false);
    }

    /**
     * does the addition of two 16 bits values
     * 
     *
     * @param the
     *            two integers
     * @return the Value of the addition + flags ( of the first 8 bits addition)
     */
    public static int add16L(int l, int r) {
        Preconditions.checkBits16(l);
        Preconditions.checkBits16(r);
        int firstAdd = add(Bits.clip(flagsOrValueSize, l), Bits.clip(flagsOrValueSize, r));
        int secondAdd = add(Bits.extract(l, flagsOrValueSize, flagsOrValueSize), Bits.extract(r, flagsOrValueSize, flagsOrValueSize),
                Bits.test(firstAdd, 4));
        return (unpackValue(secondAdd) << 16)
                | Bits.set(firstAdd, Flag.Z.index(), false);
    }

    /**
     * does the addition of two 16 bits values
     * 
     *
     * @param the
     *            two integers
     * @return the Value of the addition + flags ( of the second 8 bits
     *         addition)
     */
    public static int add16H(int l, int r) {

        Preconditions.checkBits16(l);
        Preconditions.checkBits16(r);
        int firstAdd = add(Bits.clip(flagsOrValueSize, l), Bits.clip(flagsOrValueSize, r));
        int secondAdd = add(Bits.extract(l, flagsOrValueSize, flagsOrValueSize), Bits.extract(r, flagsOrValueSize, flagsOrValueSize),
                Bits.test(firstAdd, 4));
        int flag = Bits.set(unpackFlags(secondAdd), Flag.Z.index(), false);

        return (unpackValue(secondAdd) << 16 | unpackValue(firstAdd) << flagsOrValueSize
                | flag);
    }

    /**
     * Returns the first sub method but with false for b0
     * 
     *
     * @param two
     *            numbers to be substracted l and r
     * @return l - r with the flags
     */

    public static int sub(int l, int r) {

        return sub(l, r, false);

    }

    /**
     * Returns the substraction of r from l with the correct flags
     * 
     *
     * @param two
     *            numbers to be substracted l and r and a boolean true if there
     *            is an initial borrow
     * @return l - r with the flags
     */

    public static int sub(int l, int r, boolean b0) {

        Preconditions.checkBits8(l);
        Preconditions.checkBits8(r);

        if (b0) {
            int n = Bits.clip(flagsOrValueSize, l - r - 1);

            return n << flagsOrValueSize | maskZNHC(n == 0, true,
                    (Bits.clip(4, l)) - Bits.clip(4, r) - 1 < 0, l - r - 1 < 0);

        } else {
            int n = Bits.clip(flagsOrValueSize, l - r);
            return packValueZNHC(n,n == 0, true,(Bits.clip(4, l)) < (Bits.clip(4, r)), l < r);
        }
    }

    public static int bcdAdjust(int v, boolean n, boolean h, boolean c) {
        Preconditions.checkBits8(v);
        boolean fixL = h || (!n && (Bits.clip(4, v) > 9));
        boolean fixH = c || (!n && (v > 0x99));
        int intfixL = (fixL) ? 1 : 0;
        int intfixH = (fixH) ? 1 : 0;
        int fix = (0x60 * intfixH) + (0x06 * intfixL);
        int Va = (n) ? v - fix : v + fix;
        return Bits.clip(flagsOrValueSize, Va) << flagsOrValueSize
                | maskZNHC(Bits.clip(flagsOrValueSize, Va) == 0, n, false, fixH);
    }

    public static int and(int l, int r) {
        Preconditions.checkBits8(l);
        Preconditions.checkBits8(r);
        return (l & r) << flagsOrValueSize | maskZNHC((l & r) == 0, false, true, false);
    }

    /**
     * Returns the logical operation or of l and r with the flags
     * 
     *
     * @param two
     *            numbers l and r
     * @return logical or of l and r
     */

    public static int or(int l, int r) {
        Preconditions.checkBits8(l);
        Preconditions.checkBits8(r);
        return (l | r) << flagsOrValueSize | maskZNHC((l | r) == 0, false, false, false);
    }

    /**
     * Returns the logical operation xor of l and r with the flags
     * 
     *
     * @param two
     *            numbers l and r
     * @return logical xor of l and r
     */

    public static int xor(int l, int r) {
        Preconditions.checkBits8(l);
        Preconditions.checkBits8(r);

        return (l ^ r) << flagsOrValueSize | maskZNHC((l ^ r) == 0, false, false, false);
    }
     
    public static int Nshift(int v, int n) {
    
    	if(n>=0) {
    		
    		for(int i =0 ; i< n; ++i) shiftLeft(v);
    		return Alu.unpackValue(v);
    	}
    	else { 
    		for(int i =0 ; i< n; ++i) shiftRightL(v);
    		return Alu.unpackValue(v);
          }
    }
    /**
     * It's a one bit shift to the left
     * 
     *
     * @param values
     *            of the flags that was given during the operation (Addition ,
     *            subtraction)
     * @return The value shifted, the Flag C corresponds to the neglected Bit
     *         during the shift.
     */
    public static int shiftLeft(int v) {
        Preconditions.checkBits8(v);
        int flags = maskZNHC(((Bits.clip(7, v)) << 1 == 0), false, false,
                Bits.test(v, 7));
        return (((Bits.clip(7, v)) << 9) | flags);
    }

    /**
     * It's a one bit Arithmetic shift to the left
     * 
     *
     * @param values
     *            of the flags that was given during the operation (Addition ,
     *            subtraction)
     * @return The value shifted, the Flag C corresponds to the neglected Bit
     *         during the shift.
     */
    public static int shiftRightA(int v) {
        Preconditions.checkBits8(v);
        int tempo = Bits.signExtend8(v);
        tempo = tempo >>> 1;
        tempo = Bits.clip(flagsOrValueSize, tempo);
        int flags = maskZNHC(tempo == 0, false, false, Bits.test(v, 0));

        return (tempo << flagsOrValueSize) | flags;

    }

    /**
     * It's a one bit Logic shift to the left
     * 
     *
     * @param values
     *            of the flags that was given during the operation (Addition ,
     *            subtraction)
     * @return The value shifted, the Flag C corresponds to the neglected Bit
     *         during the shift.
     */
    public static int shiftRightL(int v) {
        Preconditions.checkBits8(v);
        int tempo = v >>> 1;

        int flags = maskZNHC(tempo == 0, false, false, Bits.test(v, 0));
        return (tempo << flagsOrValueSize) | flags;
    }

    /**
     * Rotates the passed bits by a distance of 1 to the left if passed value is
     * Left and right if the passed value is right
     * 
     *
     * @param the
     *            direction and the bits
     * @return the rotated bits with the corresponding flags.
     */

    public static int rotate(RotDir d, int v) {
        Preconditions.checkBits8(v);
        Preconditions.checkArgument(d == RotDir.LEFT | d == RotDir.RIGHT);
        boolean cValue;
        if (d == RotDir.LEFT) {
            v = Bits.rotate(flagsOrValueSize, v, 1);
            cValue = Bits.test(v, 0);
            return (v << flagsOrValueSize) | maskZNHC(v == 0, false, false, cValue);
        } else {
            v = Bits.rotate(flagsOrValueSize, v, -1);
            cValue = Bits.test(v, 7);
            return (v << flagsOrValueSize) | maskZNHC(v == 0, false, false, cValue);
        }

    }

    /**
     * Rotates the passed bits by a distance of 1 to the left if passed value is
     * Left and right if the passed value is right but adds a 1 in index 8 if c
     * is true and a 0 if passed c is false
     * 
     *
     * @param the
     *            direction and the bits
     * @return the rotated bits with the corresponding flags.
     */

    public static int rotate(RotDir d, int v, boolean c) {
        Preconditions.checkBits8(v);
        Preconditions.checkArgument(d == RotDir.LEFT | d == RotDir.RIGHT);

        boolean cValue;

        if (c) {
            int carry = 1 << flagsOrValueSize;
            v = v | carry;
        }

        if (d == RotDir.LEFT) {
            v = Bits.rotate(9, v, 1);

            cValue = Bits.test(v, flagsOrValueSize);
            return ((v & 0b11111111) << flagsOrValueSize)
                    | maskZNHC((v & 0b11111111) == 0, false, false, cValue);
        } else {
            v = Bits.rotate(9, v, -1);
            cValue = Bits.test(v, flagsOrValueSize);
            return ((v & 0b11111111) << flagsOrValueSize)
                    | maskZNHC((v & 0b11111111) == 0, false, false, cValue);
        }
    }

    /**
     * Swap the first and second 4 bits of the given 8 bit integer
     * 
     * @param 8
     *            bit integer
     * @return the Swapped value and the flags Z000
     */

    public static int swap(int v) {
        Preconditions.checkBits8(v);
        int firstFour = Bits.clip(4, v);
        int secondFour = Bits.extract(v, 4, 4);
        int valueSwap = (firstFour << 4) + secondFour;
        int flag = maskZNHC(valueSwap == 0, false, false, false);
        return (valueSwap << flagsOrValueSize) + flag;
    }

    /**
     * \tests if the value of the given Integer is equal to one at a given index
     *
     * @param th
     *            integer to be tested
     * @param the
     *            index
     * @return the value zero and flags Z010
     * @throws IndexOutOfBoundsException
     *             if the index isn't between 0 and 7
     */
    public static int testBit(int v, int bitIndex)
            throws IndexOutOfBoundsException {
        Preconditions.checkBits8(v);
        if (bitIndex > 7 || bitIndex < 0) {
            throw new IndexOutOfBoundsException();
        }
        boolean z = Bits.test(v, bitIndex);

        return maskZNHC(!z, false, true, false);
    }
    
    
    private static int packValueZNHC(int v, boolean z, boolean n, boolean h, boolean c) {

        return maskZNHC(z, n, h, c) | (v << 8);
    }


}
