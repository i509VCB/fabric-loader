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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.grack.nanojson.JsonParserException;
import com.grack.nanojson.JsonReader;

import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;
import net.fabricmc.loader.api.metadata.ContactInformation;
import net.fabricmc.loader.api.metadata.ModDependency;
import net.fabricmc.loader.api.metadata.ModEnvironment;
import net.fabricmc.loader.api.metadata.Person;
import net.fabricmc.loader.util.version.VersionDeserializer;

final class V0ModMetadataParser {
	private static final Pattern WEBSITE_PATTERN = Pattern.compile("\\((.+)\\)");
	private static final Pattern EMAIL_PATTERN = Pattern.compile("<(.+)>");

	public static LoaderModMetadata parse(Path modJson) throws JsonParserException, IOException, ParseMetadataException {
		try (final InputStream stream = Files.newInputStream(modJson)) {
			final JsonReader reader = JsonReader.from(stream);

			// A `Fabric.mod.json` must be an object
			if (reader.current() != JsonReader.Type.OBJECT) {
				throw new ParseMetadataException("Root of \"fabric.mod.json\" must be an object");
			}

			reader.object();

			// All the values the `fabric.mod.json` may contain
			// Required
			String id = null;
			Version version = null;

			// Optional (mod loading)
			Map<String, ModDependency> requires = new HashMap<>();
			Map<String, ModDependency> conflicts = new HashMap<>();
			NewV0ModMetadata.Mixins mixins = null;
			ModEnvironment environment = ModEnvironment.UNIVERSAL; // Default is always universal
			String initializer = null;
			List<String> initializers = new ArrayList<>();

			String name = null;
			String description = null;
			Map<String, ModDependency> recommends = new HashMap<>();
			List<Person> authors = new ArrayList<>();
			List<Person> contributors = new ArrayList<>();
			ContactInformation links = null;
			String license = null;

			while (reader.next()) {
				switch (reader.key()) {
				case "id":
					if (reader.current() != JsonReader.Type.STRING) {
						throw new ParseMetadataException("Mod id must be a non-empty string with a length of 3-64 characters.");
					}

					id = reader.string();
					break;
				case "version":
					if (reader.current() != JsonReader.Type.STRING) {
						throw new ParseMetadataException("Version must be a non-empty string");
					}

					try {
						version = VersionDeserializer.deserialize(reader.string());
					} catch (VersionParsingException e) {
						throw new ParseMetadataException("Failed to parse version", e);
					}

					break;
				case "requires":
					V0ModMetadataParser.readDependenciesContainer(reader, requires);
					break;
				case "conflicts":
					V0ModMetadataParser.readDependenciesContainer(reader, conflicts);
					break;
				case "mixins":
					mixins = V0ModMetadataParser.readMixins(reader);
					break;
				case "side":
					if (reader.current() != JsonReader.Type.STRING) {
						throw new ParseMetadataException("Side must be a string");
					}

					switch (reader.string()) {
					case "universal":
						environment = ModEnvironment.UNIVERSAL;
						break;
					case "client":
						environment = ModEnvironment.CLIENT;
						break;
					case "server":
						environment = ModEnvironment.SERVER;
						break;
					}

					break;
				case "initializer":
					if (reader.current() != JsonReader.Type.STRING) {
						throw new ParseMetadataException("Initializer must be a non-empty string");
					}

					initializer = reader.string();
					break;
				case "initializers":
					if (reader.current() != JsonReader.Type.ARRAY) {
						throw new ParseMetadataException("Initializers must be in a list");
					}

					reader.array();

					while (reader.next()) {
						if (reader.current() != JsonReader.Type.STRING) {
							throw new ParseMetadataException("Initializer in initializers list must be a string");
						}

						initializers.add(reader.string());
					}

					break;
				case "name":
					if (reader.current() != JsonReader.Type.STRING) {
						throw new ParseMetadataException("Name must be a string");
					}

					name = reader.string();
					break;
				case "description":
					if (reader.current() != JsonReader.Type.STRING) {
						throw new ParseMetadataException("Mod description must be a string");
					}

					description = reader.string();
					break;
				case "recommends":
					V0ModMetadataParser.readDependenciesContainer(reader, recommends);
					break;
				case "authors":
					V0ModMetadataParser.readPeople(reader, authors);
					break;
				case "contributors":
					V0ModMetadataParser.readPeople(reader, contributors);
					break;
				case "links":
					// TODO
					break;
				case "license":
					if (reader.current() != JsonReader.Type.STRING) {
						throw new ParseMetadataException("License name must be a string");
					}

					license = reader.string();
					break;
				}
			}

			// Validate all required fields are resolved
			if (id == null) {
				throw new ParseMetadataException.MissingRequired("id");
			}

			if (version == null) {
				throw new ParseMetadataException.MissingRequired("version");
			}

			// `initializer` and `initializers` cannot be used at the same time
			if (initializer != null) {
				if (!initializers.isEmpty()) {
					throw new ParseMetadataException("initializer and initializers should not be set at the same time! (mod ID '" + id + "')");
				}
			}

			return new NewV0ModMetadata(id, version, requires, conflicts, mixins, environment, initializer, initializers, name, description, recommends, authors, contributors, links, license);
		}
	}

