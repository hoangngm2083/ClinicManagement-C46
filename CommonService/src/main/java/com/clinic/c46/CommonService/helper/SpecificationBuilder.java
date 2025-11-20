package com.clinic.c46.CommonService.helper;

import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public interface SpecificationBuilder {

    <T> Specification<T> fieldLike(String field, String value);

    <T> Specification<T> fieldEquals(String field, Object value);

    <T> Specification<T> keyword(String keyword, List<String> fields);

    <T, F extends Comparable<? super F>> Specification<T> fromTo(String field, Class<F> fieldType, F from,
            F to);
}
