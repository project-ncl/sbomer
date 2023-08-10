package org.jboss.sbomer.core.utils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jboss.pnc.api.constants.MDCHeaderKeys;
import org.jboss.pnc.api.constants.MDCKeys;
import org.jboss.pnc.common.Strings;
import org.jboss.pnc.common.concurrent.Sequence;
import org.slf4j.MDC;

import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.internal.shaded.jctools.queues.MessagePassingQueue.Supplier;
import jakarta.ws.rs.container.ContainerRequestContext;

public class MDCUtils {
	public static final Map<String, String> HEADER_KEY_MAPPING;
	static {
		Map<String, String> mapping = new HashMap<>();
		for (MDCHeaderKeys headerKey : MDCHeaderKeys.values()) {
			mapping.put(headerKey.getMdcKey(), headerKey.getHeaderName());
		}
		HEADER_KEY_MAPPING = Collections.unmodifiableMap(mapping);
	}

	/**
	 * Will replace MDC content with content from headers in the request context. If MDCHeaderKeys.REQUEST_CONTEXT is
	 * not provided in headers, it will generate new one.
	 */
	public static void setMDCFromRequestContext(ContainerRequestContext requestContext) {
		setMDCFromHeaders(requestContext::getHeaderString);
	}

	/**
	 * Will replace MDC content with content from headers. If MDCHeaderKeys.REQUEST_CONTEXT is not provided in headers,
	 * it will generate new one.
	 */
	public static void setMDCFromHeaders(Function<String, String> headers) {
		Map<String, String> mdcContext = new HashMap<>();

		for (MDCHeaderKeys key : MDCHeaderKeys.values()) {
			copyFromHeaders(mdcContext, key, headers);
		}
		copyFromHeaders(mdcContext, MDCHeaderKeys.REQUEST_CONTEXT, headers, () -> Sequence.nextId().toString());

		MDC.setContextMap(mdcContext);
	}

	public static void addMDCFromOtelHeadersWithFallback(
			ContainerRequestContext requestContext,
			MDCHeaderKeys traceKey,
			MDCHeaderKeys spanKey,
			MDCHeaderKeys traceFlagsKey,
			MDCHeaderKeys traceStateKey,
			SpanContext fallbackSpanContext) {

		SpanContext extractedSpanContext = OtelUtils.extractSpanContextFromHeaders(requestContext);
		if (fallbackSpanContext == null) {
			fallbackSpanContext = SpanContext.getInvalid();
		}

		if (extractedSpanContext != null && extractedSpanContext.isValid()) {
			MDC.put(traceKey.getMdcKey(), extractedSpanContext.getTraceId());
			MDC.put(spanKey.getMdcKey(), extractedSpanContext.getSpanId());
			MDC.put(traceFlagsKey.getMdcKey(), extractedSpanContext.getTraceFlags().asHex());
			MDC.put(
					traceStateKey.getMdcKey(),
					extractedSpanContext.getTraceState()
							.asMap()
							.entrySet()
							.stream()
							.map(Objects::toString)
							.collect(Collectors.joining(",")));
		} else {
			MDC.put(traceKey.getMdcKey(), fallbackSpanContext.getTraceId());
			MDC.put(spanKey.getMdcKey(), fallbackSpanContext.getSpanId());
			MDC.put(traceFlagsKey.getMdcKey(), fallbackSpanContext.getTraceFlags().asHex());
			MDC.put(
					traceStateKey.getMdcKey(),
					fallbackSpanContext.getTraceState()
							.asMap()
							.entrySet()
							.stream()
							.map(Objects::toString)
							.collect(Collectors.joining(",")));
		}
	}

