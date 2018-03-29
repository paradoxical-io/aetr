package io.paradoxical.aetr.core.lifecycle

import io.paradoxical.aetr.core.execution.Advancer
import javax.inject.Inject

class Startup @Inject()(advancer: Advancer) {
  def start(): Unit = {
    advancer.advanceAll()
  }
}
