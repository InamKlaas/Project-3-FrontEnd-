package com.cputhome.seed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/* dev boots grow a demo world, prod never runs this */
@Component
@Profile("dev")
public class DevSeedRunner implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(DevSeedRunner.class);

  private final SeedService seed;

  public DevSeedRunner(SeedService seed) {
    this.seed = seed;
  }

  @Override
  public void run(ApplicationArguments args) {
    SeedService.SeedReport report = seed.seed();
    log.info(
        "demo seed ready: {} users, {} properties ({} new), {} messages",
        report.users(), report.accommodations(), report.created(), report.messages());
  }
}
