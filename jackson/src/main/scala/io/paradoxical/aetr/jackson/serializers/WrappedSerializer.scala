package io.paradoxical.aetr.jackson.serializers

import com.fasterxml.jackson.core.{JsonGenerator, JsonParser}
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.databind.deser.{Deserializers, KeyDeserializers}
import com.fasterxml.jackson.databind.ser.Serializers
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.fasterxml.jackson.module.scala.JacksonModule
import io.paradoxical.aetr.global.tiny._
import java.util.UUID
import scala.collection.JavaConverters._

object TypeSerializerModule extends JacksonModule {
  override def getModuleName = "TypeSerializerModule"

  this += {_.addDeserializers(TypeDeSerializerLocator)}
  this += {_.addSerializers(TypeSerializerLocator)}
  this += {_.addKeyDeserializers(TypeKeyDeserializerLocator)}
}

object TypeDeSerializerLocator extends Deserializers.Base {
  override def findBeanDeserializer(javaType: JavaType, config: DeserializationConfig, beanDesc: BeanDescription): JsonDeserializer[_] = {
    if (classOf[ValueType[_]].isAssignableFrom(javaType.getRawClass)) {
      new TypeDeserializer(javaType)
    } else {
      null
    }
  }
}

object TypeKeyDeserializerLocator extends KeyDeserializers {
  override def findKeyDeserializer(javaType: JavaType, config: DeserializationConfig, beanDesc: BeanDescription): KeyDeserializer = {
    if (classOf[ValueType[_]].isAssignableFrom(javaType.getRawClass)) {
      new TypeKeyDeserializer(javaType)
    } else {
      null
    }
  }
}

object TypeSerializerLocator extends Serializers.Base {
  override def findSerializer(config: SerializationConfig, javaType: JavaType, beanDesc: BeanDescription): JsonSerializer[_] = {
    if (classOf[ValueType[_]].isAssignableFrom(javaType.getRawClass)) {
      new TypeSerializer(javaType)
    } else {
      null
    }
  }
}

class TypeKeyDeserializer(javaType: JavaType) extends KeyDeserializer {
  override def deserializeKey(key: String, ctxt: DeserializationContext): AnyRef = {
    val interfaces = javaType.getInterfaces.asScala.toList.find(m => classOf[ValueType[_]].isAssignableFrom(m.getRawClass)).head.getRawClass

    val targetValue =
      if (interfaces.isAssignableFrom(classOf[StringValue])) key
      else if (interfaces.isAssignableFrom(classOf[LongValue])) key.toLong
      else if (interfaces.isAssignableFrom(classOf[IntValue])) key.toInt
      else if (interfaces.isAssignableFrom(classOf[DoubleValue])) key.toDouble
      else if (interfaces.isAssignableFrom(classOf[FloatValue])) key.toFloat
      else if (interfaces.isAssignableFrom(classOf[UuidValue])) UUID.fromString(key)
      else throw new RuntimeException(s"Unknown wrapper type! ${javaType.toString}")

    val args: Array[Object] = Seq(targetValue.asInstanceOf[Object]).toArray

    javaType.getRawClass.getConstructors.head.newInstance(args: _*).asInstanceOf[Object]
  }
}

class TypeSerializer(javaType: JavaType) extends StdSerializer[ValueType[_]](classOf[ValueType[_]]) {
  override def serialize(value: ValueType[_], gen: JsonGenerator, provider: SerializerProvider): Unit = {
    value match {
      case v: StringValue => gen.writeString(v.value)
      case v: LongValue => gen.writeNumber(v.value)
      case v: IntValue => gen.writeNumber(v.value)
      case v: DoubleValue => gen.writeNumber(v.value)
      case v: FloatValue => gen.writeNumber(v.value)
      case v: UuidValue => gen.writeObject(v.value)
    }
  }
}

class TypeDeserializer(javaType: JavaType) extends JsonDeserializer[AnyRef] {
  override def deserialize(jp: JsonParser, context: DeserializationContext): Object = {
    val interfaces = javaType.getInterfaces.asScala.toList.find(m => classOf[ValueType[_]].isAssignableFrom(m.getRawClass)).head.getRawClass

    val inner =
      if (interfaces.isAssignableFrom(classOf[StringValue])) classOf[String]
      else if (interfaces.isAssignableFrom(classOf[LongValue])) classOf[Long]
      else if (interfaces.isAssignableFrom(classOf[IntValue])) classOf[Int]
      else if (interfaces.isAssignableFrom(classOf[DoubleValue])) classOf[Double]
      else if (interfaces.isAssignableFrom(classOf[FloatValue])) classOf[Float]
      else if (interfaces.isAssignableFrom(classOf[UuidValue])) classOf[UUID]
      else throw new RuntimeException(s"Unknown wrapper type! ${javaType.toString}")

    val args: Array[Object] = Seq(jp.getCodec.readValue(jp, inner).asInstanceOf[Object]).toArray

    javaType.getRawClass.getConstructors.head.newInstance(args: _*).asInstanceOf[Object]
  }
}
