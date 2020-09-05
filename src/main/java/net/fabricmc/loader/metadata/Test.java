package net.fabricmc.loader.metadata;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import com.grack.nanojson.JsonParserException;

public class Test {
	public void test(Path p) throws JsonParserException, IOException {
		try (InputStream stream = Files.newInputStream(p)) {
			final JsonObject object = JsonParser.object().from(stream);
			final int schemaVersion = object.getInt("schemaVersion", 0);

			switch (schemaVersion) {
			case 1:
				break;
			case 0:
				break;
			default:
				break;
			}
		}
	}

	interface JsonVisitor {

	}
}
