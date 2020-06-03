package pers.z950.common

import com.fasterxml.jackson.databind.ObjectMapper
import io.vertx.core.json.jackson.DatabindCodec

object Mapper {
  val jackson: ObjectMapper = DatabindCodec.mapper()
}
