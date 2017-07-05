import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;

public class Logger {

	static void logMessage(String message) {
		try {
			Files.write(Paths.get("LogFile.txt"), (LocalDateTime.now() + " : " + message + System.lineSeparator()).getBytes(),
					StandardOpenOption.CREATE, StandardOpenOption.APPEND);
		} catch (IOException e) {
			System.out.println("Unable to write to log file");
		}
	}
}
