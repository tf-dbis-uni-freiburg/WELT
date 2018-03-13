package welt.gerbilwrapper;

import org.restlet.Application;
import org.restlet.Restlet;
import org.restlet.routing.Router;

/**
 * 
 * @author Dimitar
 *
 * Wrapper for the web server context.
 *
 */
public class WrapperApplication extends Application {

	/**
	 * Creates a root Restlet that will receive all incoming calls.
	 */
	@Override
	public Restlet createInboundRoot() {
		Router router = new Router(getContext());
		router.attach("/", DoserResource.class);
		return router;
	}
}
