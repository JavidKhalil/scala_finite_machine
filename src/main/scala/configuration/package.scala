import cats.effect.{Blocker, ContextShift, IO, Resource}
import com.typesafe.config.ConfigFactory
import pureconfig._
import pureconfig.generic.auto._
import pureconfig.module.catseffect.syntax._


package object configuration {
  case class HttpServerConfiguration(host: String, port: Int)

  case class DatabaseConfiguration(driver: String, url: String, user: String, password: String, threadPoolSize: Int)

  case class Configuration(server: HttpServerConfiguration, database: DatabaseConfiguration)

  object Configuration {
    def load(configFile: String = "application.conf")(implicit cs: ContextShift[IO]): Resource[IO, Configuration] = {
      Blocker[IO].flatMap { block =>
        Resource.liftF(ConfigSource.fromConfig(ConfigFactory.load(configFile)).loadF[IO, Configuration](block))
      }
    }
  }
}
