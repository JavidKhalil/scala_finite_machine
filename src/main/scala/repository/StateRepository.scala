package repository

import cats.effect.IO
import doobie.util.transactor.Transactor
import fs2.Stream
import domain.{State, EntityDomain, EntityDomainNotFound}
import doobie._
import doobie.implicits._

class StateRepository(transactor: Transactor[IO]) {
  private implicit val importanceMeta: Meta[State] = Meta[String].timap(State.generateStateFromInput)(_.currentState)

  def getEntities: Stream[IO, EntityDomain] = {
    sql"SELECT id, name, state FROM entities".query[EntityDomain].stream.transact(transactor)
  }

  def getEntityDomain(id: Long): IO[Either[EntityDomainNotFound.type, EntityDomain]] = {
    sql"SELECT id, name, state FROM entities WHERE id = $id".query[EntityDomain].option.transact(transactor).map {
      case Some(entity) => Right(entity)
      case _ => Left(EntityDomainNotFound)
    }
  }

  def saveEntityDomain(entity: EntityDomain): IO[EntityDomain] = {
    sql"INSERT INTO entities (name, state) VALUES (${entity.name}, ${entity.currentState})".update.withUniqueGeneratedKeys[Long]("id").transact(transactor).map { id =>
      entity.copy(id = Some(id))
    }
  }

  def deleteEntityDomain(id: Long): IO[Either[EntityDomainNotFound.type, EntityDomain]] = {
    sql"DELETE FROM entities WHERE id=$id".update.run.transact(transactor)
      getEntityDomain(id)
    }

  def updateEntityState(id: Long, stateNew: String): IO[Either[EntityDomainNotFound.type, EntityDomain]] = {
    sql"UPDATE entities set state=$stateNew where id=$id".update.run.transact(transactor)
    getEntityDomain(id)
  }

}
