package me.i509.spihacks.launch;

import net.fabricmc.loader.api.SemanticVersion;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.metadata.ContactInformation;
import net.fabricmc.loader.api.metadata.ModDependency;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.fabricmc.loader.api.metadata.Person;
import net.fabricmc.loader.util.version.VersionParsingException;
import org.spongepowered.plugin.metadata.PluginContributor;
import org.spongepowered.plugin.metadata.PluginDependency;
import org.spongepowered.plugin.metadata.PluginMetadata;

import java.net.MalformedURLException;
import java.net.URL;

public final class FabricMetadataUtil {
	private FabricMetadataUtil() {
	}

	public static PluginMetadata createSpongeMetadata(final ModMetadata metadata) {
		final PluginMetadata.Builder builder = PluginMetadata.builder();
		// Required Metadata by Fabric
		builder.setId(metadata.getId());
		// TODO: instanceof check SemanticVersion and if needed make a sponge compliant version string
		builder.setVersion(metadata.getVersion().getFriendlyString());
		builder.setName(metadata.getName());
		builder.setDescription(metadata.getDescription());

		// Required Metadata by Sponge
		if (metadata.getAuthors().isEmpty() && metadata.getContributors().isEmpty()) {
			/* Fabric mods do not require any author(s)/contributor(s) in their meta.
			 * We will for our purposes consider authors and contributors from fabric as contributors within the scope of sponge.
			 * If we have no authors or contributors we will add an "unknown" contributor
			 */
			builder.contributor(FabricMetadataUtil.createUnknown()); // A ghost wrote this one lol
		} else {
			metadata.getAuthors().stream().map(FabricMetadataUtil::toSpongeFromAuthor).forEach(builder::contributor);
			metadata.getContributors().stream().map(FabricMetadataUtil::toSpongeFromContributor).forEach(builder::contributor);
		}

		// Dependencies
		metadata.getDepends().stream().map(FabricMetadataUtil::toSponge).forEach(builder::dependency);

		// Contact Info
		final ContactInformation metadataContact = metadata.getContact();
		// If we get any bad URLs we just ignore them.
		metadataContact.get("homepage").map(FabricMetadataUtil::createUrl).ifPresent(builder::setHomepage);
		metadataContact.get("sources").map(FabricMetadataUtil::createUrl).ifPresent(builder::setSource);
		metadataContact.get("issues").map(FabricMetadataUtil::createUrl).ifPresent(builder::setIssues);

		// TODO: CustomValues -> Extra Data
		/* I need to look into a way to dump literally all of loader's custom values from mod metadata
		final CustomValue test = metadata.getCustomValue("test");
		if (test.getType() != CustomValue.CvType.NULL) {
			builder.extraMetadata("test", test.getAsObject().)
		}
		builder.extraMetadata("test", null);
		 */

		return builder.build();
	}

	public static ModDependency toFabric(final PluginDependency pluginDependency) {
		return new SpongeModDependency(pluginDependency);
	}

	public static PluginDependency toSponge(final ModDependency dependency) {
		// TODO: Add method to get the version of a mod dependency into fabric loader
		// TODO: instanceof check SemanticVersion and if needed make a sponge compliant version string
		return PluginDependency.builder()
				.setId(dependency.getModId())
				/*.setVersion(dependency.getVersion().getFriendlyString())*/
				.build();
	}

	public static PluginContributor toSpongeFromAuthor(final Person person) {
		return PluginContributor.builder().setName(person.getName()).setDescription("author").build();
	}

	public static PluginContributor toSpongeFromContributor(final Person person) {
		return PluginContributor.builder().setName(person.getName()).setDescription("contributor").build();
	}

	public static PluginContributor createUnknown() {
		return PluginContributor.builder().setName("unknown").build();
	}

	/* @Nullable */
	private static URL createUrl(String url) {
		try {
			return new URL(url);
		} catch (MalformedURLException e) {
			return null;
		}
	}

	static final class SpongeModDependency implements ModDependency {
		private final String id;
		private final Version version;

		public SpongeModDependency(final PluginDependency pluginDependency) {
			this.id = pluginDependency.getId();
			Version tmp;

			try {
				tmp = Version.parse(pluginDependency.getVersion());
			} catch (VersionParsingException e) {
				tmp = null;
			}

			this.version = tmp;
		}

		@Override
		public String getModId() {
			return this.id;
		}

		@Override
		public boolean matches(Version version) {
			if (version instanceof SemanticVersion) {
				return this.version.equals(version); // TODO: Verify sponge version standard matches
			}

			return this.version.getFriendlyString().equals(version.getFriendlyString());
		}
	}
}
