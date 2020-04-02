package datadog.trace.instrumentation.mulehttpconnector.httplistener;

import datadog.trace.api.DDTags;
import datadog.trace.bootstrap.instrumentation.api.AgentScope;
import datadog.trace.bootstrap.instrumentation.api.AgentSpan;
import net.bytebuddy.asm.Advice;
import org.mule.service.http.impl.service.server.grizzly.DefaultHttpRequestContext;

import static datadog.trace.bootstrap.instrumentation.api.AgentTracer.activateSpan;
import static datadog.trace.bootstrap.instrumentation.api.AgentTracer.startSpan;
import static datadog.trace.instrumentation.mulehttpconnector.httplistener.MuleHttpConnectorDecorator.DECORATE;

public class HttpListenerRequestAdvice {

  @Advice.OnMethodExit(suppress = Throwable.class)
  public static void onEnter(
      @Advice.This final Object source,
      @Advice.FieldValue("requestContext") final DefaultHttpRequestContext request) {
    final AgentSpan span = startSpan("mule.http.connector.request");
    DECORATE.afterStart(span);
    DECORATE.onRequest(span, request);
    final String resourceName =
        request.getRequest().getMethod() + " " + source.getClass().getName();
    span.setTag(DDTags.RESOURCE_NAME, resourceName);

    final AgentScope scope = activateSpan(span, false);
    scope.setAsyncPropagation(true);
  }

  //  @Advice.OnMethodExit(suppress = Throwable.class)
  //  public static void stopSpan(@Advice.Enter final AgentScope scope) {
  //    final AgentSpan span = scope.span();
  //    DECORATE.beforeFinish(span);
  //    span.finish();
  //  }
}
