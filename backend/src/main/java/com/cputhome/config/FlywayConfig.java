package com.cputhome.config;

import javax.sql.DataSource;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/* Boot 4 ships no Flyway auto-configuration, so migrations run here.
 * the post-processor pins JPA behind the flyway bean — validation must
 * never see an unmigrated schema. tests use create-drop instead. */
@Configuration
@Profile("!test")
public class FlywayConfig {

  @Bean(initMethod = "migrate")
  public org.flywaydb.core.Flyway flyway(DataSource dataSource) {
    return org.flywaydb.core.Flyway.configure()
        .dataSource(dataSource)
        .locations("classpath:db/migration")
        .baselineOnMigrate(true)
        .validateOnMigrate(true)
        .load();
  }

  @Bean
  public static BeanFactoryPostProcessor flywayBeforeJpa() {
    return (ConfigurableListableBeanFactory beanFactory) -> {
      /* Boot names it entityManagerFactory; never touch &-prefixed dereferences */
      if (!beanFactory.containsBeanDefinition("entityManagerFactory")) {
        return;
      }
      String[] dependsOn = beanFactory.getBeanDefinition("entityManagerFactory").getDependsOn();
      String[] next =
          dependsOn == null
              ? new String[] {"flyway"}
              : java.util.Arrays.copyOf(dependsOn, dependsOn.length + 1);
      if (dependsOn != null) {
        next[dependsOn.length] = "flyway";
      }
      beanFactory.getBeanDefinition("entityManagerFactory").setDependsOn(next);
    };
  }
}
