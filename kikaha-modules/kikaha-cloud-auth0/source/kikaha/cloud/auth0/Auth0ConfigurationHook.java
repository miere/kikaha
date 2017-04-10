package kikaha.cloud.auth0;

import static kikaha.cloud.auth0.Auth0Authentication.NONCE;

import javax.inject.*;
import java.util.*;
import io.undertow.server.HttpServerExchange;
import kikaha.core.ChainedMap;
import kikaha.core.modules.security.*;
import kikaha.core.modules.security.login.AuthLoginHttpHandler;
import lombok.extern.slf4j.Slf4j;

/**
 *
 */
@Slf4j
@Singleton
public class Auth0ConfigurationHook implements AuthLoginHttpHandler.ConfigurationHook {

	@Inject Auth0.AuthConfig authConfig;
	@Inject FormAuthenticationConfiguration formConfig;

	@Override
	@SuppressWarnings( "unchecked" )
	public Map<String, Object> getExtraParameters() {
		return (Map)ChainedMap.with( "clientId", authConfig.clientId )
				.and( "clientDomain", authConfig.clientDomain )
				.and( "authenticationCallbackUrl", formConfig.getCallbackUrl() );
	}

	@Override
	public Map<String, Object> configure( HttpServerExchange exchange, Session session ) {
		final String state = SessionIdGenerator.generate();
		session.setAttribute( NONCE, "state=" + state );
		return Collections.singletonMap( "state", state );
	}
}
