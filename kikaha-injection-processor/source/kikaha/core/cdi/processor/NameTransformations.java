package kikaha.core.cdi.processor;

public class NameTransformations {

	public static String stripGenericsFrom( String name ) {
		return name.replaceAll("<.*>", "");
	}
}
