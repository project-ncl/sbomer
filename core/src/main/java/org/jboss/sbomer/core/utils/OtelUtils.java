package org.jboss.sbomer.core.utils;

import static com.redhat.resilience.otel.internal.OTelContextUtil.extractTraceState;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jboss.pnc.api.constants.MDCHeaderKeys;
import org.jboss.pnc.common.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.resilience.otel.internal.OTelContextUtil;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.api.trace.TraceStateBuilder;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import jakarta.ws.rs.container.ContainerRequestContext;

public class OtelUtils {

	private static final Logger logger = LoggerFactory.getLogger(OtelUtils.class);

	private static final String TRACEPARENT_VERSION_00 = "00";
	private static final Pattern MEMBER_LIST_PATTERN = Pattern.compile("\\s*,\\s*");

	public static SpanContext extractSpanContextFromHeaders(ContainerRequestContext requestContext) {

		SpanContext extractedSpanContext = null;
		if (requestContext == null) {
			return SpanContext.getInvalid();
		}

		// Extract the "traceparent" header
		String traceparent = requestContext.getHeaderString(MDCHeaderKeys.TRACEPARENT.getHeaderName());
		if (traceparent != null) {
			extractedSpanContext = OTelContextUtil.extractContextFromTraceParent(traceparent);
		}

		if (extractedSpanContext == null) {
			// The "traceparent" header was null, fall back to "trace-id" and "span-id" headers
			String traceId = requestContext.getHeaderString(MDCHeaderKeys.TRACE_ID.getHeaderName());
			String parentSpanId = requestContext.getHeaderString(MDCHeaderKeys.SPAN_ID.getHeaderName());
			if (parentSpanId == null) {
				// Some vendors use parent-id instead (https://www.w3.org/TR/trace-context/#parent-id)
				parentSpanId = requestContext.getHeaderString(MDCHeaderKeys.PARENT_ID.getHeaderName());
			}
			if (traceId != null && !traceId.isEmpty() && parentSpanId != null && !parentSpanId.isEmpty()) {
				extractedSpanContext = SpanContext.createFromRemoteParent(
						traceId,
						parentSpanId,
						TraceFlags.getDefault(),
						TraceState.getDefault());
			}
		}

		if (extractedSpanContext == null) {
			return SpanContext.getInvalid();
		} else if (!extractedSpanContext.isValid()) {
			return extractedSpanContext;
		}

		String traceStateValue = requestContext.getHeaderString(MDCHeaderKeys.TRACESTATE.getHeaderName());
		if (traceStateValue == null || traceStateValue.isEmpty()) {
			return extractedSpanContext;
		}

		try {
			TraceState traceState = extractTraceState(traceStateValue);
			return SpanContext.createFromRemoteParent(
					extractedSpanContext.getTraceId(),
					extractedSpanContext.getSpanId(),
					extractedSpanContext.getTraceFlags(),
					traceState);
		} catch (IllegalArgumentException e) {
			logger.debug("Unparseable tracestate header. Returning span context without state.");
			return extractedSpanContext;
		}
	}

	public static String createTraceParent(SpanContext spanContext) {
		return String.format(
				"%s-%s-%s-%s",
				TRACEPARENT_VERSION_00,
				spanContext.getTraceId(),
				spanContext.getSpanId(),
				spanContext.getTraceFlags().asHex());
	}

	public static String createTraceParent(String traceId, String spanId, String traceFlags) {

		if (Strings.isEmpty(traceId) || Strings.isEmpty(spanId)) {
			return createTraceParent(SpanContext.getInvalid());
		}

		return String.format(
				"%s-%s-%s-%s",
				TRACEPARENT_VERSION_00,
				traceId,
				spanId,
				Strings.isEmpty(traceFlags) ? TraceFlags.getDefault().asHex() : traceFlags);
	}

