package service

import cats.effect.IO
import domain.EntityDomain
import fs2.Stream
import io.circe.Json
import io.circe.literal._
//import domain.{High, Low, Medium, EntityDomain}
import org.http4s.circe._
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.{Request, Response, Status, Uri, _}
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import repository.StateRepository

class StateServiceSpec extends AnyWordSpec with MockFactory with Matchers {
  private val repository = stub[StateRepository]

  private val service = new StateService(repository).routes

  "EntityService" should {
    "create an entity" in {
      val id = 1L
      val entity = EntityDomain(Some(id), "some", "init")
      (repository.saveEntityDomain _).when(entity).returns(IO.pure(entity.copy(id = Some(id))))
      val createJson = json"""
        {
          "name": ${entity.name},
          "state": ${entity.currentState}
        }"""
      val response = serve(Request[IO](POST, uri"/entities").withEntity(createJson))
      response.status shouldBe Status.Created
      response.as[Json].unsafeRunSync() shouldBe json"""
        {
          "id": $id,
          "name": ${entity.name},
          "currentState": ${entity.currentState}
        }"""
    }

    "update an entity" in {
      val id = 1L
      val entity = EntityDomain(Some(id), "some", "init")
      (repository.saveEntityDomain _).when(entity).returns(IO.pure(entity.copy(id = Some(id))))
      val updateJson = json"""
        {
          "name": ${entity.name},
          "currentState": ${entity.currentState}
        }"""

      val response = serve(Request[IO](PUT, Uri.unsafeFromString(s"/entities/$id")).withEntity(updateJson))
      response.status shouldBe Status.Ok
      response.as[Json].unsafeRunSync() shouldBe json"""
        {
          "id": $id,
          "name": ${entity.name},
          "currentState": ${entity.currentState}
        }"""
    }

    "return a single entity" in {
      val id = 1L
      val ent = EntityDomain(Some(id), "ent", "init")
      (repository.getEntityDomain _).when(id).returns(IO.pure(Right(ent)))

      val response = serve(Request[IO](GET, Uri.unsafeFromString(s"/entities/$id")))
      response.status shouldBe Status.Ok
      response.as[Json].unsafeRunSync() shouldBe json"""
        {
          "id": $id,
          "currentState": ${ent.currentState},
          "importance": ${ent.name}
        }"""
    }

  }

  private def serve(request: Request[IO]): Response[IO] = {
    service.orNotFound(request).unsafeRunSync()
  }
}
