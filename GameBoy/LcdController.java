

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ch.epfl.gameboj.AddressMap;
import ch.epfl.gameboj.Bus;
import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.Register;
import ch.epfl.gameboj.RegisterFile;
import ch.epfl.gameboj.bits.Bit;
import ch.epfl.gameboj.bits.BitVector;
import ch.epfl.gameboj.bits.Bits;
import ch.epfl.gameboj.component.Clocked;
import ch.epfl.gameboj.component.Component;
import ch.epfl.gameboj.component.cpu.Alu;
import ch.epfl.gameboj.component.cpu.Cpu;
import ch.epfl.gameboj.component.cpu.Cpu.Interrupt;
import ch.epfl.gameboj.component.lcd.LcdImage;
import ch.epfl.gameboj.component.lcd.LcdImageLine;
import ch.epfl.gameboj.component.memory.Ram;

/**
 * LCD controller.
 *
 * @author Karim Sabaa (269647)
 * @author Mohamed Elasfoury (289473)
 */
public final class LcdController implements Component, Clocked {
	public static final int LCD_WIDTH = 160;
	public static final int LCD_HEIGHT = 144;
	private long nextNonIdleCycle, lcdOnCycle, currentCycle;
	private Ram videoRam, OAM;
	private Cpu cpu;
	private LcdImage.Builder nextImageBuilder;
	private LcdImage currentImage;
	private boolean startCopying;
	private int copyingCounter, winY;
	private RegisterFile<register> regs;
	private Bus bus;
	BitVector zero = new BitVector(LCD_WIDTH);

	private enum plan { // NOO
		FRONT, BACK
	}

	private enum planType { // NOO
		BACKGROUND, WINDOW
	}

	private enum register implements Register {
		LCDC, STAT, SCY, SCX, LY, LYC, DMA, BGP, OBP0, OBP1, WY, WX
	};

	private enum LCDC implements Bit {
		BG, OBJ, OBJ_SIZE, BG_AREA, TILE_SOURCE, WIN, WIN_AREA, LCD_STATUS
	}

	private enum STAT implements Bit {
		MODE0, MODE1, LYC_EQ_LY, INT_MODE0, INT_MODE1, INT_MODE2, INT_LYC
	}

	private enum mode {
		ZERO, ONE, TWO, THREE
	}

	private enum sprite implements Bit {
		DONTCARE0, DONTCARE1, DONTCAR2, DONTCARE3, PALETTE, FLIP_H, FLIP_V, BEHIND_BG
	}

