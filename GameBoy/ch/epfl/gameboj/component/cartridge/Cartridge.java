package ch.epfl.gameboj.component.cartridge;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.component.Component;
import ch.epfl.gameboj.component.memory.Rom;

/**
 * Represents a Cartrdige. It's not connected to the Bus.
 * 
 *
 * @author Karim Sabaa (269647)
 * @author Mohamed Elasfoury (289473)
 */
public final class Cartridge implements Component {
	private Component romController;
	private static final int type = 0x147;
	private static final int RAM_SIZE[] = new int[] { 0, 2048, 8192, 32768 };

	private Cartridge(Component romController) {
		this.romController = romController;
	}

	/**
	 * Constructs a Cartridge. It's Rom contains the octets of the given file.
	 * 
	 * @param romFile
	 * @throws IllegalArgumentException
	 *             if the file doesn't contain a zero at the position
	 * @throws IOException
	 *             in case of input/output problem, including if the fileName
	 *             doesn't exist.
	 */
	public static Cartridge ofFile(File romFile) throws IOException  {

		byte[] data = Files.readAllBytes(romFile.toPath());
	
		if (!(data[type] >= 0 && data[type] < 4))
			throw new IllegalArgumentException();

		return new Cartridge((data[type] == 0) ? new MBC0(new Rom(data))
				: new MBC1(new Rom(data), RAM_SIZE[data[0x149]]));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ch.epfl.gameboj.component.Component#read(int)
	 */
	@Override
	public int read(int address) throws IndexOutOfBoundsException {
		Preconditions.checkBits16(address);
		return romController.read(address);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ch.epfl.gameboj.component.Component#write(int, int)
	 */
	@Override
	public void write(int address, int data) {
		Preconditions.checkBits8(data);
		Preconditions.checkBits16(address);
		romController.write(address, data);
	}

}
