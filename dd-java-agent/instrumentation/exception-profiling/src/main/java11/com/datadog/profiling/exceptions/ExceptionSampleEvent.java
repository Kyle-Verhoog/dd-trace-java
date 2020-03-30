package com.datadog.profiling.exceptions;

import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;

@Name("datadog.ExceptionSample")
@Label("ExceptionSample")
@Description("Datadog exception sample event.")
@Category("Datadog")
public class ExceptionSampleEvent extends Event {
  @Label("Exception Type")
  private String type;

  @Label("Exception message")
  private final String message;

  @Label("Exception stackdepth")
  private final int stackDepth;

  @Label("Sampled")
  private final boolean sampled;

  @Label("First occurrence")
  private final boolean firstOccurrence;

  public ExceptionSampleEvent(Exception e, boolean sampled, boolean firstOccurrence) {
    this.type = e.getClass().getName();
    this.message = e.getMessage();
    this.stackDepth = e.getStackTrace().length;
    this.sampled = sampled;
    this.firstOccurrence = firstOccurrence;
  }

  // used in tests only
  String getType() {
    return type;
  }
}
