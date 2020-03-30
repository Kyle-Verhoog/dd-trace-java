package datadog.trace.common.processor.rule;

import datadog.trace.DDSpan;
import datadog.trace.bootstrap.instrumentation.api.Tags;
import datadog.trace.common.processor.TraceProcessor;
import java.util.Collection;
import java.util.Map;

/**
 * Converts db.statement tag to resource name. This is later set to sql.query by the datadog agent
 * after obfuscation.
 */
public class DBStatementRule implements TraceProcessor.Rule {
  @Override
  public String[] aliases() {
    return new String[] {"DBStatementAsResourceName"};
  }

  @Override
  public void processSpan(
      final DDSpan span, final Map<String, Object> tags, final Collection<DDSpan> trace) {
    if (tags.containsKey(Tags.DB_STATEMENT)) {
      // Special case: Mongo
      // Skip the decorators
      if (tags.containsKey(Tags.COMPONENT) && "java-mongo".equals(tags.get(Tags.COMPONENT))) {
        return;
      }

      final String statement = tags.get(Tags.DB_STATEMENT).toString();
      if (!statement.isEmpty()) {
        span.setResourceName(statement);
      }
      span.setTag(Tags.DB_STATEMENT, (String) null); // Remove the tag
    }
  }
}
