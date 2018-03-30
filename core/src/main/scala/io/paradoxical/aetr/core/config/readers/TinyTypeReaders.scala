package io.paradoxical.aetr.core.config.readers

import com.typesafe.config.{Config, ConfigUtil}
import io.paradoxical.global.tiny._
import java.util.UUID
import net.ceedubs.ficus.readers.ValueReader
import scala.collection.JavaConverters._
import scala.reflect.ClassTag
import scala.reflect.runtime.universe._
import scala.util.control.NonFatal

trait TinyTypeReaders {
  private lazy val mirror = runtimeMirror(getClass.getClassLoader)

  implicit def tinyStringReader[T <: StringValue : ClassTag] = new ValueReader[T] {
    override def read(config: Config, path: String): T = {
      tiny[String, T](config.getString(path))
    }
  }

  implicit def tinyLongReader[T <: LongValue : ClassTag] = new ValueReader[T] {
    override def read(config: Config, path: String): T = {
      tiny[Long, T](config.getLong(path))
    }
  }

  implicit def tinyIntReader[T <: IntValue : ClassTag] = new ValueReader[T] {
    override def read(config: Config, path: String): T = {
      tiny[Int, T](config.getInt(path))
    }
  }

  implicit def tinyDoubleReader[T <: DoubleValue : ClassTag] = new ValueReader[T] {
    override def read(config: Config, path: String): T = {
      tiny[Double, T](config.getDouble(path))
    }
  }

  implicit def tinyUuidValue[T <: UuidValue : ClassTag] = new ValueReader[T] {
    override def read(config: Config, path: String): T = {
      tiny[UUID, T](UUID.fromString(config.getString(path)))
    }
  }

  implicit def tinyTypeMapReader[Key <: StringValue : ClassTag, A](implicit entryReader: ValueReader[A]): ValueReader[Map[Key, A]] = new ValueReader[Map[Key, A]] {
    def read(config: Config, path: String): Map[Key, A] = {
      val relativeConfig = config.getConfig(path)
      relativeConfig.root().entrySet().asScala map { entry =>
        val key = entry.getKey
        tiny[String, Key](key) -> entryReader.read(relativeConfig, ConfigUtil.quoteString(key))
      } toMap
    }
  }

  private def tiny[S: ClassTag : TypeTag, T <: ValueType[S] : ClassTag](source: S): T = {
    val instanceUsingApply = for {
      companion <- companionObjectMirror[T]
      applyMethod <- moduleApplyMethod(companion)
      if hasApplyMethodWithRequiredArg[S](applyMethod)
    } yield mirror.reflect(companion.instance).reflectMethod(applyMethod).apply(source).asInstanceOf[T]

    instanceUsingApply.getOrElse {
      runtimeClass[T].
        getConstructor(runtimeClass[S]).
        newInstance(Array(source.asInstanceOf[Object]): _*).
        asInstanceOf[T]
    }

  }

  private def runtimeClass[T: ClassTag] = implicitly[ClassTag[T]].runtimeClass

  private def companionObjectMirror[T : ClassTag] = {
    try {
      val moduleSymbol = mirror.staticModule(runtimeClass[T].getName)
      Some(mirror.reflectModule(moduleSymbol))
    } catch {
      case NonFatal(_) => None
    }
  }

  private def moduleApplyMethod(moduleMirror: ModuleMirror): Option[MethodSymbol] = {
    moduleMirror.symbol.typeSignature.member(TermName("apply")) match {
      case NoSymbol => None
      case s if s.isMethod => Some(s.asMethod)
      case _ => None
    }
  }

  private def hasApplyMethodWithRequiredArg[S : TypeTag](methodSymbol: MethodSymbol): Boolean = {
    methodSymbol.paramLists.exists(list => list.size == 1 && list.head.typeSignature =:= typeOf[S])
  }
}

object TinyTypeReaders extends TinyTypeReaders
