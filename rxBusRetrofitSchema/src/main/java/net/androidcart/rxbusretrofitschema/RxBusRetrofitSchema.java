package net.androidcart.rxbusretrofitschema;

/**
 * Created by Amin Amini on 8/23/18.
 */

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface RxBusRetrofitSchema {
    Class<?> handler() default void.class;
}
