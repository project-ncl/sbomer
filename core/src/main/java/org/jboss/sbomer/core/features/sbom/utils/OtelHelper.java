/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2023 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.sbomer.core.features.sbom.utils;

import static org.jboss.sbomer.core.features.sbom.utils.MDCUtils.MDC_SPAN_ID_KEY;
import static org.jboss.sbomer.core.features.sbom.utils.MDCUtils.MDC_TRACE_FLAGS_KEY;
import static org.jboss.sbomer.core.features.sbom.utils.MDCUtils.MDC_TRACE_ID_KEY;
import static org.jboss.sbomer.core.features.sbom.utils.MDCUtils.MDC_TRACE_STATE_KEY;

import java.util.Map;
import java.util.function.Supplier;

import org.jboss.pnc.common.otel.OtelUtils;
import org.slf4j.MDC;

import com.redhat.resilience.otel.internal.EnvarExtractingPropagator;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

/**
 * Helper class to make setup / teardown of OpenTelemetry tracing simpler for CLI tools.
 */
@Slf4j
@UtilityClass
public class OtelHelper {
    private SpanProcessor spanProcessor;

    private Span root = null;

    /**
     * Setup a {@link OtlpGrpcSpanExporter} exporter with the given endpoint.
     *
     * @param endpoint The gRPC endpoint for sending span data
     * @return The {@link OtlpGrpcSpanExporter} instance
     */
    public SpanExporter defaultSpanExporter(String endpoint) {
        return OtlpGrpcSpanExporter.builder().setEndpoint(endpoint).build();
    }

    /**
     * Setup a {@link BatchSpanProcessor} with the supplied {@link SpanExporter}.
     *
     * @param exporter The {@link SpanExporter}, which MAY come from {@link OtelHelper#defaultSpanExporter}
     * @return The {@link BatchSpanProcessor} instance
     */
    public SpanProcessor defaultSpanProcessor(SpanExporter exporter) {
        return BatchSpanProcessor.builder(exporter).build();
    }

    /**
     * Setup {@link GlobalOpenTelemetry} using the provided service name and span processor (which contains an
     * exporter). This will also ininitialize with the {@link EnvarExtractingPropagator} context propagator, which knows
     * how to set W3C HTTP headers for propagating context downstream, but reads similar fields from
     * {@link System#getenv()}.
     * <p>
     * When the {@link GlobalOpenTelemetry} setup is done, <b>this method will also start a root span</b>, which enables
     * the CLI execution to use {@link Span#current()} to set attributes directly with no further setup required.
     *
     * @param serviceName This translates into 'service.name' in the span, which is usually required for span validity
     * @param commandName This is used to name the new span
     * @param processor This is a span processor that determines how spans are exported
     */
    public void startOTel(
            String serviceName,
            String commandName,
            SpanProcessor processor,
            Map<String, String> attributes) {

        if (spanProcessor != null) {
            throw new IllegalStateException("startOTel has already been called");
        }
        if (serviceName == null) {
            throw new RuntimeException("serviceName must be passed in");
        }
        if (commandName == null) {
            commandName = serviceName;
        }

        spanProcessor = processor;

        AttributesBuilder attrBuilder = Attributes.builder().put(ResourceAttributes.SERVICE_NAME, serviceName);
        Resource resource = Resource.getDefault().merge(Resource.create(attrBuilder.build()));
        SdkTracerProvider sdkTracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(processor)
                .setResource(resource)
                .build();

        // NOTE the use of EnvarExtractingPropagator here
        OpenTelemetrySdk openTelemetry = OpenTelemetrySdk.builder()
                .setTracerProvider(sdkTracerProvider)
                .setPropagators(ContextPropagators.create(EnvarExtractingPropagator.getInstance()))
                .buildAndRegisterGlobal();

        Context parentContext = EnvarExtractingPropagator.getInstance().extract(Context.current(), null, null);

        SpanBuilder spanBuilder = openTelemetry.getTracer(serviceName)
                .spanBuilder(commandName)
                .setParent(parentContext);
        attributes.forEach((key, value) -> {
            spanBuilder.setAttribute(key, value);
        });

        root = spanBuilder.startSpan();

        root.makeCurrent();
        log.debug(
                "Running with traceId {} spanId {}",
                Span.current().getSpanContext().getTraceId(),
                Span.current().getSpanContext().getSpanId());
    }

