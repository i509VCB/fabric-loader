/*
 * Copyright 2016 FabricMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.fabricmc.loader.metadata;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import com.grack.nanojson.JsonParserException;
import com.grack.nanojson.JsonReader;

public final class NewModMetadataParser {
	public static final int LATEST_VERSION = 1;
	// Per the ECMA-404 (www.ecma-international.org/publications/files/ECMA-ST/ECMA-404.pdf), the JSON spec does not prohibit duplicate keys.
	// For all intensive purposes of replicating the logic of Gson before we have migrated to Nanojson, duplicate keys will replace previous entries.
	public static LoaderModMetadata parseMetadata(Path modJson) throws JsonParserException, IOException, ParseMetadataException {
		// We don't know the version of the `fabric.mod.json`.
		// The first thing we do is figure out the schema version of the file.
		// If the schemaVersion field is absent, assume version 0
		// If we find the schemaVersion field, close the input stream and read using the proper facilities.
		int schemaVersion;

		try (final InputStream stream = Files.newInputStream(modJson)) {
			final JsonReader reader = JsonReader.from(stream);

			// We don't know the version of the `fabric.mod.json`.
			// The first thing we do is figure out the schema version of the file.
			// If the schemaVersion field is absent, assume version 0
			schemaVersion = NewModMetadataParser.detectVersion(reader);
		}

		switch (schemaVersion) {
		case 1:
			return V1ModMetadataParser.parse(modJson);
		case 0:
			return V0ModMetadataParser.parse(modJson);
		default:
			// TODO: Warn about version that does not exist yet
			return null;
		}
	}

	private static int detectVersion(JsonReader reader) throws JsonParserException, ParseMetadataException {
		if (reader.current() == JsonReader.Type.OBJECT) {
			reader.object();

			Integer schemaVersion = null;

			while (reader.next()) {
				final String key = reader.key();

				if (key.equals("schemaVersion")) {
					if (reader.current() != JsonReader.Type.NUMBER) {
						throw new ParseMetadataException("Schema version was not a number");
					}

					if (schemaVersion != null) {
						// If we have a mismatch on the duplicate entry of the schemaVersion, then fail.
						int version = reader.intVal();

						if (version != schemaVersion) {
							throw new ParseMetadataException(String.format("Duplicate element of \"schemaVersion\" mismatches original schema version of \"%s\", duplicate value was \"%s\"", schemaVersion, version));
						}

						// Ok, duplicate matches
						schemaVersion = version;
					} else {
						schemaVersion = reader.intVal();
					}
				}
			}

			// Assume version 0 if no `schemaVersion` field is present on root object
			return schemaVersion != null ? schemaVersion : 0;
		} else {
			throw new ParseMetadataException("Root of \"fabric.mod.json\" must be an object");
		}
	}

	private NewModMetadataParser() {
	}
}
