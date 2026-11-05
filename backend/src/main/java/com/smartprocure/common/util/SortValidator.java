package com.smartprocure.common.util;

import com.smartprocure.common.exception.BusinessException;
import com.smartprocure.common.exception.ErrorCode;

import java.util.Set;

/**
 * Validates that a client-supplied {@code sort} field name is in the
 * allow-listed set of sortable fields for a given resource.
 *
 * <p>Prevents Spring Data from throwing a PropertyReferenceException (500)
 * when an unknown or malicious field name is passed as a query parameter.</p>
 */
public final class SortValidator {

    private SortValidator() {}

    /**
     * Throws {@link BusinessException} with {@code INVALID_SORT_FIELD} if
     * {@code field} is not present in {@code allowed}.
     *
     * @param field   the sort field supplied by the client
     * @param allowed the allow-listed field names for this resource
     */
    public static void validate(String field, Set<String> allowed) {
        if (!allowed.contains(field)) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR,
                    "Invalid sort field '" + field + "'. Allowed: " + allowed);
        }
    }

    /**
     * Parse the {@code dir} query param into a Spring Sort.Direction.
     * Defaults to DESCENDING for any unrecognised value.
     */
    public static org.springframework.data.domain.Sort.Direction direction(String dir) {
        return "asc".equalsIgnoreCase(dir)
                ? org.springframework.data.domain.Sort.Direction.ASC
                : org.springframework.data.domain.Sort.Direction.DESC;
    }
}
