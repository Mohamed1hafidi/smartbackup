package com.smartbackup.util;

import com.smartbackup.exception.BadRequestException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Locale;
import java.util.Set;

public final class PageableUtil {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("createdAt", "originalName", "fileSize");
    private static final int MAX_PAGE_SIZE = 100;

    private PageableUtil() {
    }

    public static Pageable create(int page, int size, String sort, String direction) {
        return create(page, size, sort, direction, MAX_PAGE_SIZE);
    }

    public static Pageable create(
            int page, int size, String sort, String direction, int maxPageSize) {
        if (page < 0) {
            throw new BadRequestException("Le numéro de page doit être supérieur ou égal à 0");
        }
        if (size < 1 || size > maxPageSize) {
            throw new BadRequestException("La taille de page doit être comprise entre 1 et " + maxPageSize);
        }
        if (!ALLOWED_SORT_FIELDS.contains(sort)) {
            throw new BadRequestException("Champ de tri invalide : " + sort);
        }

        Sort.Direction sortDirection = switch (direction.toLowerCase(Locale.ROOT)) {
            case "asc" -> Sort.Direction.ASC;
            case "desc" -> Sort.Direction.DESC;
            default -> throw new BadRequestException("Direction de tri invalide : " + direction);
        };

        return PageRequest.of(page, size, Sort.by(sortDirection, sort));
    }
}
