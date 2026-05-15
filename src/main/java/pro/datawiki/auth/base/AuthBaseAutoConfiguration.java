package pro.datawiki.auth.base;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Auto-configuration entry point for auth-base library.
 * Consuming applications just add this lib as dependency — beans, entities
 * and repositories are registered automatically.
 */
@AutoConfiguration
@ComponentScan(basePackages = "pro.datawiki.auth.base")
@EnableJpaRepositories(basePackages = "pro.datawiki.auth.base.repository")
@EntityScan(basePackages = "pro.datawiki.auth.base.domain")
public class AuthBaseAutoConfiguration {
}
