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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import com.grack.nanojson.JsonParserException;
import com.grack.nanojson.JsonReader;

import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;
import net.fabricmc.loader.api.metadata.ContactInformation;
import net.fabricmc.loader.api.metadata.CustomValue;
import net.fabricmc.loader.api.metadata.ModDependency;
import net.fabricmc.loader.api.metadata.ModEnvironment;
import net.fabricmc.loader.api.metadata.Person;
import net.fabricmc.loader.util.version.VersionDeserializer;

final class V1ModMetadataParser {
	/**
	 * Reads a {@code fabric.mod.json} file of schema version {@code 1}.
	 *
	 * @param modJson the json file to read
	 * @return the metadata of this file, null if the file could not be parsed
	 * @throws IOException if there was any issue reading the file
	 * @throws JsonParserException
	 */
	static LoaderModMetadata parse(Path modJson) throws IOException, JsonParserException, ParseMetadataException {
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
			ModEnvironment environment = ModEnvironment.UNIVERSAL; // Default is always universal
			Map<String, List<EntrypointMetadata>> entrypoints = new HashMap<>();
			List<NestedJarEntry> jars = new ArrayList<>();
			List<V1ModMetadata.MixinEntry> mixins = new ArrayList<>();
			String accessWidener = null;

			// Optional (dependency resolution)
			Map<String, ModDependency> depends = new HashMap<>();
			Map<String, ModDependency> recommends = new HashMap<>();
			Map<String, ModDependency> suggests = new HashMap<>();
			Map<String, ModDependency> conflicts = new HashMap<>();
			Map<String, ModDependency> breaks = new HashMap<>();

			// Happy little accidents
			@Deprecated
			Map<String, ModDependency> requires = new HashMap<>();

			// Optional(metadata)
			String name = null;
			String description = null;
			List<Person> authors = new ArrayList<>();
			List<Person> contributors = new ArrayList<>();
			ContactInformation contact = null;
			List<String> license = new ArrayList<>();
			V1ModMetadata.IconEntry icon = null;

			// Optional (language adapter providers)
			Map<String, String> languageAdapters = new HashMap<>();

			// Optional (custom values)
			Map<String, CustomValue> customValues = new HashMap<>();

			while (reader.next()) {
				// Work our way from required to entirely optional
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
				case "environment":
					if (reader.current() != JsonReader.Type.STRING) {
						throw new ParseMetadataException("Environment must be a string");
					}

					environment = V1ModMetadataParser.readEnvironment(reader);
					break;
				case "entrypoints":
					V1ModMetadataParser.readEntrypoints(reader, entrypoints);
					break;
				case "jars":
					V1ModMetadataParser.readNestedJarEntries(reader, jars);
					break;
				case "mixins":
					V1ModMetadataParser.readMixinConfigs(reader, mixins);
					break;
				case "accessWidener":
					if (reader.current() != JsonReader.Type.STRING) {
						throw new ParseMetadataException("Access Widener file must be a string");
					}

					accessWidener = reader.string();
					break;
				case "depends":
					V1ModMetadataParser.readDependenciesContainer(reader, depends);
					break;
				case "recommends":
					V1ModMetadataParser.readDependenciesContainer(reader, recommends);
					break;
				case "suggests":
					V1ModMetadataParser.readDependenciesContainer(reader, suggests);
					break;
				case "conflicts":
					V1ModMetadataParser.readDependenciesContainer(reader, conflicts);
					break;
				case "breaks":
					V1ModMetadataParser.readDependenciesContainer(reader, breaks);
					break;
				case "requires":
					V1ModMetadataParser.readDependenciesContainer(reader, requires);
					break;
				case "name":
					if (reader.current() != JsonReader.Type.STRING) {
						throw new ParseMetadataException("Mod name must be a string");
					}

					name = reader.string();
					break;
				case "description":
					if (reader.current() != JsonReader.Type.STRING) {
						throw new ParseMetadataException("Mod description must be a string");
					}

					description = reader.string();
					break;
				case "authors":
					V1ModMetadataParser.parsePeople(reader, authors);
					break;
				case "contributors":
					V1ModMetadataParser.parsePeople(reader, contributors);
					break;
				case "contact":
					contact = V1ModMetadataParser.readContactInfo(reader);
					break;
				case "license":
					V1ModMetadataParser.readLicense(reader, license);
					break;
				case "icon":
					icon = V1ModMetadataParser.readIcon(reader);
					break;
				case "languageAdapters":
					V1ModMetadataParser.readLanguageAdapters(reader, languageAdapters);
					break;
				case "custom":
					V1ModMetadataParser.readCustomValues(reader, customValues);
					break;
				default:
					// TODO: Error?
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

			return new V1ModMetadata(id, version, environment, entrypoints, jars, mixins, accessWidener, depends, recommends, suggests, conflicts, breaks, requires, name, description, authors, contributors, contact, license, icon, languageAdapters, customValues);
		}
	}

