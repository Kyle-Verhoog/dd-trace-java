package datadog.trace.instrumentation.mulehttpconnector.httprequestor;

import com.ning.http.client.Request;
import datadog.trace.api.DDTags;
import datadog.trace.bootstrap.ContextStore;
import datadog.trace.bootstrap.InstrumentationContext;
import datadog.trace.bootstrap.instrumentation.api.AgentScope;
import datadog.trace.bootstrap.instrumentation.api.AgentSpan;
import net.bytebuddy.asm.Advice;
import org.mule.service.http.impl.service.client.async.ResponseAsyncHandler;

import static datadog.trace.bootstrap.instrumentation.api.AgentTracer.activateSpan;
import static datadog.trace.bootstrap.instrumentation.api.AgentTracer.startSpan;
import static datadog.trace.instrumentation.mulehttpconnector.httplistener.MuleHttpConnectorDecorator.DECORATE;

public class HttpRequesterRequestAdvice {

  @Advice.OnMethodExit(suppress = Throwable.class)
  public static void onEnter(
      @Advice.This final Object source,
      @Advice.Argument(0) final Request request,
      @Advice.Argument(1) final ResponseAsyncHandler handler) {

    final ContextStore<ResponseAsyncHandler, AgentSpan> contextStore =
        InstrumentationContext.get(ResponseAsyncHandler.class, AgentSpan.class);

    final AgentSpan span = startSpan("mule.http.requester.request");
    DECORATE.afterStart(span);
    DECORATE.onRequest(span, request);
    final String resourceName = request.getMethod() + " " + source.getClass().getName();
    span.setTag(DDTags.RESOURCE_NAME, resourceName);

    final AgentScope scope = activateSpan(span, false);
    scope.setAsyncPropagation(true);
    contextStore.put(handler, span);
  }

  //  @Advice.OnMethodExit(suppress = Throwable.class)
  //  public static void stopSpan(@Advice.Enter final AgentScope scope) {
  //    final AgentSpan span = scope.span();
  //    DECORATE.beforeFinish(span);
  //    span.finish();
  //  }
}
