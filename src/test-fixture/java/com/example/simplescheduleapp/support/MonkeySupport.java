package com.example.simplescheduleapp.support;

import com.navercorp.fixturemonkey.FixtureMonkey;
import com.navercorp.fixturemonkey.api.introspector.ConstructorPropertiesArbitraryIntrospector;
import com.navercorp.fixturemonkey.api.introspector.FailoverIntrospector;
import com.navercorp.fixturemonkey.api.introspector.FieldReflectionArbitraryIntrospector;
import com.navercorp.fixturemonkey.jakarta.validation.plugin.JakartaValidationPlugin;

import java.util.List;

public class MonkeySupport {

    protected final FixtureMonkey sut = FixtureMonkey.builder()
            .objectIntrospector(new FailoverIntrospector(
                    List.of(
                            FieldReflectionArbitraryIntrospector.INSTANCE,
                            ConstructorPropertiesArbitraryIntrospector.INSTANCE
                    )
            ))
            .plugin(new JakartaValidationPlugin())
            .defaultNotNull(true)
            .build();
}