	private static ModEnvironment readEnvironment(JsonReader reader) throws JsonParserException, ParseMetadataException {
		final String environment = reader.string().toLowerCase(Locale.ROOT);

		if (environment.isEmpty() || environment.equals("*")) {
			return ModEnvironment.UNIVERSAL;
		}  else if (environment.equals("client")) {
			return ModEnvironment.CLIENT;
		} else if (environment.equals("server")) {
			return ModEnvironment.SERVER;
		} else {
			throw new ParseMetadataException("Invalid environment type: " + environment + "!");
		}
	}

	private static void readEntrypoints(JsonReader reader, Map<String, List<EntrypointMetadata>> entrypoints) throws JsonParserException, ParseMetadataException {
		// Entrypoints must be an object
		if (reader.current() != JsonReader.Type.OBJECT) {
			throw new ParseMetadataException("Entrypoints must be an object");
		}

		reader.object();

		while (reader.next()) {
			final String key = reader.key();

			List<EntrypointMetadata> metadata = new ArrayList<>();

			if (reader.current() != JsonReader.Type.ARRAY) {
				throw new ParseMetadataException("Entrypoint list must be an array!");
			}

			reader.array();

			while (reader.next()) {
				String adapter = "default";
				String value = null;

				// Entrypoints may be specified directly as a string or as an object to allow specification of the language adapter to use.
				switch (reader.current()) {
				case STRING:
					value = reader.string();
					break;
				case OBJECT:
					reader.object();

					while (reader.next()) {
						switch (reader.key()) {
						case "adapter":
							adapter = reader.string();
							break;
						case "value":
							value = reader.string();
							break;
						default: // TODO: Ignore invalid elements in the entrypoint object?
						}
					}
					break;
				default:
					throw new ParseMetadataException("Entrypoint must be a string or object with \"value\" field");
				}

				if (value == null) {
					throw new ParseMetadataException.MissingRequired("Entrypoint value must be present");
				}

				metadata.add(new V1ModMetadata.EntrypointMetadataImpl(adapter, value));
			}

			// Empty arrays are acceptable, do not check if the List of metadata is empty
			entrypoints.put(key, metadata);
		}
	}

	private static void readNestedJarEntries(JsonReader reader, List<NestedJarEntry> jars) throws JsonParserException, ParseMetadataException {
		if (reader.current() != JsonReader.Type.ARRAY) {
			throw new ParseMetadataException("Jar entries must be in an array");
		}

		reader.array();

		while (reader.next()) {
			if (reader.current() != JsonReader.Type.OBJECT) {
				throw new ParseMetadataException("Invalid type for JAR entry!");
			}

			reader.object();
			String file = null;

			while (reader.next()) {
				if (reader.key().equals("file")) {
					if (reader.current() == JsonReader.Type.STRING) {
						file = reader.string();
					}
				}
			}

			if (file == null) {
				throw new ParseMetadataException("Missing mandatory key 'file' in JAR entry!");
			}

			jars.add(new V1ModMetadata.JarEntry(file));
		}
	}

	private static void readMixinConfigs(JsonReader reader, List<V1ModMetadata.MixinEntry> mixins) throws JsonParserException, ParseMetadataException {
		if (reader.current() != JsonReader.Type.ARRAY) {
			throw new ParseMetadataException("Mixin configs must be in an array");
		}

		reader.array();

		while (reader.next()) {
			switch (reader.current()) {
			case STRING:
				// All mixin configs specified via string are assumed to be universal
				mixins.add(new V1ModMetadata.MixinEntry(reader.string(), ModEnvironment.UNIVERSAL));
				break;
			case OBJECT:
				reader.object();

				String config = null;
				ModEnvironment environment = null;

				while (reader.next()) {
					switch (reader.key()) {
					// Environment is optional
					case "environment":
						environment = V1ModMetadataParser.readEnvironment(reader);
						break;
					case "config":
						if (reader.current() != JsonReader.Type.STRING) {
							throw new ParseMetadataException("Value of \"config\" must be a string");
						}

						config = reader.string();
						break;
					}
				}

				if (environment == null) {
					environment = ModEnvironment.UNIVERSAL; // Default to universal
				}

				if (config == null) {
					throw new ParseMetadataException.MissingRequired("Missing mandatory key 'config' in mixin entry!");
				}

				mixins.add(new V1ModMetadata.MixinEntry(config, environment));
				break;
			}
		}
	}

	private static void readDependenciesContainer(JsonReader reader, Map<String, ModDependency> modDependencies) throws JsonParserException, ParseMetadataException {
		if (reader.current() != JsonReader.Type.OBJECT) {
			throw new ParseMetadataException("Dependency container must be an object!");
		}

		reader.object();

		while (reader.next()) {
			final String modId = reader.key();
			final List<String> matcherStringList = new ArrayList<>();

			switch (reader.current()) {
			case STRING:
				matcherStringList.add(reader.string());
				break;
			case ARRAY:
				reader.array();

				while (reader.next()) {
					if (reader.current() != JsonReader.Type.STRING) {
						throw new ParseMetadataException("Dependency version range array must only contain string values");
					}

					matcherStringList.add(reader.string());
				}

				break;
			default:
				throw new ParseMetadataException("Dependency version range must be a string or string array!");
			}

			modDependencies.put(modId, new ModDependencyImpl(modId, matcherStringList));
		}
	}

