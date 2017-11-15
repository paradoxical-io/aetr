//
// Copyright (c) 2011-2017 by Curalate, Inc.
//

package io.paradoxical.aetr.jackson.model

import com.fasterxml.jackson.annotation.{JsonAnyGetter, JsonAnySetter, JsonIgnore}
import java.util
import scala.collection.JavaConverters._

/**
 * Mixin trait to support deserializing arbitrary JSON into the map. This can be used to strongly type _some_ fields
 * and have a catch-all bag for all the other fields
 */
trait ArbitraryJson {
  private var _map = new util.HashMap[String, Object]()

  @JsonAnyGetter
  def getMap: util.HashMap[String, Object] = _map

  @JsonAnySetter def set(key: String, obj: Object): Unit = {
    _map.put(key, obj)
  }

  /**
   * The arbitrary data
   * @return
   */
  @JsonIgnore
  def arbitrary = _map.asScala

  @JsonIgnore
  def setMap(map: util.HashMap[String, Object]): Unit = {
    _map = map
  }

  @JsonIgnore
  def +=(data: (String, Object)*): Unit = {
    data.foreach {
      case (k, v) => _map.put(k, v)
    }
  }
}