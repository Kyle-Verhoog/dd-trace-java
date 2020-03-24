package datadog.trace.jfr;

import datadog.trace.DDSpanContext;

/** Event factory that returns {@link DDNoopScopeEvent} */
public final class DDNoopScopeEventFactory implements DDScopeEventFactory {
  @Override
  public DDScopeEvent create(final DDSpanContext context) {
    return DDNoopScopeEvent.INSTANCE;
  }
}
