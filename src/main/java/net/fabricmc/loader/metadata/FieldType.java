package net.fabricmc.loader.metadata;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * An enumeration of all fields present in all {@code fabric.mod.json} schema versions.
 */
enum FieldType {
	ID("id"),
	VERSION("version"),
	// Common fields - Same parsing across all versions
	NAME("name"),
	DESCRIPTION("description"),
	// Common fields - Different parsing
	AUTHORS("authors"), // Both contain name, v0 only supported certain keys while v1 is arbitrary
	CONTRIBUTORS("contributors"),
	LICENSE("license"), // String only in v0, either string or array in v1
	REQUIRES("requires"), // Deprecated in v1.
	CONFLICTS("conflicts"), // Fail in v0, warning in v1
	MIXINS("mixins"), // Parsing is different - v0 is an object and v1 is an array

	// For removal in v2; deprecated in v1
	RECOMMENDS("recommends"),

	// V0 Only fields. Removed in V1
	SIDE("side", 0),
	INITIALIZER("initializer", 0),
	INITIALIZERS("initializers", 0),
	LINKS("links", 0), // Replaced by `contact` in v1

	// Fields added in V1
	ENVIRONMENT("environment", 1),
	ENTRYPOINTS("entrypoints", 1),
	JARS("jars", 1),
	ACCESSWIDNER("accessWidener", 1),
	DEPENDS("depends", 1),
	SUGGESTS("suggests", 1),
	BREAKS("breaks", 1),
	CONTACT("contact", 1),
	ICON("icon", 1),
	LANGUAGE_ADAPTERS("languageAdapters", 1),
	CUSTOM_VALUES("custom", 1)
	;

	private final String name;
	private final List<Integer> versions;

	/**
	 * Creates a field type which applies to all schema versions
	 * @param name the name of the field
	 */
	FieldType(String name) {
		this(name, IntStream.rangeClosed(0, ModMetadataParser.LATEST_VERSION));
	}

	FieldType(String name, int applicableVersion) {
		this(name, IntStream.of(applicableVersion));
	}

	FieldType(String name, int minimumVersion, int maximumVersion) {
		this(name, IntStream.rangeClosed(minimumVersion, maximumVersion));
	}

	// IntStream is used for the case that future schema versions may use old names for different purposes
	FieldType(String name, IntStream versionRange) {
		this.name = name;
		this.versions = Collections.unmodifiableList(versionRange.boxed().collect(Collectors.toList()));
	}

	/**
	 * Gets an (immutable) list of all versions this field is present at.
	 */
	List<Integer> applicableVersions() {
		return this.versions;
	}

	/* @Nullable */
	static FieldType byName(String name) {
		for (FieldType value : FieldType.values()) {
			if (value.name.equals(name)) {
				return value;
			}
		}

		return null;
	}
}
