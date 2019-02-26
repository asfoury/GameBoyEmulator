package ch.epfl.gameboj.gui;

import java.io.File;
import java.util.List;
import java.util.Map;

import ch.epfl.gameboj.GameBoy;
import ch.epfl.gameboj.component.Joypad;
import ch.epfl.gameboj.component.Joypad.Key;
import ch.epfl.gameboj.component.cartridge.Cartridge;
import ch.epfl.gameboj.component.lcd.LcdController;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public final class Main extends Application {
	private final Map<KeyCode, Joypad.Key> directionKeysMap = Map.of(KeyCode.UP, Key.UP, KeyCode.DOWN, Key.DOWN,
			KeyCode.LEFT, Key.LEFT, KeyCode.RIGHT, Key.RIGHT);

	private final Map<String, Joypad.Key> letterKeysMap = Map.of("a", Key.A, "b", Key.B, "s", Key.START, " ",
			Key.SELECT);
   /*
    * calls the method launch of Application
    */
	public static void main(String[] args) {
        String a = "Super Mario Land (JUE) (V1.1) [!] (1).gb";
        String b = "flappyboy.gb";
        String c = "Super Mario Land 2 - 6 Golden Coins (UE) (V1.2) [!].gb";
		Application.launch(b);
		
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javafx.application.Application#start(javafx.stage.Stage)
	 */
	@Override
	public void start(Stage stage) throws Exception {
		double numberOfCyclesPerNanoSecond = Math.pow(2, 20) / Math.pow(10, 9);
		List<String> args = getParameters().getRaw();
		if (args.size() != 1)
			System.exit(1);
		File romFile = new File(args.get(0));
		Cartridge cartridge = Cartridge.ofFile(romFile);
		GameBoy gameboy = new GameBoy(cartridge);
		ImageView imageView = new ImageView();
		imageView.setFitWidth(2f * LcdController.LCD_WIDTH);
		imageView.setFitHeight(2f * LcdController.LCD_HEIGHT);

		imageView.setOnKeyPressed((keyEvent) -> {
			if (keyEvent.getText().length() == 0) {
				Joypad.Key k = directionKeysMap.get(keyEvent.getCode());
				if (k != null)
					gameboy.joypad().keyPressed(k);
			} else {
				Joypad.Key k = letterKeysMap.get(keyEvent.getText());
				if (k != null)
					gameboy.joypad().keyPressed(k);
			}
		});

		imageView.setOnKeyReleased((keyEvent) -> {
			if (keyEvent.getText().length() == 0) {
				Joypad.Key k = directionKeysMap.get(keyEvent.getCode());
				if (k != null)
					gameboy.joypad().keyReleased(k);
			} else {
				Joypad.Key k = letterKeysMap.get(keyEvent.getText());
				if (k != null)
					gameboy.joypad().keyReleased(k);
			}
		});
		BorderPane borderPane = new BorderPane(imageView);
		Scene scene = new Scene(borderPane);
        stage.setTitle("GameBoy");
		stage.setResizable(false);
		stage.setScene(scene);
		stage.sizeToScene();
		stage.show();
		imageView.requestFocus();
		
		long start = System.nanoTime();
		new AnimationTimer() {

			@Override
			public void handle(long now) {
				long elapsed = now - start;
				gameboy.runUntil((long) (elapsed * numberOfCyclesPerNanoSecond));
				imageView.setImage(ImageConverter.convert(gameboy.lcdController().currentImage()));

			}

		}.start();

	}

}
