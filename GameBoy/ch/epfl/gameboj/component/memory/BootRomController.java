package ch.epfl.gameboj.component.memory;

import ch.epfl.gameboj.AddressMap;
import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.component.Component;
import ch.epfl.gameboj.component.cartridge.Cartridge;

/**
 * this class represents the controller of the read-only memory of booting.
 * Represented as a component attached to the Bus. It controls the access to the
 * cartridge in order to intercept the readings done in the rang of 0 to 0xFF as
 * long as the booting memory is still not deactivated. 
 *
 * @author Karim Sabaa (269647)
 * @author Mohamed Elasfoury (289473)
 */
public final class BootRomController implements Component {

	private Cartridge cartridge;
	private boolean deactivated;

	/**
	 * Constructs a controller to the read-only memory of booting. The given
	 * cartridge is attached to the controller.
	 * 
	 * @param cartridge
	 * @throws NullPointerException 
	 *             if the given cartridge is Null.
	 */
	public BootRomController(Cartridge cartridge) throws NullPointerException {
		if (cartridge == null)
			throw new NullPointerException("The Given Cartridge is null");
		this.cartridge = cartridge;
		this.deactivated = false;
	}
	/**
	 * If the boot memory is deactivated, it does as defined 
	 * in Component({@link Component#read(int)}) and reads from cartridge
	 * or else, if the address is in the range of the Boot ROM: 
	 * it intercept it and read it from the boot memory.
	 * if it isn't in the range,  it will pass the read command to the cartridge.
	 * @param address
	 * @return octet from cartridge or boot ROM (unsigned)
	 * @throws IllegalArgumenteException if the Address isn't 16 bits.
	 */
	@Override
	public int read(int address) throws IllegalArgumentException {
         Preconditions.checkBits16(address);
		if (address >= AddressMap.BOOT_ROM_START && address < AddressMap.BOOT_ROM_END)

			return (deactivated) ? cartridge.read(address) : Byte.toUnsignedInt(BootRom.DATA[address- AddressMap.BOOT_ROM_START]);

		return cartridge.read(address);
	}
   /**
    * if address isn't equal to AddressMap.REG_BOOT_ROM_DISABLE,
    *  it does as defined in ({@link Component#write(int, int)} and writes in cartridge.
    *  But if address does equal to AddressMap.REG_BOOT_ROM_DISABLE, it deactivates the boot memory.
    * @param address, data
    * @throws IllegalArgumentException if the address isn't  16 bits or data isn't 8 bits. 
    */
	@Override
	public void write(int address, int data) throws IllegalArgumentException {
		Preconditions.checkBits16(address);
		Preconditions.checkBits8(data);
		if (address == AddressMap.REG_BOOT_ROM_DISABLE) {
			deactivated = true;
		} else {
			cartridge.write(address, data);
		}

	}

}
