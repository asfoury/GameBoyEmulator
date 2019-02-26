package ch.epfl.gameboj.component.cartridge;

import java.util.Objects;

import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.component.Component;
import ch.epfl.gameboj.component.memory.Rom;

/**
 * Represents a memory bank controller type 0
 * i.e It has only a ROM composed of 32768 octets.
 * Although it implements component, it's not attached
 *  to the Bus. This choice is a matter of simplicity.
 *
 * @author Karim Sabaa (269647)
 * @author Mohamed Elasfoury (289473)
 */
public final class MBC0 implements Component {

	private Rom rom;
    protected static final int sizeMemoryBankType0 = 32768;
	/**
	 * constructs a controller type 0 for the given memory.
	 * 
	 * @param Rom
	 * @throws NullPointerException
	 *             if the ROM is null
	 * @throws IllegalArgumentException
	 *             if the ROM size isn't correct
	 */
	public MBC0(Rom rom){

		Objects.requireNonNull(rom);

		Preconditions.checkArgument(rom.size() == sizeMemoryBankType0);
		this.rom = rom;

	}
	
	/**
	 * Reads the value in the ROM at a given address.
	 * 
	 * @param address
	 * 
	 * @throws IndexOutOfBoundsException
	 *             if the address isn't between 0 (include) and 0x7FFF (include).
	 * @return octet at given address in the ROM or NO_DATA.
	 */
	@Override
	public int read(int address) throws IndexOutOfBoundsException {
		Preconditions.checkBits16(address);
		if (address < rom.size())
			return rom.read(address); 

		return Component.NO_DATA;

	}

	/**
	 * It's impossible to write in a ROM. This method doesn't do anything.
	 */
	@Override
	public void write(int address, int data) {

	}

	

}