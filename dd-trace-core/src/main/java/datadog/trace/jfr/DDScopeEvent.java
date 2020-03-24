package datadog.trace.jfr;

/** Scope event */
public interface DDScopeEvent {

  void start();

  void finish();
}
