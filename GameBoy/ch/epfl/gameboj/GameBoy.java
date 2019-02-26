package ch.epfl.gameboj;

import ch.epfl.gameboj.component.Joypad;
import ch.epfl.gameboj.component.Timer;
import ch.epfl.gameboj.component.cartridge.Cartridge;
import ch.epfl.gameboj.component.cpu.Cpu;
import ch.epfl.gameboj.component.lcd.LcdController;
import ch.epfl.gameboj.component.memory.BootRomController;
import ch.epfl.gameboj.component.memory.Ram;
import ch.epfl.gameboj.component.memory.RamController;

/**
 * this class represents a gameBoy, it's purpose is to instantiate the different
 * components of the game and to attach it to a common Bus.
 *
 * @author Karim Sabaa (269647)
 * @author Mohamed Elasfoury (289473)
 */
public final class GameBoy {

	private Bus bus;
	private Ram ramWork;
	private Ram ramEcho;
	private RamController workRamController;
	private RamController echoRamController;
	private BootRomController bootRomController;
	private Timer timer;
	private Cpu cpu;
	private long currentCycle;
	private LcdController lcdController;
	private Joypad joypad;
	private static final long cyclePerSecond = 1 << 20;
	private static final double cyclePerNanosecond = cyclePerSecond * Math.pow(10, -9); 

	/**
	 * Constructs a GameBoy by creating the necessary components: - Bus - Cpu -
	 * working Ram and it's RamController - Echo Ram and it's RamController -
	 * BootRomController - Timer
	 * 
	 * @param cartridge
	 * @throws NullPointerException
	 *             if the Given Cartridge is null.
	 */
	public GameBoy(Cartridge cartridge) throws NullPointerException {
		if (cartridge == null)
			throw new NullPointerException("The given cartridge is null");

		bus = new Bus();

		currentCycle = 0;

		cpu = new Cpu();
		cpu.attachTo(bus);

		this.ramWork = new Ram(AddressMap.WORK_RAM_SIZE);
		workRamController = new RamController(ramWork, AddressMap.WORK_RAM_START, AddressMap.WORK_RAM_END);
		workRamController.attachTo(bus);

		this.ramEcho = new Ram(AddressMap.ECHO_RAM_SIZE);
		echoRamController = new RamController(ramEcho, AddressMap.ECHO_RAM_START, AddressMap.ECHO_RAM_END);
		echoRamController.attachTo(bus);

		bootRomController = new BootRomController(cartridge);
		bootRomController.attachTo(bus);

		timer = new Timer(cpu);
		timer.attachTo(bus);

		lcdController = new LcdController(cpu);
		lcdController.attachTo(bus);
		
		joypad = new Joypad(cpu);
		joypad.attachTo(bus);

	}

	/**
	 * Simulates the functioning of the GameBoy from the beginning(cycle = 0) to the
	 * given ( as an argument) cycle minus one.
	 * 
	 * @param cycle
	 * @throws IllegalArgumentException
	 *             if the given cycle is strictly smaller than the current cycle.
	 */
	public void runUntil(long cycle) throws IllegalArgumentException {
		if (this.currentCycle > cycle)
			throw new IllegalArgumentException("The given Cycle is strictly smaller than the current Cycle");

		while (this.currentCycle < cycle) {
			timer.cycle(this.currentCycle);
			lcdController.cycle(currentCycle);
			cpu.cycle(this.currentCycle);
			this.currentCycle += 1;
		}
	}

	/**
	 * @return Timer
	 */
	public Timer timer() {
		return timer;
	}

	/**
	 * @return currentCycle, a long representing the current cycle.
	 */
	public long cycles() {
		return this.currentCycle;
	}

	/**
	 * 
	 * @return the Bus (bus)
	 */

	public Bus bus() {
		return this.bus;
	}

	/**
	 * 
	 * @return the Cpu (cpu).
	 */
	public Cpu cpu() {
		return this.cpu;
	}

	public LcdController lcdController() {
		return lcdController;
	}
	public Joypad joypad() {
		return joypad;
	}

}