    public boolean otelEnabled() {
        return spanProcessor != null;
    }

    /**
     * Shutdown the span processor, giving it some time to flush any pending spans out to the exporter.
     */
    public void stopOTel() {
        if (otelEnabled()) {
            log.debug("Finishing OpenTelemetry instrumentation for {}", root);
            if (root != null) {
                root.end();
            }
            spanProcessor.close();
            spanProcessor = null;
        }
    }

    /**
     * Executes an action within an OpenTelemetry span context.
     *
     * <p>
     * This method sets up a span with a given name (based on spanSuffix), logs its trace identifiers, and ensures that
     * the span is made current during the execution of the provided action. The span is properly ended after the action
     * completes, regardless of success or failure.
     * </p>
     *
     * <p>
     * This is useful for ensuring consistent tracing and logging across multiple reconcile phases, while avoiding code
     * duplication related to span creation and lifecycle management.
     * </p>
     *
     * @param <T> the type of the resource being reconciled, must extend
     *        {@link io.fabric8.kubernetes.api.model.HasMetadata}
     * @param spanSuffix a suffix used to name the span and log context
     * @param spanAttributes the attributes to be associated with the span
     * @param mdcContext the MDC context map
     * @param action a functional interface containing the logic to execute within the span context
     * @return an {@link io.javaoperatorsdk.operator.api.reconciler.UpdateControl} result from the reconciliation action
     */
    public <R> R withSpan(
            Class<?> callerClass,
            String spanSuffix,
            Map<String, String> spanAttributes,
            Map<String, String> mdcContext,
            Supplier<R> action) {

        String traceId = mdcContext.get(MDC_TRACE_ID_KEY);
        String spanId = mdcContext.get(MDC_SPAN_ID_KEY);
        String traceFlags = mdcContext.getOrDefault(MDC_TRACE_FLAGS_KEY, "01");
        String traceState = mdcContext.get(MDC_TRACE_STATE_KEY);

        SpanContext parentSpanContext = Span.current().getSpanContext();
        if (!parentSpanContext.isValid() && traceId != null && spanId != null) {
            parentSpanContext = SpanContext.createFromRemoteParent(
                    traceId,
                    spanId,
                    TraceFlags.fromHex(traceFlags, 0),
                    TraceState.getDefault());
        }

        Context parentContext = Context.root().with(Span.wrap(parentSpanContext));

        SpanBuilder spanBuilder = OtelUtils
                .buildChildSpan(
                        GlobalOpenTelemetry.get().getTracer(""),
                        getEffectiveClassName(callerClass) + spanSuffix,
                        SpanKind.CLIENT,
                        traceId,
                        spanId,
                        traceFlags,
                        traceState,
                        parentSpanContext,
                        spanAttributes)
                .setParent(parentContext);

        Span span = spanBuilder.startSpan();

        log.debug(
                "Started a new span context with traceId: {}, spanId: {}, traceFlags: {}",
                span.getSpanContext().getTraceId(),
                span.getSpanContext().getSpanId(),
                span.getSpanContext().getTraceFlags().asHex());

        try (Scope scope = span.makeCurrent()) {
            return action.get();
        } catch (Throwable t) {
            span.recordException(t);
            span.setStatus(StatusCode.ERROR, t.getMessage());
            throw t;
        } finally {
            span.end();
        }
    }

    public static String getEffectiveClassName(Object obj) {
        return getEffectiveClassName(obj.getClass());
    }

    public static String getEffectiveClassName(Class<?> clazz) {
        while (clazz.getSimpleName().matches(".*_\\w+")) {
            clazz = clazz.getSuperclass();
        }
        return clazz.getSimpleName();
    }

}
