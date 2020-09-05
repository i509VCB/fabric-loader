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
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.grack.nanojson.JsonParserException;
import com.grack.nanojson.JsonReader;

public final class ModMetadataParser {
	public static final int LATEST_VERSION = 1;
	// Per the ECMA-404 (www.ecma-international.org/publications/files/ECMA-ST/ECMA-404.pdf), the JSON spec does not prohibit duplicate keys.
	// For all intensive purposes of replicating the logic of Gson before we have migrated to Nanojson, duplicate keys will replace previous entries.
	public static LoaderModMetadata parseMetadata(Path modJson) throws JsonParserException, IOException, ParseMetadataException {
		List<Integer> estimatedVersions = IntStream.rangeClosed(0, ModMetadataParser.LATEST_VERSION).boxed().collect(Collectors.toList());
		Integer schemaVersion = null;

		try (final InputStream stream = Files.newInputStream(modJson)) {
			final JsonReader reader = JsonReader.from(stream);

			if (reader.current() != JsonReader.Type.OBJECT) {
				throw new ParseMetadataException("Root of \"fabric.mod.json\" must be an object");
			}

			reader.object();

			while (reader.next()) {
				if (schemaVersion != null) {
					// TODO: Certainly parse the rest of the file

					return null; // TODO: Return finished file
				}

				final String fieldKey = reader.key();

				// Always try to find schemaVersion first.
				if (fieldKey.equals("schemaVersion")) {
					if (reader.current() != JsonReader.Type.NUMBER) {
						throw new ParseMetadataException("\"schemaVersion\" must be a number");
					}

					schemaVersion = reader.intVal();
					continue;
				}

				// We do not know the `schemaVersion` of the `fabric.mod.json` right now.
				// Since json or no schema requires the field to be first, we must make an educated guess on the version we are parsing.

				// The FieldType is used to narrow down the versions that are supported as each field has a version range that is supported.
				// For field types that apply to multiple versions, assume the latest schema version is being used while parsing.
				// If a field is only present on one schema version, we can make a pretty good bet we are parsing that schema version.
				// However we cannot be certain on any version being parsed until we meet a schemaVersion field.
				final FieldType fieldType = FieldType.byName(fieldKey);

				if (fieldType == null) {
					// TODO: Warn about unsupported field, continue and ignore it's precense
					continue;
				}

				// This field is certainly belongs to one version.
				// Good bet the only version this field is applicable to is our schema version
				if (fieldType.applicableVersions().size() == 1) {

				} else {

				}
			}
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

	private ModMetadataParser() {
	}
}
