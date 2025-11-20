package com.clinic.c46.CommonService.helper;

import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;


@Service
@NoArgsConstructor
public class SpecificationBuilderImpl implements SpecificationBuilder {

    @Override
    public <T> Specification<T> keyword(String keyword, List<String> fields) {
        if (keyword == null || keyword.isBlank()) {
            return Specification.allOf();
        }

        String lowerKeyword = "%" + keyword.toLowerCase() + "%";

        return (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            for (String field : fields) {
                predicates.add(cb.like(cb.lower(root.get(field)), lowerKeyword));
            }
            return cb.or(predicates.toArray(new Predicate[0]));
        };
    }

    public <T, F extends Comparable<? super F>> Specification<T> fromTo(String field, Class<F> fieldType, F from,
            F to) {

        Specification<T> specFrom = (root, cq, cb) -> {
            if (from == null) {
                return null;
            }
            Path<F> fieldPath = root.get(field);

            return cb.greaterThanOrEqualTo(fieldPath, from);
        };

        Specification<T> specTo = (root, cq, cb) -> {
            if (to == null) {
                return null;
            }
            Path<F> fieldPath = root.get(field);

            return cb.lessThanOrEqualTo(fieldPath, to);
        };

        return Specification.allOf(specFrom, specTo);
    }

    @Override
    public <T> Specification<T> fieldEquals(String field, Object value) {
        if (value == null) return Specification.allOf();
        return (root, cq, cb) -> cb.equal(root.get(field), value);
    }

    @Override
    public <T> Specification<T> fieldLike(String field, String value) {
        if (value == null || value.isBlank()) return Specification.allOf();
        String lowerValue = "%" + value.toLowerCase() + "%";
        return (root, cq, cb) -> cb.like(cb.lower(root.get(field)), lowerValue);
    }
}
