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

public final class ModMetadataParser {
	public static final int LATEST_VERSION = 1;

	// Per the ECMA-404 (www.ecma-international.org/publications/files/ECMA-ST/ECMA-404.pdf), the JSON spec does not prohibit duplicate keys.
	// For all intensive purposes of replicating the logic of Gson before we have migrated to Nanojson, duplicate keys will replace previous entries.
	public static LoaderModMetadata parseMetadata(Path modJson, /* @Nullable */ Integer schemaVersion) throws IOException, JsonParserException, ParseMetadataException {
		// So some context:
		// Per the json specification, ordering of fields is not typically enforced.
		// Furthermore we cannot guarantee the `schemaVersion` is the first field in every `fabric.mod.json`
		//
		// To work around this, we do the following:
		// Try to read first field
		// If the first field is the schemaVersion, read the file normally.
		//
		// If the first field is not the schema version, fallback to a more exhaustive check.
		// Read the rest of the file, looking for the `schemaVersion` field.
		// If we find the field, cache the value
		// If there happens to be another `schemaVersion` that has a differing value, then fail.
		// At the end, if we find no `schemaVersion` then assume the `schemaVersion` is 0
		// Re-read the JSON file.
		try (final InputStream stream = Files.newInputStream(modJson)) {
			final JsonReader reader = JsonReader.from(stream);

			if (reader.current() != JsonReader.Type.OBJECT) {
				throw new ParseMetadataException("Root of \"fabric.mod.json\" must be an object");
			}

			reader.object();

			// This is our second read
			if (schemaVersion != null) {
				return ModMetadataParser.readModMetadata(reader, schemaVersion);
			}

			boolean firstField = true;

			while (reader.next()) {
				// Try to read the schemaVersion
				if (reader.key().equals("schemaVersion")) {
					if (reader.current() != JsonReader.Type.NUMBER) {
						throw new ParseMetadataException("\"schemaVersion\" must be a number.");
					}

					if (firstField) {
						// Finish reading the metadata
						return ModMetadataParser.readModMetadata(reader, reader.intVal());
					}

					// schemaVersion is not the first field; we have found it's value though
					if (schemaVersion != null) {
						// Possible duplicate version
						final int read = reader.intVal();

						if (schemaVersion != read) {
							throw new ParseMetadataException(String.format("Found duplicate \"schemaVersion\" field with different value. First read value was \"%s\" and the duplicate value was \"%s\".", schemaVersion, read));
						}

						schemaVersion = read;
					} else {
						schemaVersion = reader.intVal();
					}
				}

				firstField = false;
			}
		}

		// No schema version was present, assume version 0
		if (schemaVersion == null) {
			schemaVersion = 0;
		}

		// Try reading the json file again with schemaVersion for context
		return ModMetadataParser.parseMetadata(modJson, schemaVersion);
	}

	private static LoaderModMetadata readModMetadata(JsonReader reader, int schemaVersion) throws JsonParserException, ParseMetadataException {
		switch (schemaVersion) {
		case 1:
			return V1ModMetadataParser.parse(reader);
		case 0:
			// TODO: Warn about v0 being deprecated when we have full object at the end of parse method
			return V0ModMetadataParser.parse(reader);
		default:
			throw new ParseMetadataException(String.format("Invalid schema version \"%s\" was found", schemaVersion));
		}
	}

	private ModMetadataParser() {
	}
}
