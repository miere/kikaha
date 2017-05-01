package kikaha.config;

import java.util.Collection;
import javax.enterprise.inject.*;
import javax.inject.*;
import lombok.Getter;

/**
 * Make the default configuration widely available.
 */
@Singleton
public class KikahaConfigurationProducer {

	@Inject @Typed( ConfigEnrichment.class )
	Collection<ConfigEnrichment> listOfEnrichment;

	@Getter( lazy = true )
	private final Config config = loadConfiguration();

	private Config loadConfiguration() {
		Config mergeableConfig = ConfigLoader.loadDefaults();
		for ( final ConfigEnrichment enrichment : listOfEnrichment )
			mergeableConfig = enrichment.enrich( mergeableConfig );
		return mergeableConfig;
	}

	@Produces
	public Config produceAConfiguration(){
		return getConfig();
	}
}
