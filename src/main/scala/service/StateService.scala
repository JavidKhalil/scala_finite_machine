package service

import cats.effect.IO
import fs2.Stream
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.{Decoder, Encoder}
import domain.{State, EntityDomain, EntityDomainNotFound}
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.{Location, `Content-Type`}
import org.http4s.{HttpRoutes, MediaType, Uri}
import repository.StateRepository
import org.http4s.EntityDecoder
import org.http4s.EntityEncoder
import io.circe.syntax._
import io.circe.generic.auto._

class StateService(repository: StateRepository) extends Http4sDsl[IO] {

  private implicit val encodeState: Encoder[State] = Encoder.encodeString.contramap[State](_.currentState)
  private implicit val decodeState: Decoder[State] = Decoder.decodeString.map[State](State.generateStateFromInput)
  implicit val entityEncoder: EntityEncoder[IO, EntityDomain] = jsonEncoderOf[IO, EntityDomain]
  implicit val entityDecoder = jsonOf[IO, EntityDomain]
  implicit val nameEncoder: EntityEncoder[IO, Name] = jsonEncoderOf[IO, Name]
  implicit val nameDecoder: EntityDecoder[IO, Name] = jsonOf[IO, Name]

  case class Name(name: String)

  val routes = HttpRoutes.of[IO] {
    case GET -> Root / "entities" =>
      Ok(Stream("[") ++ repository.getEntities.
        map(_.asJson.noSpaces).intersperse(",") ++ Stream("]"), `Content-Type`(MediaType.application.json))

    case req @ POST -> Root / "entities" =>
      for {
        name <- req.as[Name]
        created <- repository.saveEntityDomain(EntityDomain(None, name.name, "init"))
        response <- Created(created.asJson, Location(Uri.unsafeFromString(s"/entities/${created.id.get}")))
      } yield response

    case POST -> Root / "entities" / id / "transit" / changeState => repository.updateEntityState(id.toLong, changeState).flatMap( res => adjustResultType(res))

    case DELETE -> Root / "entities" / "delete" / id => repository.deleteEntityDomain(id.toLong).flatMap( ent => adjustResultType(ent))

    case GET -> Root / "entities" / id / "history" => repository.getEntityDomain(id.toLong).flatMap( res => adjustResultType(res))

  }

  private def adjustResultType(result: Either[EntityDomainNotFound.type, EntityDomain]) = {
    result match {
      case Left(EntityDomainNotFound) => NotFound()
      case Right(entity) => Ok(entity.asJson)
    }
  }

}
