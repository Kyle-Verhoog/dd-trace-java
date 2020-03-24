package datadog.trace.jfr.openjdk;

import datadog.trace.DDSpanContext;
import datadog.trace.jfr.DDNoopScopeEvent;
import datadog.trace.jfr.DDScopeEvent;
import datadog.trace.jfr.DDScopeEventFactory;
import jdk.jfr.EventType;

/** Event factory for {@link ScopeEvent} */
public class ScopeEventFactory implements DDScopeEventFactory {

  private final EventType eventType;

  public ScopeEventFactory() throws ClassNotFoundException {
    BlackList.checkBlackList();
    // Note: Loading ScopeEvent when ScopeEventFactory is loaded is important because it also loads
    // JFR classes - which may not be present on some JVMs
    eventType = EventType.getEventType(ScopeEvent.class);
  }

  @Override
  public DDScopeEvent create(final DDSpanContext context) {
    return eventType.isEnabled() ? new ScopeEvent(context) : DDNoopScopeEvent.INSTANCE;
  }
}