	public static void addMDCFromOtelHeadersWithFallback(
			ContainerRequestContext requestContext,
			SpanContext fallbackSpanContext,
			boolean slf4jStandard) {

		String mdcTraceKey = slf4jStandard ? MDCKeys.SLF4J_TRACE_ID_KEY : MDCKeys.TRACE_ID_KEY;
		String mdcSpanKey = slf4jStandard ? MDCKeys.SLF4J_SPAN_ID_KEY : MDCKeys.SPAN_ID_KEY;
		String mdcTraceFlagsKey = slf4jStandard ? MDCKeys.SLF4J_TRACE_FLAGS_KEY : MDCKeys.TRACE_FLAGS_KEY;
		String mdcTraceStateKey = slf4jStandard ? MDCKeys.SLF4J_TRACE_STATE_KEY : MDCKeys.TRACE_STATE_KEY;

		SpanContext extractedSpanContext = OtelUtils.extractSpanContextFromHeaders(requestContext);
		if (fallbackSpanContext == null) {
			fallbackSpanContext = SpanContext.getInvalid();
		}

		if (extractedSpanContext != null && extractedSpanContext.isValid()) {
			MDC.put(mdcTraceKey, extractedSpanContext.getTraceId());
			MDC.put(mdcSpanKey, extractedSpanContext.getSpanId());
			MDC.put(mdcTraceFlagsKey, extractedSpanContext.getTraceFlags().asHex());
			MDC.put(
					mdcTraceStateKey,
					extractedSpanContext.getTraceState()
							.asMap()
							.entrySet()
							.stream()
							.map(Objects::toString)
							.collect(Collectors.joining(",")));
		} else {
			MDC.put(mdcTraceKey, fallbackSpanContext.getTraceId());
			MDC.put(mdcSpanKey, fallbackSpanContext.getSpanId());
			MDC.put(mdcTraceFlagsKey, fallbackSpanContext.getTraceFlags().asHex());
			MDC.put(
					mdcTraceStateKey,
					fallbackSpanContext.getTraceState()
							.asMap()
							.entrySet()
							.stream()
							.map(Objects::toString)
							.collect(Collectors.joining(",")));
		}
	}

	public static Map<String, String> getHeadersFromMDC() {
		Map<String, String> headers = new HashMap<>();

		for (MDCHeaderKeys key : MDCHeaderKeys.values()) {
			String value = MDC.get(key.getMdcKey());
			if (!Strings.isEmpty(value)) {
				headers.put(key.getHeaderName(), value);
			}
		}

		headers.putAll(getOtelHeadersFromMDC());
		return headers;
	}

	public static Map<String, String> getOtelHeadersFromMDC() {
		String traceId = Strings.isEmpty(MDC.get(MDCKeys.SLF4J_TRACE_ID_KEY)) ? MDC.get(MDCKeys.TRACE_ID_KEY)
				: MDC.get(MDCKeys.SLF4J_TRACE_ID_KEY);
		String spanId = Strings.isEmpty(MDC.get(MDCKeys.SLF4J_SPAN_ID_KEY)) ? MDC.get(MDCKeys.SPAN_ID_KEY)
				: MDC.get(MDCKeys.SLF4J_SPAN_ID_KEY);
		String traceFlags = Strings.isEmpty(MDC.get(MDCKeys.SLF4J_TRACE_FLAGS_KEY)) ? MDC.get(MDCKeys.TRACE_FLAGS_KEY)
				: MDC.get(MDCKeys.SLF4J_TRACE_FLAGS_KEY);
		String traceState = Strings.isEmpty(MDC.get(MDCKeys.SLF4J_TRACE_STATE_KEY)) ? MDC.get(MDCKeys.TRACE_STATE_KEY)
				: MDC.get(MDCKeys.SLF4J_TRACE_STATE_KEY);

		Map<String, String> otelHeaders = new HashMap<>();
		otelHeaders.putAll(OtelUtils.createTraceParentHeader(traceId, spanId, traceFlags));
		otelHeaders.putAll(OtelUtils.createTraceStateHeader(traceState));

		return otelHeaders;
	}

	private static void copyFromHeaders(
			Map<String, String> map,
			MDCHeaderKeys headerKeys,
			Function<String, String> valueGetter) {
		String value = valueGetter.apply(headerKeys.getHeaderName());
		if (!Strings.isEmpty(value)) {
			map.put(headerKeys.getMdcKey(), value);
		}
	}

	private static void copyFromHeaders(
			Map<String, String> map,
			MDCHeaderKeys headerKeys,
			Function<String, String> valueGetter,
			Supplier<String> defaultSupplier) {
		String value = valueGetter.apply(headerKeys.getHeaderName());
		if (Strings.isEmpty(value)) {
			map.put(headerKeys.getMdcKey(), defaultSupplier.get());
		} else {
			map.put(headerKeys.getMdcKey(), value);
		}
	}

}