package datadog.trace.common.processor.rule;

import datadog.trace.DDSpan;
import datadog.trace.DDSpanContext;
import datadog.trace.bootstrap.instrumentation.api.Tags;
import datadog.trace.common.processor.TraceProcessor;
import java.util.Collection;
import java.util.Map;
import java.util.regex.Pattern;

public class URLAsResourceNameRule implements TraceProcessor.Rule {

  // Matches any path segments with numbers in them. (exception for versioning: "/v1/")
  public static final Pattern PATH_MIXED_ALPHANUMERICS =
      Pattern.compile("(?<=/)(?![vV]\\d{1,2}/)(?:[^\\/\\d\\?]*[\\d]+[^\\/\\?]*)");

  @Override
  public String[] aliases() {
    return new String[] {"URLAsResourceName"};
  }

  @Override
  public void processSpan(
      final DDSpan span, final Map<String, Object> tags, final Collection<DDSpan> trace) {
    final DDSpanContext context = span.context();
    final Object httpStatus = tags.get(Tags.HTTP_STATUS);
    if (context.isResourceNameSet()
        || tags.get(Tags.HTTP_URL) == null
        || (httpStatus != null && (httpStatus.equals(404) || httpStatus.equals("404")))) {
      return;
    }

    final String rawPath = rawPathFromUrlString(tags.get(Tags.HTTP_URL).toString().trim());
    final String normalizedPath = normalizePath(rawPath);
    final String resourceName = addMethodIfAvailable(tags, normalizedPath);

    context.setResourceName(resourceName);
  }

  private String rawPathFromUrlString(final String url) {
    // Get the path without host:port
    // url may already be just the path.

    if (url.isEmpty()) {
      return "/";
    }

    final int queryLoc = url.indexOf("?");
    final int fragmentLoc = url.indexOf("#");
    final int endLoc;
    if (queryLoc < 0) {
      if (fragmentLoc < 0) {
        endLoc = url.length();
      } else {
        endLoc = fragmentLoc;
      }
    } else {
      if (fragmentLoc < 0) {
        endLoc = queryLoc;
      } else {
        endLoc = Math.min(queryLoc, fragmentLoc);
      }
    }

    final int protoLoc = url.indexOf("://");
    if (protoLoc < 0) {
      return url.substring(0, endLoc);
    }

    final int pathLoc = url.indexOf("/", protoLoc + 3);
    if (pathLoc < 0) {
      return "/";
    }

    if (queryLoc < 0) {
      return url.substring(pathLoc);
    } else {
      return url.substring(pathLoc, endLoc);
    }
  }

  // Method to normalise the url string
  private String normalizePath(final String path) {
    if (path.isEmpty() || path.equals("/")) {
      return "/";
    }

    return PATH_MIXED_ALPHANUMERICS.matcher(path).replaceAll("?");
  }

  private String addMethodIfAvailable(final Map<String, Object> meta, String path) {
    // if the verb (GET, POST ...) is present, add it
    final Object verb = meta.get(Tags.HTTP_METHOD);
    if (verb != null && !verb.toString().isEmpty()) {
      path = verb.toString().toUpperCase() + " " + path;
    }
    return path;
  }
}
