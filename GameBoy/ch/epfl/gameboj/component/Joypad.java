package ch.epfl.gameboj.component;

import java.util.Objects;

import ch.epfl.gameboj.AddressMap;
import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.bits.Bits;
import ch.epfl.gameboj.component.cpu.Alu;
import ch.epfl.gameboj.component.cpu.Cpu;
import ch.epfl.gameboj.component.cpu.Cpu.Interrupt;

public final class Joypad implements Component {
	private Cpu cpu;
	private int p1;
	private int pressed;

	public enum Key {
		RIGHT, LEFT, UP, DOWN, A, B, SELECT, START
	}

	/**
	 * constructs a Joypad.
	 * 
	 * @param cpu
	 *            of the GameBoy
	 */
	public Joypad(Cpu cpu) {

		Objects.requireNonNull(cpu);
		this.cpu = cpu;
		p1 = 0;
		pressed = 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ch.epfl.gameboj.component.Component#read(int)
	 */
	@Override
	public int read(int address) throws IllegalArgumentException {
		Preconditions.checkBits16(address);
		if (address == AddressMap.REG_P1)
			return Bits.complement8(p1);
		return Component.NO_DATA;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ch.epfl.gameboj.component.Component#write(int, int)
	 */
	@Override
	public void write(int address, int data) throws IllegalArgumentException {
		Preconditions.checkBits16(address);
		Preconditions.checkBits8(data);
		if (address == AddressMap.REG_P1)
			adjustP1((Bits.complement8(data) & 0b00110000) | (p1 & 0b11001111));

	}

	public void keyPressed(Key b) {
		pressed = Bits.set(pressed, b.ordinal(), true);
	}

	public void keyReleased(Key b) {
		pressed = Bits.set(pressed, b.ordinal(), false);
	}

	
	
	
	private void adjustP1(int data) {
		Preconditions.checkBits8(data);
		int msbP1Shifted = Alu.Nshift(Bits.extract(p1, 4, 2), 4);
		int msbPressed = Bits.extract(pressed, 4, 4);
		int lsbPressed = Bits.extract(pressed, 0, 4);
        p1 = data;
		int pPrime = p1;
		switch (Bits.extract(p1, 4, 2)) {
		case 0b00: {
			p1 = msbP1Shifted;
		}
			break;
		case 0b01: {
			p1 = msbP1Shifted | lsbPressed;
		}
			break;
		case 0b10: {
			p1 = msbP1Shifted | msbPressed;
		}
			break;
		case 0b11: {
			p1 = msbP1Shifted | msbPressed | lsbPressed;
		}
			break;
		}
		if (p1 != pPrime)
			cpu.requestInterrupt(Interrupt.JOYPAD);
	}

}
