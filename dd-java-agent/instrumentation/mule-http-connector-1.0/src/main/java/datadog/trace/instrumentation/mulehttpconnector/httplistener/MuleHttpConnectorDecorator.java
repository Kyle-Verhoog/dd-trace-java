package datadog.trace.instrumentation.mulehttpconnector.httplistener;

import com.ning.http.client.Request;
import com.ning.http.client.Response;
import datadog.trace.bootstrap.instrumentation.decorator.HttpClientDecorator;

import java.net.URI;
import java.net.URISyntaxException;

public class MuleHttpConnectorDecorator extends HttpClientDecorator<Request, Response> {

  public static final MuleHttpConnectorDecorator DECORATE = new MuleHttpConnectorDecorator();

  @Override
  protected String[] instrumentationNames() {
    return new String[] {"mule-http-connector"};
  }

  @Override
  protected String component() {
    return "http-listener";
  }

  @Override
  protected String method(final Request request) {
    return request.getMethod();
  }

  @Override
  protected URI url(final Request request) throws URISyntaxException {
    return request.getUri().toJavaNetURI();
  }

  @Override
  protected Integer status(final Response response) {
    return response.getStatusCode();
  }
}
