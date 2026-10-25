package com.volunnear.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.customizers.PropertyCustomizer;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Q67: every property of a schema that appears in a response is marked required, because a response
 * record always serialises every component. A component that can be null is annotated
 * {@code @Schema(nullable = true)}, so the generated TypeScript type is {@code T | null} instead of {@code T?}.
 * Request schemas keep springdoc's rule (required only with {@code @NotNull} and friends), because a partial
 * update really may leave fields out.
 */
@Configuration
public class OpenApiConfig {
    private static final String SCHEMA_REF_PREFIX = "#/components/schemas/";

    static {
        // Jackson writes LocalTime as "08:00:00", but springdoc would describe it as an object {hour, minute, ...}.
        SpringDocUtils.getConfig().replaceWithSchema(LocalTime.class, new StringSchema().format("time").example("08:00:00"));
    }

    @Bean
    public OpenApiCustomizer responseFieldsRequired() {
        return openApi -> {
            Map<String, Schema> schemas = openApi.getComponents() == null ? null : openApi.getComponents().getSchemas();
            if (schemas == null) {
                return;
            }
            for (String name : responseSchemaNames(openApi, schemas)) {
                Schema<?> schema = schemas.get(name);
                if (schema != null && schema.getProperties() != null) {
                    schema.setRequired(new ArrayList<>(schema.getProperties().keySet()));
                }
            }
        };
    }

    // OpenAPI 3.0 ignores "nullable" next to a $ref, and swagger-core drops it. A reference to another schema
    // annotated @Schema(nullable = true) is therefore wrapped as allOf: [$ref] with nullable: true.
    @Bean
    public PropertyCustomizer nullableReferences() {
        return (property, type) -> {
            boolean annotatedNullable = type.getCtxAnnotations() != null && Arrays.stream(type.getCtxAnnotations())
                    .anyMatch(a -> a instanceof io.swagger.v3.oas.annotations.media.Schema s && s.nullable());
            if (property == null || property.get$ref() == null || !annotatedNullable) {
                return property;
            }
            Schema<?> reference = new Schema<>().$ref(property.get$ref());
            ComposedSchema wrapped = new ComposedSchema();
            wrapped.setAllOf(List.of(reference));
            wrapped.setNullable(true);
            return wrapped;
        };
    }

    // The named schemas reachable from any response body, following properties and array items.
    private static Set<String> responseSchemaNames(OpenAPI openApi, Map<String, Schema> schemas) {
        Deque<Schema<?>> toVisit = new ArrayDeque<>();
        if (openApi.getPaths() != null) {
            openApi.getPaths().values().stream()
                    .flatMap(path -> path.readOperations().stream())
                    .map(Operation::getResponses)
                    .filter(Objects::nonNull)
                    .flatMap(responses -> responses.values().stream())
                    .filter(response -> response.getContent() != null)
                    .flatMap(response -> response.getContent().values().stream())
                    .map(mediaType -> mediaType.getSchema())
                    .filter(Objects::nonNull)
                    .forEach(toVisit::add);
        }

        Set<String> names = new HashSet<>();
        while (!toVisit.isEmpty()) {
            Schema<?> schema = toVisit.pop();
            String ref = schema.get$ref();
            if (ref != null && ref.startsWith(SCHEMA_REF_PREFIX)) {
                String name = ref.substring(SCHEMA_REF_PREFIX.length());
                if (names.add(name) && schemas.containsKey(name)) {
                    toVisit.push(schemas.get(name));
                }
                continue;
            }
            if (schema.getItems() != null) {
                toVisit.push(schema.getItems());
            }
            if (schema.getProperties() != null) {
                schema.getProperties().values().forEach(toVisit::push);
            }
            if (schema.getAllOf() != null) {
                schema.getAllOf().forEach(toVisit::push);
            }
        }
        return names;
    }
}
