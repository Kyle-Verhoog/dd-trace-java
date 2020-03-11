import datadog.trace.agent.test.AgentTestRunner
import datadog.trace.agent.test.asserts.ListWriterAssert
import datadog.trace.api.Trace
import datadog.trace.bootstrap.instrumentation.api.AgentScope
import datadog.trace.bootstrap.instrumentation.api.AgentSpan
import datadog.trace.bootstrap.instrumentation.api.Tags
import groovy.transform.stc.ClosureParams
import groovy.transform.stc.SimpleType
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import spock.lang.Shared

import java.time.Duration

import static datadog.trace.bootstrap.instrumentation.api.AgentTracer.activateSpan
import static datadog.trace.bootstrap.instrumentation.api.AgentTracer.startSpan

class ReactorCoreTest extends AgentTestRunner {

  public static final String EXCEPTION_MESSAGE = "test exception"

  @Shared
  def addOne = { i -> addOneFunc(i) }
  @Shared
  def throwException = {
    throw new RuntimeException(EXCEPTION_MESSAGE)
  }

  def "Publisher '#name' test"() {
    when:
    def result = runUnderTrace(publisher)

    then:
    result == expected
    and:
    sortAndAssertTraces(1) {
      trace(0, workSpans + 2) {
        span(0) {
          resourceName "trace-parent"
          operationName "trace-parent"
          parent()
          tags {
            "$Tags.COMPONENT" "trace"
            defaultTags()
          }
        }
        span(1) {
          resourceName "publisher-parent"
          operationName "publisher-parent"
          childOf(span(0))
          tags {
            defaultTags()
          }
        }
        for (int i = 0; i < workSpans; i++) {
          span(i + 2) {
            resourceName "addOne"
            operationName "addOne"
            childOf(span(1))
            tags {
              "$Tags.COMPONENT" "trace"
              defaultTags()
            }
          }
        }
      }
    }

    where:
    name                  | expected | workSpans | publisher
    "basic mono"          | 2        | 1         | Mono.just(1).map(addOne)
    "two operations mono" | 4        | 2         | Mono.just(2).map(addOne).map(addOne)
    "delayed mono"        | 4        | 1         | Mono.just(3).delayElement(Duration.ofMillis(100)).map(addOne)
    "delayed twice mono"  | 6        | 2         | Mono.just(4).delayElement(Duration.ofMillis(100)).map(addOne).delayElement(Duration.ofMillis(100)).map(addOne)
    "basic flux"          | [6, 7]   | 2         | Flux.fromIterable([5, 6]).map(addOne)
    "two operations flux" | [8, 9]   | 4         | Flux.fromIterable([6, 7]).map(addOne).map(addOne)
    "delayed flux"        | [8, 9]   | 2         | Flux.fromIterable([7, 8]).delayElements(Duration.ofMillis(100)).map(addOne)
    "delayed twice flux"  | [10, 11] | 4         | Flux.fromIterable([8, 9]).delayElements(Duration.ofMillis(100)).map(addOne).delayElements(Duration.ofMillis(100)).map(addOne)

    "mono from callable"  | 12       | 2         | Mono.fromCallable({ addOneFunc(10) }).map(addOne)
  }

  def "Publisher error '#name' test"() {
    when:
    runUnderTrace(publisher)

    then:
    def exception = thrown RuntimeException
    exception.message == EXCEPTION_MESSAGE
    and:
    sortAndAssertTraces(1) {
      trace(0, 2) {
        span(0) {
          resourceName "trace-parent"
          operationName "trace-parent"
          parent()
          errored true
          tags {
            "$Tags.COMPONENT" "trace"
            errorTags(RuntimeException, EXCEPTION_MESSAGE)
            defaultTags()
          }
        }
        span(1) {
          resourceName "publisher-parent"
          operationName "publisher-parent"
          childOf(span(0))
          // MonoError and FluxError are both Fuseable.ScalarCallable which we cannot wrap without
          // causing a lot of problems in other places where we integrate with reactor, like webflux
          //          errored true
          //          tags {
          //            errorTags(RuntimeException, EXCEPTION_MESSAGE)
          //            defaultTags()
          //          }
        }
      }
    }

    where:
    name   | publisher
    "mono" | Mono.error(new RuntimeException(EXCEPTION_MESSAGE))
    "flux" | Flux.error(new RuntimeException(EXCEPTION_MESSAGE))
  }

