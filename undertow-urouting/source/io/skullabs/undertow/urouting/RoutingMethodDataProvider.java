package io.skullabs.undertow.urouting;

import io.skullabs.undertow.urouting.api.ContextProducer;
import io.skullabs.undertow.urouting.api.ContextProducerFactory;
import io.skullabs.undertow.urouting.api.ConversionException;
import io.skullabs.undertow.urouting.api.ConverterFactory;
import io.skullabs.undertow.urouting.api.RoutingException;
import io.skullabs.undertow.urouting.api.Unserializer;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.Cookie;
import io.undertow.util.Headers;
import io.undertow.util.PathTemplateMatch;

import java.io.Reader;
import java.nio.channels.Channels;
import java.util.Queue;

import trip.spi.Provided;
import trip.spi.ServiceProvider;
import trip.spi.ServiceProviderException;
import trip.spi.Singleton;
import trip.spi.helpers.KeyValueProviderContext;

/**
 * Provides data to a routing method.
 */
@Singleton
public class RoutingMethodDataProvider {

	@Provided
	ConverterFactory converterFactory;

	@Provided
	ServiceProvider provider;

	@Provided
	ContextProducerFactory contextProducerFactory;

	/**
	 * Get a cookie from request converted to the {@code <T>} type as defined by
	 * {@code clazz} argument.
	 * 
	 * @param exchange
	 * @param cookieParam
	 * @param clazz
	 * @return
	 * @throws ConversionException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 */
	public <T> T getCookieParam( final HttpServerExchange exchange, final String cookieParam, final Class<T> clazz )
			throws ConversionException,
			InstantiationException, IllegalAccessException {
		final Cookie cookie = exchange.getRequestCookies().get( cookieParam );
		if ( cookie == null )
			return null;
		final String value = cookie.getValue();
		return converterFactory.getConverterFor( clazz ).convert( value );
	}

	/**
	 * Get a query parameter from request converted to the {@code <T>} type as
	 * defined by {@code clazz} argument.
	 * 
	 * @param exchange
	 * @param queryParam
	 * @param clazz
	 * @return
	 * @throws ConversionException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 */
	public <T> T getQueryParam( final HttpServerExchange exchange, final String queryParam, final Class<T> clazz )
			throws ConversionException, InstantiationException, IllegalAccessException {
		final Queue<String> queryParams = exchange.getQueryParameters().get( queryParam );
		if ( queryParams == null )
			return null;
		final String value = queryParams.peek();
		return converterFactory.getConverterFor( clazz ).convert( value );
	}

	/**
	 * Get a header parameter from request converted to the {@code <T>} type as
	 * defined by {@code clazz} argument.
	 * 
	 * @param exchange
	 * @param headerParam
	 * @param clazz
	 * @return
	 * @throws ConversionException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 */
	public <T> T getHeaderParam( final HttpServerExchange exchange, final String headerParam, final Class<T> clazz )
			throws ConversionException, InstantiationException, IllegalAccessException {
		final String value = exchange.getRequestHeaders().getFirst( headerParam );
		if ( value == null )
			return null;
		return converterFactory.getConverterFor( clazz ).convert( value );
	}

	/**
	 * Get a path parameter from request converted to the {@code <T>} type as
	 * defined by {@code clazz} argument.
	 * 
	 * @param exchange
	 * @param pathParam
	 * @param clazz
	 * @return
	 * @throws ConversionException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 */
	public <T> T getPathParam( final HttpServerExchange exchange, final String pathParam, final Class<T> clazz )
			throws ConversionException, InstantiationException, IllegalAccessException {
		final PathTemplateMatch pathTemplate = exchange.getAttachment( PathTemplateMatch.ATTACHMENT_KEY );
		final String value = pathTemplate.getParameters().get( pathParam );
		return converterFactory.getConverterFor( clazz ).convert( value );
	}

	/**
	 * Get the body of current request and convert to {@code <T>} type as
	 * defined by {@code clazz} argument.
	 * 
	 * @param exchange
	 * @param clazz
	 * @return
	 * @throws ServiceProviderException
	 * @throws RoutingException
	 */
	public <T> T getBody( final HttpServerExchange exchange, final Class<T> clazz ) throws ServiceProviderException, RoutingException {
		return getBody( exchange, clazz, null );
	}

	/**
	 * Get the body of current request and convert to {@code <T>} type as
	 * defined by {@code clazz} argument.<br>
	 * <br>
	 * 
	 * It searches for {@link Unserializer} implementations to convert sent data
	 * from client into the desired object. The "Content-Type" header is the
	 * information needed to define which {@link Unserializer} should be used to
	 * decode the sent data into an object. When no {@link Unserializer} is
	 * found it uses the {@code defaulConsumingContentType} argument to seek
	 * another one. It throws {@link RoutingException} when no decoder was
	 * found.
	 * 
	 * @param exchange
	 * @param clazz
	 * @param defaulConsumingContentType
	 * @return
	 * @throws ServiceProviderException
	 * @throws RoutingException
	 *             when no {@link Unserializer} implementation was found
	 */
	public <T> T getBody( final HttpServerExchange exchange, final Class<T> clazz, final String defaulConsumingContentType )
			throws ServiceProviderException, RoutingException {
		String contentEncoding = exchange.getRequestHeaders().getFirst( Headers.CONTENT_ENCODING_STRING );
		if ( contentEncoding == null )
			contentEncoding = "UTF-8";
		final String contentType = exchange.getRequestHeaders().getFirst( Headers.CONTENT_TYPE_STRING );
		final Unserializer unserializer = getUnserializer( contentType, defaulConsumingContentType );
		final Reader reader = Channels.newReader( exchange.getRequestChannel(), contentEncoding );
		return unserializer.unserialize( reader, clazz );
	}

	/**
	 * Retrieves an {@link Unserializer} for a given {@code contentType}
	 * argument. When no {@link Unserializer} is found it uses the
	 * {@code defaulConsumingContentType} argument to seek another one. It
	 * throws {@link RoutingException} when no decoder was found.
	 * 
	 * @param contentType
	 * @param defaulConsumingContentType
	 * @return
	 * @throws ServiceProviderException
	 * @throws RoutingException
	 */
	private Unserializer getUnserializer( final String contentType, String defaulConsumingContentType ) throws ServiceProviderException,
			RoutingException {
		Unserializer unserializer = provider.load( Unserializer.class, contentType );
		if ( unserializer == null && defaulConsumingContentType != null )
			unserializer = provider.load( Unserializer.class, defaulConsumingContentType );
		if ( unserializer == null )
			throw new RoutingException( "BadRequest: No unserializer found this request." );
		return unserializer;
	}

	/**
	 * Retrieve ( or produces ) a request-time object.
	 * 
	 * @param exchange
	 * @param clazz
	 * @return
	 * @throws ServiceProviderException
	 * @throws RoutingException
	 */
	public <T> T getData( final HttpServerExchange exchange, final Class<T> clazz ) throws ServiceProviderException, RoutingException {
		final ContextProducer<T> producerFor = contextProducerFactory.producerFor( clazz );
		if ( producerFor != null )
			return producerFor.produce( exchange );

		final KeyValueProviderContext context = new KeyValueProviderContext();
		context.attribute( HttpServerExchange.class, exchange );
		return provider.load( clazz, context );
	}
}
