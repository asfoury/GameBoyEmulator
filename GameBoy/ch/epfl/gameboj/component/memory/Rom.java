package ch.epfl.gameboj.component.memory;

import java.util.Arrays;
import java.util.Objects;

/**
 * This Class represents a read-only memory.
 * it's immutable.
 *
 * @author Karim Sabaa (269647)
 * @author Mohamed Elasfoury (289473)
 */

public final class Rom {

	private final byte[] data;

	/**
	 * Constructs a read-only memory of the same size and content as the given data table.
	 * 
	 * @param data
	 *            the table of data.
	 * @throws NullPointerException
	 *             if data is null.
	 */
	public Rom(byte[] data) throws NullPointerException {
		
		Objects.requireNonNull(data);

		this.data = Arrays.copyOf(data, data.length); // this Class is immutable.

	}

	/**
	 * Returns the size of the data table passed to the constructor.
	 * 
	 * @return size of the memory
	 */

	public int size() {

		return data.length;
	}

	/**
	 * Returns the octet at the given index.
	 * 
	 *
	 * @param index
	 *            of the wanted octet.
	 * @throws IndexOutOfBoundsException
	 *             if the passed index is not valid
	 *             ( negative or bigger than the size of the memory {@link #size()}).
	 * @return octet 
	 *             (value between 0 and 0xFF) at given index.
	 */

	public int read(int index) throws IndexOutOfBoundsException {
		if (!(index >=0 && index < data.length)) {
			throw new IndexOutOfBoundsException();
		}

		return Byte.toUnsignedInt(data[index]);
	}

}
