package datadog.trace.common.sampling;

import datadog.trace.DDSpan;

public interface PrioritySampler {
  void setSamplingPriority(DDSpan span);
}
