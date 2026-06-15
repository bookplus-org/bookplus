package com.bookplus.auth.shared.annotation;

import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * Estereotipo para los adaptadores web (REST Controllers).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface WebAdapter {
    @AliasFor(annotation = Component.class)
    String value() default "";
}