  def "Publisher step '#name' test"() {
    when:
    runUnderTrace(publisher)

    then:
    def exception = thrown RuntimeException
    exception.message == EXCEPTION_MESSAGE
    and:
    sortAndAssertTraces(1) {
      trace(0, workSpans + 2) {
        span(0) {
          resourceName "trace-parent"
          operationName "trace-parent"
          parent()
          errored true
          tags {
            "$Tags.COMPONENT" "trace"
            errorTags(RuntimeException, EXCEPTION_MESSAGE)
            defaultTags()
          }
        }
        span(1) {
          resourceName "publisher-parent"
          operationName "publisher-parent"
          childOf(span(0))
          errored true
          tags {
            errorTags(RuntimeException, EXCEPTION_MESSAGE)
            defaultTags()
          }
        }
        for (int i = 0; i < workSpans; i++) {
          span(i + 2) {
            resourceName "addOne"
            operationName "addOne"
            childOf(span(1))
            tags {
              "$Tags.COMPONENT" "trace"
              defaultTags()
            }
          }
        }
      }
    }

    where:
    name                 | workSpans | publisher
    "basic mono failure" | 1         | Mono.just(1).map(addOne).map({ throwException() })
    "basic flux failure" | 1         | Flux.fromIterable([5, 6]).map(addOne).map({ throwException() })
  }

  def "Publisher '#name' cancel"() {
    when:
    cancelUnderTrace(publisher)

    then:
    assertTraces(1) {
      trace(0, 2) {
        span(0) {
          resourceName "trace-parent"
          operationName "trace-parent"
          parent()
          tags {
            "$Tags.COMPONENT" "trace"
            defaultTags()
          }
        }
        span(1) {
          resourceName "publisher-parent"
          operationName "publisher-parent"
          childOf(span(0))
          tags {
            defaultTags()
          }
        }
      }
    }

    where:
    name         | publisher
    "basic mono" | Mono.just(1)
    "basic flux" | Flux.fromIterable([5, 6])
  }

  @Trace(operationName = "trace-parent", resourceName = "trace-parent")
  def runUnderTrace(def publisher) {
    final AgentSpan span = startSpan("publisher-parent")
    AgentScope scope = activateSpan(span, true)
    scope.setAsyncPropagation(true)
    try {
      // Read all data from publisher
      if (publisher instanceof Mono) {
        return publisher.block()
      } else if (publisher instanceof Flux) {
        return publisher.toStream().toArray({ size -> new Integer[size] })
      }

      throw new RuntimeException("Unknown publisher: " + publisher)
    } finally {
      scope.close()
    }
  }

  @Trace(operationName = "trace-parent", resourceName = "trace-parent")
  def cancelUnderTrace(def publisher) {
    final AgentSpan span = startSpan("publisher-parent")
    AgentScope scope = activateSpan(span, true)
    scope.setAsyncPropagation(true)

    publisher.subscribe(new Subscriber<Integer>() {
      void onSubscribe(Subscription subscription) {
        subscription.cancel()
      }

      void onNext(Integer t) {
      }

      void onError(Throwable error) {
      }

      void onComplete() {
      }
    })

    scope.close()
  }

  @Trace(operationName = "addOne", resourceName = "addOne")
  def static addOneFunc(int i) {
    return i + 1
  }

  void sortAndAssertTraces(
    final int size,
    @ClosureParams(value = SimpleType, options = "datadog.trace.agent.test.asserts.ListWriterAssert")
    @DelegatesTo(value = ListWriterAssert, strategy = Closure.DELEGATE_FIRST)
    final Closure spec) {

    TEST_WRITER.waitForTraces(size)

    TEST_WRITER.each {
      it.sort({
        a, b ->
          // Intentionally backward because asserts are sorted that way already
          return b.resourceName <=> a.resourceName
      })
    }

    assertTraces(size, spec)
  }
}
