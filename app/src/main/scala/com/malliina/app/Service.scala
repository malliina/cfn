package com.malliina.app

import cats.data.Kleisli
import cats.effect.kernel.Async
import cats.effect.{ExitCode, IO, IOApp}
import cats.syntax.all.{catsSyntaxApplicativeError, toFlatMapOps}
import com.comcast.ip4s.{Port, host, port}
import com.malliina.app.Service.log
import com.malliina.util.AppLogger
import io.circe.Json
import io.circe.syntax.EncoderOps
import org.http4s.CacheDirective.{`must-revalidate`, `no-cache`, `no-store`}
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.headers.`Cache-Control`
import org.http4s.server.Router
import org.http4s.{HttpRoutes, BuildInfo as _, *}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

object Service extends IOApp:
  private val log = AppLogger(getClass)
  private val serverPort: Port =
    sys.env.get("SERVER_PORT").flatMap(s => Port.fromString(s)).getOrElse(port"9000")

  def appResource[F[_]: Async] =
    val conf = DatabaseConf.unsafe()
    for
      db <- DoobieDatabase.default[F](conf)
      service = Service[F](DemoDatabase(db))
      server <- serverResource(service)
    yield server

  def serverResource[F[_]: Async](service: Service[F]) = EmberServerBuilder
    .default[F]
    .withIdleTimeout(30.days)
    .withHost(host"0.0.0.0")
    .withPort(serverPort)
    .withHttpApp(service.router)
    .withShutdownTimeout(1.millis)
    .build

  override def run(args: List[String]): IO[ExitCode] =
    appResource[IO].use(_ => IO.never).as(ExitCode.Success)

class Service[F[_]: Async](db: DemoDatabase[F]) extends Implicits[F]:
  val noCache = `Cache-Control`(`no-cache`(), `no-store`, `must-revalidate`)

  val html = AppHtml(!BuildInfo.isProd)
  val routes = HttpRoutes.of[F] {
    case GET -> Root =>
      ok(html.index.tags)
    case GET -> Root / "health" =>
      ok(Json.obj("gitHash" -> BuildInfo.gitHash.asJson))
    case GET -> Root / "version" =>
      db.version.flatMap { version =>
        ok(Json.obj("version" -> version.asJson))
      }.handleErrorWith { err =>
        log.error("Failed to fetch database version.", err)
        ise(Json.obj("message" -> "Failed.".asJson))
      }
  }
  val router: Kleisli[F, Request[F], Response[F]] = Router("/" -> routes).orNotFound

  private def ok[A](a: A)(implicit w: EntityEncoder[F, A]) = Ok(a, noCache)
  private def ise[A](a: A)(implicit w: EntityEncoder[F, A]) = InternalServerError(a, noCache)
