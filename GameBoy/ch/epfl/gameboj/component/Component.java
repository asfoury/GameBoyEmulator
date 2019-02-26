package ch.epfl.gameboj.component;

import ch.epfl.gameboj.Bus;

/**
 * This interface represents a component of the GameBoy connected to the Bus
 * (for example: Processor, KeyBoard etc..).
 *
 * @author Karim Sabaa (269647)
 * @author Mohamed Elasfoury (289473)
 */
public interface Component {

	public static final int NO_DATA = 0x100;

	/**
	 * Reads the octet stored at the given address if found, else returns NO_DATA.
	 * 
	 * @param address
	 *            of the wanted octet.
	 * @return octet stored at the given address (if found), or NO_DATA.
	 * @throws IllegalArgumentException
	 *             if passed address is not a 16 bits value
	 */
	public int read(int address) throws IllegalArgumentException;

	/**
	 * Stores the passed value at the passed address if component permits else does
	 * nothing
	 * 
	 *
	 * @param address
	 *            of the octet to be stored.
	 * @param data
	 *            the octet to be stored.
	 * @throws IllegalArgumentException
	 *             if the address is not a 16 bits value or if value is not a 8 bits
	 *             value
	 */
	public void write(int address, int data)  throws IllegalArgumentException;

	/**
	 * Attaches the component to the given Bus.
	 *
	 * @param Bus
	 *            where the component will be attached.
	 * 
	 */

	public default void attachTo(Bus bus) {
		bus.attach(this);
	}

}
