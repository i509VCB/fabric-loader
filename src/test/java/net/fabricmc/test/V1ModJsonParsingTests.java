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

package net.fabricmc.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import com.grack.nanojson.JsonParserException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.fabricmc.loader.api.SemanticVersion;
import net.fabricmc.loader.metadata.LoaderModMetadata;
import net.fabricmc.loader.metadata.NewModMetadataParser;
import net.fabricmc.loader.metadata.ParseMetadataException;

final class V1ModJsonParsingTests {
	private static Path testLocation;

	@BeforeAll
	private static void setupPaths() {
		V1ModJsonParsingTests.testLocation = new File(System.getProperty("user.dir"))
				.toPath()
				.resolve("src")
				.resolve("test")
				.resolve("resources")
				.resolve("testing")
				.resolve("parsing")
				.resolve("v1");
	}

	@Test
	public void testRequiredValues() throws JsonParserException, IOException, ParseMetadataException {
		final Path dir = V1ModJsonParsingTests.testLocation.resolve("minimumRequirements");

		// Required fields
		final LoaderModMetadata metadata = NewModMetadataParser.parseMetadata(dir.resolve("required.json"));
		assertNotNull(metadata, "Failed to read mod metadata!");
		this.validateRequiredValues(metadata);

		// Required fields in different order to verify we don't have ordering issues
		final LoaderModMetadata reversedMetadata = NewModMetadataParser.parseMetadata(dir.resolve("required_reversed.json"));
		assertNotNull(reversedMetadata, "Failed to read mod metadata!");
		this.validateRequiredValues(reversedMetadata);
	}

	@Test
	public void verifyMissingVersionFails() {
		final Path dir = V1ModJsonParsingTests.testLocation.resolve("minimumRequirements");

		// Missing version should throw an exception
		assertThrows(ParseMetadataException.MissingRequired.class, () -> {
			NewModMetadataParser.parseMetadata(dir.resolve("missing_version.json"));
		}, "Missing version exception was not caught");
	}

	@Test
	public void validateDuplicateSchemaVersionMismatchFails() {
		final Path dir = V1ModJsonParsingTests.testLocation.resolve("minimumRequirements");

		assertThrows(ParseMetadataException.class, () -> {
			NewModMetadataParser.parseMetadata(dir.resolve("missing_version.json"));
		}, "Parser did not fail when the duplicate \"schemaVersion\" mismatches");
	}

	private void validateRequiredValues(LoaderModMetadata metadata) {
		final int schemaVersion = metadata.getSchemaVersion();
		assertEquals(1, metadata.getSchemaVersion(), String.format("Parsed JSON file had schema version %s, expected \"1\"", schemaVersion));

		final String id = metadata.getId();
		assertEquals("v1-parsing-test", id, String.format("Mod id \"%s\" was found, expected \"v1-parsing-test\"", id));

		final String friendlyString = metadata.getVersion().getFriendlyString();
		assertEquals("1.0.0-SNAPSHOT", friendlyString, String.format("Version \"%s\" was found, expected \"1.0.0-SNAPSHOT\"", friendlyString));

		assertTrue(metadata.getVersion() instanceof SemanticVersion, "Parsed version was not a semantic version, expected a semantic version");
	}
}
