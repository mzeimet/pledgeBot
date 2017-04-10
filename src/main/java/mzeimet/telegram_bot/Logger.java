package mzeimet.telegram_bot;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class Logger {

	public static void write(String s) {
		File f = new File(Config.LOG_FILE_PATH);
		if (!f.exists())
			try {
				f.createNewFile();
				String txt = s + "\n";
				Files.write(Paths.get(Config.LOG_FILE_PATH), txt.getBytes(), StandardOpenOption.APPEND);
			} catch (IOException e) {
				e.printStackTrace();
			}
	}
}