	private static NewV0ModMetadata.Mixins readMixins(JsonReader reader) throws JsonParserException, ParseMetadataException {
		final List<String> client = new ArrayList<>();
		final List<String> common = new ArrayList<>();
		final List<String> server = new ArrayList<>();

		if (reader.current() != JsonReader.Type.OBJECT) {
			throw new ParseMetadataException("Expected mixins to be an object.");
		}

		reader.object();

		while (reader.next()) {
			switch (reader.key()) {
			case "client":
				client.addAll(V0ModMetadataParser.readStringArray(reader, "client"));
				break;
			case "common":
				common.addAll(V0ModMetadataParser.readStringArray(reader, "common"));
				break;
			case "server":
				server.addAll(V0ModMetadataParser.readStringArray(reader, "server"));
				break;
			}
		}

		return new NewV0ModMetadata.Mixins(client, common, server);
	}

	private static List<String> readStringArray(JsonReader reader, String key) throws JsonParserException, ParseMetadataException {
		switch (reader.current()) {
		case NULL:
			return Collections.emptyList();
		case STRING:
			return Collections.singletonList(reader.string());
		case ARRAY:
			reader.array();
			final List<String> list = new ArrayList<>();

			while (reader.next()) {
				if (reader.current() != JsonReader.Type.STRING) {
					throw new ParseMetadataException(String.format("Expected entries in %s to be an array of strings", key));
				}

				list.add(reader.string());
			}

			return list;
		default:
			throw new ParseMetadataException(String.format("Expected %s to be a string or an array of strings", key));
		}
	}

	private static void readDependenciesContainer(JsonReader reader, Map<String, ModDependency> dependencies) throws JsonParserException {
	}

	private static void readPeople(JsonReader reader, List<Person> people) throws JsonParserException, ParseMetadataException {
		if (reader.current() != JsonReader.Type.ARRAY) {
			throw new ParseMetadataException("List of people must be an array");
		}

		reader.array();

		while (reader.next()) {
			people.add(V0ModMetadataParser.readPerson(reader));
		}
	}

	private static Person readPerson(JsonReader reader) throws JsonParserException {
		final HashMap<String, String> contactMap = new HashMap<>();
		String name = "";

		switch (reader.current()) {
		case STRING:
			final String person = reader.string();
			String[] parts = person.split(" ");

			Matcher websiteMatcher = V0ModMetadataParser.WEBSITE_PATTERN.matcher(parts[parts.length - 1]);

			if (websiteMatcher.matches()) {
				contactMap.put("website", websiteMatcher.group(1));
				parts = Arrays.copyOf(parts, parts.length - 1);
			}

			Matcher emailMatcher = V0ModMetadataParser.EMAIL_PATTERN.matcher(parts[parts.length - 1]);

			if (emailMatcher.matches()) {
				contactMap.put("email", emailMatcher.group(1));
				parts = Arrays.copyOf(parts, parts.length - 1);
			}

			name = String.join(" ", parts);

			return new ContactInfoBackedPerson(name, new MapBackedContactInformation(contactMap));
		case OBJECT:
			reader.object();

			while (reader.next()) {
				switch (reader.key()) {
				case "name":
					if (reader.current() != JsonReader.Type.STRING) {
						break;
					}

					name = reader.string();
					break;
				case "email":
					if (reader.current() != JsonReader.Type.STRING) {
						break;
					}

					contactMap.put("email", reader.string());
					break;
				case "website":
					if (reader.current() != JsonReader.Type.STRING) {
						break;
					}

					contactMap.put("website", reader.string());
					break;
				}
			}

			return new ContactInfoBackedPerson(name, new MapBackedContactInformation(contactMap));
		default:
			throw new RuntimeException("Expected person to be a string or object");
		}
	}
}
