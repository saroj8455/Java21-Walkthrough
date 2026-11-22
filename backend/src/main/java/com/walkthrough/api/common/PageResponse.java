package com.walkthrough.api.common;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Stable JSON shape for paged results. Generic: {@code PageResponse<T>} works like {@code Page<T>} in TypeScript.
 *
 * <p>Why not return Spring's {@code Page} directly? Its JSON contains internal details and Spring Data
 * itself warns that its serialized form is not a stable contract. A small record is explicit and documented.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    /**
     * Converts a Spring {@link Page} of entities into a page of DTOs.
     * {@code <E, T>} declares two type parameters for this static method (E = entity type, T = DTO type).
     */
    public static <E, T> PageResponse<T> from(Page<E> page, Function<E, T> mapper) {
        return new PageResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
