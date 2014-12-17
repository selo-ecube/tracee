package io.tracee.jaxws.container;


import io.tracee.Tracee;
import io.tracee.TraceeConstants;
import io.tracee.jaxws.client.TraceeClientHandlerResolver;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ejb.embeddable.EJBContainer;
import javax.naming.NamingException;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import java.net.URL;
import java.util.List;
import java.util.Properties;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class TraceeJaxWsTestServiceIT {

	private static URL ENDPOINT_URL;
	private static final QName ENDPOINT_QNAME = new QName(TraceeJaxWsEndpoint.Descriptor.TNS, TraceeJaxWsEndpointImpl.Descriptor.SERVICE_NAME);

	@BeforeClass
	public static void setUp() throws Exception {
		ENDPOINT_URL = new URL("http://127.0.0.1:4204/jaxws/TraceeJaxWsEndpointImpl?wsdl");
		Properties p = new Properties();
		p.setProperty("openejb.embedded.remotable", "true");

		p.setProperty("log4j.category.io.tracee", "info");
		p.setProperty("log4j.appender.C.layout", "org.apache.log4j.PatternLayout");
		p.setProperty("log4j.appender.C.layout.ConversionPattern", "%d{yyyy-MM-dd HH:mm:ss.SSSS} %t:[x-tracee-request:%X{" + TraceeConstants.HTTP_HEADER_NAME + "}] \n %m%n");
		ejbContainer = EJBContainer.createEJBContainer(p);
	}

	private static EJBContainer ejbContainer;

	@AfterClass
	public static void tearDown() throws NamingException {
		ejbContainer.getContext().close();
		ejbContainer.close();
	}

	@After
	public void resetBackend() {
		Tracee.getBackend().clear();
	}

	@Test
	public void testJaxWsServiceRoundtrip() throws Exception {
		Service calculatorService = Service.create(ENDPOINT_URL, ENDPOINT_QNAME);
		calculatorService.setHandlerResolver(new TraceeClientHandlerResolver());

		Tracee.getBackend().put("inRequest", "yes");

		final TraceeJaxWsEndpoint remote = calculatorService.getPort(TraceeJaxWsEndpoint.class);
		final List<String> result = remote.getCurrentTraceeContext();

		// the result contains the posted "inRequest" param and the generated requestId
		assertThat(result, not(empty()));
		assertThat(result, 	hasItem(TraceeConstants.REQUEST_ID_KEY));
		assertThat(result, 	hasItem("inRequest"));
		assertThat(result, 	hasItem("yes"));

		Tracee.getBackend().getLoggerFactory().getLogger(TraceeJaxWsTestServiceIT.class).info("back in client");

		// the clients traceeContext contains the REQUEST_ID_KEY generated by the container.
		assertThat(Tracee.getBackend().get(TraceeConstants.REQUEST_ID_KEY), not(isEmptyOrNullString()));
	}
}