	private static void parsePeople(JsonReader reader, List<Person> people) throws JsonParserException, ParseMetadataException {
		if (reader.current() != JsonReader.Type.ARRAY) {
			throw new ParseMetadataException("List of people must be an array");
		}

		reader.array();

		while (reader.next()) {
			switch (reader.current()) {
			case STRING:
				// Just a name
				people.add(new SimplePerson(reader.string()));
				break;
			case OBJECT:
				// Map-backed impl
				reader.object();
				// Name is required
				String personName = null;
				ContactInformation contactInformation = null;

				while (reader.next()) {
					switch (reader.key()) {
					case "name":
						if (reader.current() != JsonReader.Type.STRING) {
							throw new ParseMetadataException("Name of person in dependency container must be a string");
						}

						personName = reader.string();
						break;
					// Effectively optional
					case "contact":
						contactInformation = V1ModMetadataParser.readContactInfo(reader);
						break;
					default: // Ignore unsupported keys
					}
				}

				if (personName == null) {
					throw new ParseMetadataException.MissingRequired("Person object must have a 'name' field!");
				}

				if (contactInformation == null) {
					contactInformation = ContactInformation.EMPTY; // Empty if not specified
				}

				people.add(new ContactInfoBackedPerson(personName, contactInformation));
				break;
			default:
				throw new ParseMetadataException("Person type must be an object or string!");
			}
		}
	}

	private static ContactInformation readContactInfo(JsonReader reader) throws JsonParserException, ParseMetadataException {
		if (reader.current() != JsonReader.Type.OBJECT) {
			throw new ParseMetadataException("Contact info must in an object");
		}

		reader.object();

		final Map<String, String> map = new HashMap<>();

		while (reader.next()) {
			if (reader.current() != JsonReader.Type.STRING) {
				throw new ParseMetadataException("Contact information entries must be a string");
				// TODO: Spec-question - Error? Must be a string?
			}

			map.put(reader.key(), reader.string());
		}

		// Map is wrapped as unmodifiable in the contact info impl
		return new MapBackedContactInformation(map);
	}

	private static void readLicense(JsonReader reader, List<String> license) throws JsonParserException, ParseMetadataException {
		switch (reader.current()) {
		case STRING:
			license.add(reader.string());
			break;
		case ARRAY:
			reader.array();

			while (reader.next()) {
				if (reader.current() != JsonReader.Type.STRING) {
					throw new ParseMetadataException("List of licenses must only contain strings");
				}

				license.add(reader.string());
			}

			break;
		default:
			throw new ParseMetadataException("License must be a string or array of strings!");
		}
	}

	private static V1ModMetadata.IconEntry readIcon(JsonReader reader) throws JsonParserException, ParseMetadataException {
		switch (reader.current()) {
		case STRING:
			return new V1ModMetadata.Single(reader.string());
		case OBJECT:
			reader.object();

			final SortedMap<Integer, String> iconMap = new TreeMap<>(Comparator.naturalOrder());

			while (reader.next()) {
				if (reader.current() != JsonReader.Type.STRING) {
					throw new ParseMetadataException("Icon path must be a string");
				}

				String key = reader.key();

				int size;

				try {
					size = Integer.parseInt(key);
				} catch (NumberFormatException e) {
					throw new ParseMetadataException("Could not parse icon size '" + key + "'!", e);
				}

				if (size < 1) {
					throw new ParseMetadataException("Size must be positive!");
				}
			}

			if (iconMap.isEmpty()) {
				throw new ParseMetadataException("Icon object must not be empty!");
			}

			return new V1ModMetadata.MapEntry(iconMap);
		default:
			throw new ParseMetadataException("Icon entry must be an object or string!");
		}
	}

	private static void readLanguageAdapters(JsonReader reader, Map<String, String> languageAdapters) throws JsonParserException, ParseMetadataException {
		if (reader.current() != JsonReader.Type.OBJECT) {
			throw new ParseMetadataException("Language adapters must be in an object");
		}

		reader.object();

		while (reader.next()) {
			if (reader.current() != JsonReader.Type.STRING) {
				throw new ParseMetadataException("Value of language adapter entry must be a string");
			}

			languageAdapters.put(reader.key(), reader.string());
		}
	}

	private static void readCustomValues(JsonReader reader, Map<String, CustomValue> customValues) throws JsonParserException, ParseMetadataException {
		if (reader.current() != JsonReader.Type.OBJECT) {
			throw new ParseMetadataException("Custom values must be in an object!");
		}

		reader.object();

		while (reader.next()) {
			customValues.put(reader.key(), CustomValueImpl.readCustomValue(reader));
		}
	}

	private V1ModMetadataParser() {
	}
}
