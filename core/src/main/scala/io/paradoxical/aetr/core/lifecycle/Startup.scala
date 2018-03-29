package io.paradoxical.aetr.core.lifecycle

import io.paradoxical.aetr.core.execution.Advancer
import javax.inject.Inject

/**
 * On startup try and advance whatever we can.
 *
 * If the service crashes, or has other transient errors
 * this should catch any orphaned failures
 * @param advancer
 */
class Startup @Inject()(advancer: Advancer) {
  def start(): Unit = {
    advancer.advanceAll()
  }
}