	public static Map<String, String> createTraceParentHeader(SpanContext spanContext) {
		return Collections.singletonMap(MDCHeaderKeys.TRACEPARENT.getHeaderName(), createTraceParent(spanContext));
	}

	public static Map<String, String> createTraceParentHeader(String traceId, String spanId, String traceFlags) {

		if (Strings.isEmpty(traceId) || Strings.isEmpty(spanId)) {
			return Collections.emptyMap();
		}

		return Collections.singletonMap(
				MDCHeaderKeys.TRACEPARENT.getHeaderName(),
				createTraceParent(
						traceId,
						spanId,
						Strings.isEmpty(traceFlags) ? TraceFlags.getDefault().asHex() : traceFlags));
	}

	public static Map<String, String> createTraceStateHeader(SpanContext spanContext) {
		return Collections.singletonMap(
				MDCHeaderKeys.TRACESTATE.getHeaderName(),
				spanContext.getTraceState()
						.asMap()
						.entrySet()
						.stream()
						.map(Objects::toString)
						.collect(Collectors.joining(",")));

	}

	public static Map<String, String> createTraceStateHeader(String membersList) {
		return Collections.singletonMap(
				MDCHeaderKeys.TRACESTATE.getHeaderName(),
				Strings.isEmpty(membersList)
						? TraceState.getDefault()
								.asMap()
								.entrySet()
								.stream()
								.map(Objects::toString)
								.collect(Collectors.joining(","))
						: membersList);

	}

	public static TraceState createTraceState(Map<String, String> membersMap) {
		TraceStateBuilder builder = TraceState.builder();
		membersMap.forEach((key, value) -> {
			builder.put(key, value);
		});
		return builder.build();
	}

	public static TraceState createTraceState(String membersList) {
		Map<String, String> membersMap = MEMBER_LIST_PATTERN.splitAsStream(membersList.trim())
				.map(s -> s.split("=", 2))
				.collect(Collectors.toMap(a -> a[0], a -> a.length > 1 ? a[1] : "", (a1, a2) -> a1));
		TraceStateBuilder builder = TraceState.builder();
		membersMap.forEach((key, value) -> {
			builder.put(key, value);
		});
		return builder.build();
	}

	public static SpanContext createSpanContextWithFallback(
			String traceId,
			String spanId,
			String traceFlagsHex,
			String traceStateMemberList,
			SpanContext fallback) {

		TraceFlags traceFlags = null;
		TraceState traceState = null;

		if (fallback == null) {
			fallback = SpanContext.getInvalid();
		}

		if (Strings.isEmpty(traceId)) {
			traceId = fallback.getTraceId();
			spanId = fallback.getSpanId();
		}
		if (!Strings.isEmpty(traceFlagsHex)) {
			traceFlags = TraceFlags.fromHex(traceFlagsHex, 0);
		} else {
			traceFlags = fallback.getTraceFlags();
		}
		if (!Strings.isEmpty(traceStateMemberList)) {
			traceState = createTraceState(traceStateMemberList);
		} else {
			traceState = fallback.getTraceState();
		}

		return SpanContext.create(traceId, spanId, traceFlags, traceState);
	}

	public static SpanBuilder buildChildSpan(
			Tracer tracer,
			String spanName,
			SpanKind spanKind,
			String parentTrace,
			String parentSpan,
			String parentTraceFlagsHex,
			String parentTraceStateMemberList,
			SpanContext fallback,
			Map<String, String> attributes) {

		SpanContext myParentContext = createSpanContextWithFallback(
				parentTrace,
				parentSpan,
				parentTraceFlagsHex,
				parentTraceStateMemberList,
				fallback);

		SpanBuilder spanBuilder = tracer.spanBuilder(spanName)
				.setParent(Context.current().with(Span.wrap(myParentContext)))
				.setSpanKind(spanKind);
		attributes.forEach((key, value) -> {
			spanBuilder.setAttribute(key, value);
		});

		return spanBuilder;
	}

}
