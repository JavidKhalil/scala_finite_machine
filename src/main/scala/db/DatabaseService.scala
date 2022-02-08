package db

import cats.effect.{Blocker, ContextShift, IO, Resource}
import configuration.DatabaseConfiguration
import doobie.hikari.HikariTransactor
import org.flywaydb.core.Flyway

import scala.concurrent.ExecutionContext

object DatabaseService {
  def xa(config: DatabaseConfiguration, executionContext: ExecutionContext, blocker: Blocker)(implicit contextShift: ContextShift[IO]): Resource[IO, HikariTransactor[IO]] = {
    HikariTransactor.newHikariTransactor[IO](
      config.driver,
      config.url,
      config.user,
      config.password,
      executionContext,
      blocker
    )
  }

  def initialize(xa: HikariTransactor[IO]): IO[Unit] = {
    xa.configure { dataSource =>
      IO {
        val flyWay = Flyway.configure().dataSource(dataSource).load()
        flyWay.migrate()
        ()
      }
    }
  }
}
