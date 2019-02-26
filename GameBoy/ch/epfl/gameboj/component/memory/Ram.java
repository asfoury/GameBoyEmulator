package ch.epfl.gameboj.component.memory;

import ch.epfl.gameboj.Preconditions;

public final class Ram {

    /**
     * Ram class
     *
     * @author Asfoury.(289473)
     * @autor Karim.(269647)
     * 
     */
    private final byte[] data;
    private final int size;

    public Ram(int size) {
        Preconditions.checkArgument(size >= 0);
        this.size = size;
        this.data = new byte[size];
    }

    /**
     * Returns the size of the memory passed to the constructor
     * 
     * 
     *
     * @return size of the data array
     */
    public int size() {
        return this.size;
    }

    /**
     * Returns the byte at the given index
     * 
     *
     * @param int
     *            index of the byte
     * @throws IndexOutOfBoundsException
     *             if the passed index is not valid(negative or bigger than the
     *             size of the array)
     * @return byte at passed index
     */

    public int read(int index) throws IndexOutOfBoundsException {

        if (index < 0 || index > data.length) {
            throw new IndexOutOfBoundsException();
        }

        return Byte.toUnsignedInt(data[index]);

    }

    /**
     * Changes the value of a byte at a given index
     * 
     *
     * @param int
     *            index of the byte
     * @param int
     *            value of the byte
     * @throws IndexOutOfBoundsException
     *             if the passed index is not valid(negative or bigger than the
     *             size of the array)
     * @throws IllegalArgument
     *             if the passed value isn't between 0 and 0XFF
     * 
     */
    public void write(int index, int value) throws IndexOutOfBoundsException {
        Preconditions.checkBits8(value);

        if (index < 0 || index > data.length) {
            throw new IndexOutOfBoundsException();
        }

        data[index] = (byte) value;
    }

}
