package radium.oracle;

import java.io.File;
import java.io.IOException;

public class TryToReadAndWriteTNSNames {

	public static void main(String[] arguments) throws IOException {
		TNSNames.readFrom(new File("./tnsnames.ora")).writeTo(System.out);
	}
	
}
