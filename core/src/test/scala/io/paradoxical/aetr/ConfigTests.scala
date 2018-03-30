package io.paradoxical.aetr

import io.paradoxical.aetr.core.config.ConfigLoader

class ConfigTests extends TestBase {
  "Config" should "load" in {
    ConfigLoader.load()
  }
}
