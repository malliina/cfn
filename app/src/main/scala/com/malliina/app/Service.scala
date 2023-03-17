package com.malliina.app

import cats.data.Kleisli
import cats.effect.{ExitCode, IO, IOApp}
import com.comcast.ip4s.{Port, host, port}
import io.circe.Json
import io.circe.syntax.EncoderOps
import org.http4s.CacheDirective.{`must-revalidate`, `no-cache`, `no-store`}
import org.http4s.{HttpRoutes, BuildInfo as _, *}
import org.http4s.headers.`Cache-Control`
import org.http4s.server.Router
import org.http4s.ember.server.EmberServerBuilder

import concurrent.duration.DurationInt
import scala.concurrent.ExecutionContext

object Service extends IOApp:
  val app = Service().router

  private val serverPort: Port =
    sys.env.get("SERVER_PORT").flatMap(s => Port.fromString(s)).getOrElse(port"9000")

  def server = EmberServerBuilder
    .default[IO]
    .withIdleTimeout(30.days)
    .withHost(host"0.0.0.0")
    .withPort(serverPort)
    .withHttpApp(app)
    .withShutdownTimeout(1.millis)
    .build

  override def run(args: List[String]): IO[ExitCode] =
    server.use(_ => IO.never).as(ExitCode.Success)

class Service extends Implicits:
  val noCache = `Cache-Control`(`no-cache`(), `no-store`, `must-revalidate`)

  val html = AppHtml(!BuildInfo.isProd)
  val routes = HttpRoutes.of[IO] {
    case GET -> Root =>
      ok(html.index.tags)
    case GET -> Root / "health" =>
      ok(Json.obj("gitHash" -> BuildInfo.gitHash.asJson))
  }
  val router: Kleisli[IO, Request[IO], Response[IO]] = Router("/" -> routes).orNotFound

  private def ok[A](a: A)(implicit w: EntityEncoder[IO, A]) = Ok(a, noCache)
