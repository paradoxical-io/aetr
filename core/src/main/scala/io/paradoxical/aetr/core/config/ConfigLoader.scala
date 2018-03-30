package io.paradoxical.aetr.core.config

import com.typesafe.config.ConfigFactory
import io.paradoxical.aetr.core.config.readers.StandardTypeReaders
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.AllValueReaderInstances

object ConfigLoader extends AllValueReaderInstances with StandardTypeReaders {
  def load(): ServiceConfig = {
    ConfigFactory.load().getConfig("aetr").as[ServiceConfig]
  }
}

