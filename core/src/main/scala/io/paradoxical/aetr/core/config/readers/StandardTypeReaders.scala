package io.paradoxical.aetr.core.config.readers

import com.typesafe.config.Config
import java.io.File
import net.ceedubs.ficus.readers.ValueReader
import scala.util.matching.Regex

trait StandardTypeReaders {
  implicit val file = new ValueReader[File] {
    override def read(config: Config, path: String): File = {
      new File(config.getString(path))
    }
  }

  implicit val regexReader = new ValueReader[Regex] {
    override def read(config: Config, path: String): Regex = {
      config.getString(path).r
    }
  }
}
