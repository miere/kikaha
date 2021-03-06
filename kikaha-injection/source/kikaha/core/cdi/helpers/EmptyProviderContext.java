package kikaha.core.cdi.helpers;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Collections;

import kikaha.core.cdi.ProviderContext;

public class EmptyProviderContext implements ProviderContext {

	public final static ProviderContext INSTANCE = new EmptyProviderContext();

	@Override
	public <A extends Annotation> A getAnnotation( Class<A> annotationClass ) {
		return null;
	}

	@Override
	public Class<?> targetType() {
		return null;
	}

	@Override
	public Object attribute( Object key ) {
		return null;
	}

	@Override
	public <T> T attribute( Class<T> key ) {
		return null;
	}

	@Override
	public Collection<Class<? extends Annotation>> qualifierAnnotations() {
		return Collections.emptyList();
	}
}
