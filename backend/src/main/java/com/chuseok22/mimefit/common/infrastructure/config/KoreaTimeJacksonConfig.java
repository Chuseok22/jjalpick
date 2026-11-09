package com.chuseok22.mimefit.common.infrastructure.config;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.StdSerializer;

/**
 * 요청·응답 본문의 {@link Instant}를 KST 오프셋이 붙은 ISO-8601 문자열로 변환하는 설정이다.
 * <p>
 * 서버 내부는 {@link Instant}를 쓰고, 변환은 이 설정에서만 한다. 쓸 때는 {@code 2026-11-09T14:00:00+09:00}
 * 형태로 내보내고, 읽을 때는 오프셋이 있는 ISO-8601 문자열만 받는다. 오프셋이 없는 값과 에포크 숫자·숫자 문자열은
 * {@code DatabindException}으로 실패하며 요청 본문 변환 단계에서 400이 된다.
 * <p>
 * 이 설정은 요청 본문(JSON)의 {@link Instant}에만 적용된다. {@code @RequestParam Instant}는 Spring 변환기가
 * 처리하며, 오프셋이 없는 값은 {@code MethodArgumentTypeMismatchException}이 되어 전역 예외 처리기가
 * {@code INVALID_REQUEST}(400)로 변환한다.
 */
@Configuration
public class KoreaTimeJacksonConfig {

    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter KOREA_OFFSET_FORMATTER =
        DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(KOREA_ZONE);

    @Bean
    public JsonMapperBuilderCustomizer koreaTimeJsonMapperBuilderCustomizer() {
        SimpleModule module = new SimpleModule("KoreaTimeModule");
        module.addSerializer(Instant.class, new KoreaOffsetInstantSerializer());
        module.addDeserializer(Instant.class, new OffsetRequiredInstantDeserializer());
        return builder -> builder.addModule(module);
    }

    private static class KoreaOffsetInstantSerializer extends StdSerializer<Instant> {

        KoreaOffsetInstantSerializer() {
            super(Instant.class);
        }

        @Override
        public void serialize(Instant value, JsonGenerator generator, SerializationContext context) {
            generator.writeString(KOREA_OFFSET_FORMATTER.format(value));
        }

    }

    private static class OffsetRequiredInstantDeserializer extends StdDeserializer<Instant> {

        OffsetRequiredInstantDeserializer() {
            super(Instant.class);
        }

        @Override
        public Instant deserialize(JsonParser parser, DeserializationContext context) {
            if (!parser.hasToken(JsonToken.VALUE_STRING)) {
                return context.reportInputMismatch(Instant.class, "오프셋이 있는 ISO-8601 문자열만 허용한다.");
            }
            String text = parser.getString();
            try {
                return OffsetDateTime.parse(text, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant();
            } catch (DateTimeParseException exception) {
                return (Instant) context.handleWeirdStringValue(
                    Instant.class, text, "오프셋이 있는 ISO-8601 형식이 아니다.");
            }
        }

    }

}
