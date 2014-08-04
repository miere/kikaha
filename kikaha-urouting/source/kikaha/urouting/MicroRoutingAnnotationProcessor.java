package kikaha.urouting;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;

import kikaha.urouting.api.DELETE;
import kikaha.urouting.api.GET;
import kikaha.urouting.api.MultiPartFormData;
import kikaha.urouting.api.PATCH;
import kikaha.urouting.api.POST;
import kikaha.urouting.api.PUT;
import trip.spi.helpers.filter.Filter;

@SupportedAnnotationTypes( "kikaha.urouting.api.*" )
public class MicroRoutingAnnotationProcessor extends AbstractProcessor {

	RoutingMethodClassGenerator generator;

	@Override
	public synchronized void init( ProcessingEnvironment processingEnv ) {
		super.init( processingEnv );
		generator = new RoutingMethodClassGenerator( filer() );
	}

	@Override
	public boolean process( Set<? extends TypeElement> annotations, RoundEnvironment roundEnv ) {
		try {
			generateRoutingMethods( roundEnv, GET.class );
			generateRoutingMethods( roundEnv, POST.class );
			generateRoutingMethods( roundEnv, PUT.class );
			generateRoutingMethods( roundEnv, DELETE.class );
			generateRoutingMethods( roundEnv, PATCH.class );
			generateRoutingMethods( roundEnv, MultiPartFormData.class );
			return false;
		} catch ( IOException e ) {
			throw new IllegalStateException( e );
		}
	}

	void generateRoutingMethods( RoundEnvironment roundEnv, Class<? extends Annotation> httpMethodAnnotation ) throws IOException {
		Iterable<Element> elementsAnnotatedWith = retrieveMethodsAnnotatedWith( roundEnv, httpMethodAnnotation );
		for ( Element method : elementsAnnotatedWith )
			generateRoutingMethods( (ExecutableElement)method, roundEnv, httpMethodAnnotation );
	}

	void generateRoutingMethods( ExecutableElement method, RoundEnvironment roundEnv,
			Class<? extends Annotation> httpMethodAnnotation ) throws IOException {
		generator.generate( RoutingMethodData.from( method, httpMethodAnnotation ) );
	}

	@SuppressWarnings( "unchecked" )
	Iterable<Element> retrieveMethodsAnnotatedWith( RoundEnvironment roundEnv, Class<? extends Annotation> annotation )
			throws IOException {
		return (Iterable<Element>)Filter.filter(
				roundEnv.getElementsAnnotatedWith( annotation ),
				new MethodsOnlyCondition() );
	}

	Filer filer() {
		return this.processingEnv.getFiler();
	}

	/**
	 * We just return the latest version of whatever JDK we run on. Stupid?
	 * Yeah, but it's either that or warnings on all versions but 1. Blame Joe.
	 * 
	 * PS: this method was copied from Project Lombok. ;)
	 */
	@Override
	public SourceVersion getSupportedSourceVersion() {
		return SourceVersion.values()[SourceVersion.values().length - 1];
	}

}
