package com.frezo.server.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Centralized MapStruct configuration.
 *
 * All MapStruct mappers use componentModel = "spring", so the generated
 * *MapperImpl classes are annotated with @Component. This config explicitly
 * scans all mapper packages to ensure Spring registers every generated
 * mapper bean (e.g. ProductMapperImpl, CustomerMapperImpl, etc.)
 * even across multi-module boundaries.
 */
@Configuration
@ComponentScan(basePackages = {
        "com.frezo.product.mapper",
        "com.frezo.customer.mapper",
        "com.frezo.qlns.mapper",
        "com.frezo.qtht.mapper",
        "com.frezo.qtbv.mapper",
        "com.frezo.task.mapper",
        "com.frezo.email.mapper",
        "com.frezo.common.mapper"
})
public class MapStructConfig {
}