	/**
	 * 
	 * @param cpu
	 *            of the gameBoy
	 * @throws NullPointerException
	 *             if the given CPU is null
	 */
	public LcdController(Cpu cpu) throws NullPointerException {
		Preconditions.checkArgument(cpu != null);
		videoRam = new Ram(AddressMap.VIDEO_RAM_SIZE);
		OAM = new Ram(AddressMap.OAM_RAM_SIZE);
		this.cpu = cpu;
		currentCycle = 0;
		nextNonIdleCycle = 0;
		startCopying = false;
		regs = new RegisterFile<register>(new register[12]);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ch.epfl.gameboj.component.Clocked#cycle(long)
	 */
	@Override
	public void cycle(long cycle) {
		if (copyingCounter == AddressMap.OAM_RAM_SIZE) {
			startCopying = false;
			copyingCounter = 0;
		}
		if (startCopying) {
			OAM.write(copyingCounter, bus.read(Bits.make16(regs.get(register.DMA), copyingCounter)));
			++copyingCounter;

		}

		if (nextNonIdleCycle == Long.MAX_VALUE && regs.testBit(register.LCDC, LCDC.LCD_STATUS)) {
			nextNonIdleCycle = cycle;
			lcdOnCycle = cycle;
		}
		if (cycle < nextNonIdleCycle)
			return;
		currentCycle = cycle;
		reallyCycle();

	}

	@Override
	public void attachTo(Bus bus) {
		this.bus = bus;
		bus.attach(this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ch.epfl.gameboj.component.Component#read(int)
	 */
	public int read(int address) {

		Preconditions.checkBits16(address);
		if (address >= AddressMap.OAM_START && address < AddressMap.OAM_END) {
			return OAM.read(address - AddressMap.OAM_START);
		}
		if (address >= AddressMap.REGS_LCDC_START && address < AddressMap.REGS_LCDC_END) {
			return regs.get(register.values()[address - AddressMap.REGS_LCDC_START]);
		}
		if (address >= AddressMap.VIDEO_RAM_START && address < AddressMap.VIDEO_RAM_END) {
			return videoRam.read(address - AddressMap.VIDEO_RAM_START);
		}
		return Component.NO_DATA;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ch.epfl.gameboj.component.Component#write(int, int)
	 */
	@Override
	public void write(int address, int data) throws IllegalArgumentException {
		Preconditions.checkBits8(data);
		Preconditions.checkBits16(address);
		if (address >= AddressMap.OAM_START && address < AddressMap.OAM_END) {
			OAM.write(address - AddressMap.OAM_START, data);
		}

		if (address >= AddressMap.VIDEO_RAM_START && address < AddressMap.VIDEO_RAM_END)
			videoRam.write(address - AddressMap.VIDEO_RAM_START, data);

		if (address >= AddressMap.REGS_LCDC_START && address < AddressMap.REGS_LCDC_END) {

			register reg = register.values()[address - AddressMap.REGS_LCDC_START];

			switch (reg) {
			case LCDC: {
				regs.set(register.LCDC, data);
				if (!regs.testBit(register.LCDC, LCDC.LCD_STATUS)) {
					setMode(mode.ZERO);
					regs.set(register.LY, 0);
					if (regs.testBit(register.STAT, STAT.INT_MODE0))
						cpu.requestInterrupt(Interrupt.LCD_STAT);
					nextNonIdleCycle = Long.MAX_VALUE;
				}
			}
				break;
			case STAT: {
				regs.set(register.STAT,
						(Alu.Nshift(Bits.extract(data, 3, 5), 3)) + Bits.clip(3, regs.get(register.STAT)));
			}
				break;
			case LYC: {
				setRegLY_LYC(reg, data);
			}
				break;
			case DMA: {
				startCopying = true;
				copyingCounter = 0;
				regs.set(register.DMA, data);

			}
				break;
			case LY: {
			}
				break;
			default: {
				regs.set(reg, data);
			}
				break;
			}
		}
	}

	/**
	 * returns the image currently displayed on the screen, never Null with size
	 * 160*144
	 * 
	 * @return LcdImage
	 */
	public LcdImage currentImage() {
		List<LcdImageLine> lines = new ArrayList<>();
		BitVector zeroLine = new BitVector(LCD_WIDTH);
		for (int i = 0; i < LCD_HEIGHT; ++i)
			lines.add(new LcdImageLine(zeroLine, zeroLine, zeroLine));
		return (currentImage == null) ? new LcdImage(LCD_WIDTH, LCD_HEIGHT, lines) : currentImage;
	}
	///////////////////////////////////////// Private///////////////////////////////////////////////////

	private void setRegLY_LYC(register r, int data) {
		Preconditions.checkBits8(data);
		Preconditions.checkArgument(r == register.LY || r == register.LYC);

		regs.set(r, data);
		regs.setBit(register.STAT, STAT.LYC_EQ_LY, regs.get(register.LY) == regs.get(register.LYC));
		if (regs.get(register.LY) == regs.get(register.LYC) && regs.testBit(register.STAT, STAT.INT_LYC))
			cpu.requestInterrupt(Interrupt.LCD_STAT);
	}

	private mode getMode() {

		Boolean mode0 = regs.testBit(register.STAT, STAT.MODE0);
		Boolean mode1 = regs.testBit(register.STAT, STAT.MODE1);

		return mode.values()[(mode1 ? (1 << 1) : 0) + (mode0 ? 1 : 0)];
	}

	private void setMode(mode m) {
		int stat =regs.get(register.STAT);
		regs.set(register.STAT, (stat&0b11111100)|m.ordinal());
	}

	private void reallyCycle() {
		final int numberCyclePerLine = 114;
		final int numberCycleMode2 = 20;
		final int numberCycleMode3 = 43;
		final int numberCycleMode0 = 51;
		switch (getMode()) {
		case ZERO: {
			if (regs.get(register.LY) < LCD_HEIGHT - 1) {
				setMode(mode.TWO);
				nextNonIdleCycle += numberCycleMode2;
				if (currentCycle - lcdOnCycle == 0) {
					winY = 0;
					nextImageBuilder = new LcdImage.Builder(LCD_WIDTH, LCD_HEIGHT);
				}
				setRegLY_LYC(register.LY, currentCycle - lcdOnCycle == 0 ? 0 : regs.get(register.LY) + 1);
				if (regs.testBit(register.STAT, STAT.INT_MODE2))
					cpu.requestInterrupt(Interrupt.LCD_STAT);
			} else {
				setMode(mode.ONE);
				nextNonIdleCycle += numberCyclePerLine;
				currentImage = nextImageBuilder.build();
				setRegLY_LYC(register.LY, regs.get(register.LY) + 1);
				cpu.requestInterrupt(Interrupt.VBLANK);
				if (regs.testBit(register.STAT, STAT.INT_MODE1))
					cpu.requestInterrupt(Interrupt.LCD_STAT);
			}
		}
			break;
		case ONE: {
			
			if (regs.get(register.LY) < LCD_HEIGHT + 9) {
				nextNonIdleCycle += numberCyclePerLine;
				setRegLY_LYC(register.LY, regs.get(register.LY) + 1);
			} else {
				setMode(mode.TWO);
				nextNonIdleCycle += numberCycleMode2;
				setRegLY_LYC(register.LY, 0);
				if (regs.testBit(register.STAT, STAT.INT_MODE2))
					cpu.requestInterrupt(Interrupt.LCD_STAT);
			}
		}
			break;
		case TWO: {
			setMode(mode.THREE);
			nextNonIdleCycle += numberCycleMode3;
			nextImageBuilder.setLine(regs.get(register.LY), computeLine(regs.get(register.LY)));
		}
			break;
		case THREE: {
			setMode(mode.ZERO);
			this.nextNonIdleCycle += numberCycleMode0;
			if (regs.testBit(register.STAT, STAT.INT_MODE0))
				cpu.requestInterrupt(Interrupt.LCD_STAT);
		}
			break;
		}
	}

	private int lsbAddress(int x, int y, planType pt) { // stillllllllllllllllllllllllllllllllllllllll

		int yInsideBackGround = (pt == planType.BACKGROUND) ? (y + regs.get(register.SCY)) % 256 : winY;

		int indexAddress = (yInsideBackGround / 8) * 32 + (x / 8) + AddressMap.BG_DISPLAY_DATA[regs
				.testBit(register.LCDC, (pt == planType.BACKGROUND) ? LCDC.BG_AREA : LCDC.WIN_AREA) ? 1 : 0];

		int tileIndex = read(indexAddress);
		int tileAddress;
		if (regs.testBit(register.LCDC, LCDC.TILE_SOURCE)) {
			tileAddress = AddressMap.TILE_SOURCE[1] + tileIndex * 16;
		} else {
			if (tileIndex >= 0x80) {
				tileAddress = AddressMap.TILE_SOURCE[0] + tileIndex * 0x10 - 0x800;
			} else {
				tileAddress = AddressMap.TILE_SOURCE[0] + tileIndex * 0x10 + 0x800;
			}

		}

		return tileAddress + 2 * (yInsideBackGround % 8);
	}

	private LcdImageLine computeLine(int lineIndex) {
        int wx = regs.get(register.WX) - 7;
		Boolean windowON = regs.testBit(register.LCDC, LCDC.WIN)
				&& (wx < LCD_WIDTH && wx >= 0);
		Boolean spriteON = regs.testBit(register.LCDC, LCDC.OBJ);

		LcdImageLine imageLine = (lineIndex < regs.get(register.WY) || !windowON) ? backGroundLine(lineIndex)
				: backGroundLine(lineIndex).join(windowLine(),wx);

		BitVector opacity = imageLine.opacity().or(lineWithSprites(lineIndex, plan.BACK).opacity().not());
		return (spriteON) ? lineWithSprites(lineIndex, plan.BACK).below(imageLine, opacity)
				.below(lineWithSprites(lineIndex, plan.FRONT)) : imageLine;

	}

	private LcdImageLine backGroundLine(int lineIndex) {

		Boolean backGroundOn = regs.testBit(register.LCDC, LCDC.BG);
		LcdImageLine.Builder builder = new LcdImageLine.Builder(256);
		if (backGroundOn) {
			for (int i = 0; i < 32; i++)
				builder.setBytes(i, Bits.reverse8(read(lsbAddress(i * 8, lineIndex, planType.BACKGROUND) + 1)),
						Bits.reverse8(read(lsbAddress(i * 8, lineIndex, planType.BACKGROUND))));

			return builder.build().extractWrapped(LCD_WIDTH, regs.get(register.SCX)).mapColors(regs.get(register.BGP));

		} else {
			return new LcdImageLine(zero, zero, zero);
		}
	}

	private LcdImageLine windowLine() {

		LcdImageLine.Builder builder = new LcdImageLine.Builder(LCD_WIDTH);
		for (int i = 0; i * 8 < LCD_WIDTH; i++)
			builder.setBytes(i, Bits.reverse8(read(lsbAddress(i * 8, winY, planType.WINDOW) + 1)),
					Bits.reverse8(read(lsbAddress(i * 8, winY, planType.WINDOW))));
		++winY;
		return builder.build().shift(regs.get(register.WX) - 7).mapColors(regs.get(register.BGP));
	}

	private LcdImageLine lineWithSprites(int line, plan p) {
		LcdImageLine l = new LcdImageLine(zero, zero, zero);
		int s[] = spritesIntersectingLine(line);
		for (int sprit : s) {
			if (p == plan.BACK) {
				if (Bits.test(OAM.read(sprit * 4 + 3), sprite.BEHIND_BG)) {

					l = oneSpriteLine(sprit, line).below(l);
				}
			} else {
				if (!Bits.test(OAM.read(sprit * 4 + 3), sprite.BEHIND_BG)) {

					l = oneSpriteLine(sprit, line).below(l);
				}
			}
		}
		return l;

	}

	private LcdImageLine oneSpriteLine(int indexOfSprite, int lineIndex) {

		LcdImageLine.Builder builder = new LcdImageLine.Builder(LCD_WIDTH);
		boolean flipH = Bits.test(OAM.read(4 * indexOfSprite + 3), sprite.FLIP_H);
		int l = lineInsideSprite(indexOfSprite, lineIndex);

		int tileIndex = OAM.read(4 * indexOfSprite + 2);
		int msb = videoRam.read(16 * (l >= 8 ? tileIndex + 1 : tileIndex) + 2 * (l % 8) + 1);
		int lsb = videoRam.read(16 * (l >= 8 ? tileIndex + 1 : tileIndex) + 2 * (l % 8));

		builder.setBytes(0, !flipH ? Bits.reverse8(msb) : msb, !flipH ? Bits.reverse8(lsb) : lsb);

		return builder.build().shift(OAM.read(4 * indexOfSprite + 1) - 8).mapColors(
				regs.get(Bits.test(OAM.read(4 * indexOfSprite + 3), sprite.PALETTE) ? register.OBP1 : register.OBP0));

	}

	private int[] spritesIntersectingLine(int line) {
		int counter = 0;
		int xIndex[] = new int[10];

		for (int i = 0; i < 40; i++) {
			Boolean lineIntersectSprite = line >= OAM.read(4 * i) - 16
					&& line < OAM.read(4 * i) - 16 + (regs.testBit(register.LCDC, LCDC.OBJ_SIZE) ? 16 : 8);
			if (lineIntersectSprite && counter < 10) {
				xIndex[counter] = Bits.make16(OAM.read(4 * i + 1), i);
				++counter;
			}
		}
		int index[] = new int[counter];
		Arrays.sort(xIndex, 0, counter);
		for (int i = 0; i < counter; i++)
			index[i] = Bits.clip(8, xIndex[i]);
		return index;
	}

	private int lineInsideSprite(int indexOfSprite, int lineIndex) {
		int size = (regs.testBit(register.LCDC, LCDC.OBJ_SIZE) ? 16 : 8);
		int yCoordinate = OAM.read(4 * indexOfSprite) - 16;
		return (Bits.test(OAM.read(indexOfSprite * 4 + 3), sprite.FLIP_V)) ? size - lineIndex + yCoordinate - 1
				: lineIndex - yCoordinate;
	}
}
