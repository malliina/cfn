package com.malliina.app

import cats.effect.Async
import cats.effect.kernel.Resource
import com.malliina.config.ConfigReadable
import com.malliina.util.AppLogger
import com.malliina.values.ErrorMessage
import com.typesafe.config.{Config, ConfigFactory}
import com.zaxxer.hikari.HikariConfig
import doobie.{ConnectionIO, LogHandler}
import doobie.hikari.HikariTransactor
import doobie.util.log.{ExecFailure, ProcessingFailure, Success}
import doobie.implicits.{toConnectionIOOps, toSqlInterpolator}
import doobie.util.ExecutionContexts

import concurrent.duration.DurationInt
import java.nio.file.Paths

implicit class ConfigOps(c: Config) extends AnyVal:
  def read[T](key: String)(implicit r: ConfigReadable[T]): Either[ErrorMessage, T] =
    r.read(key, c)

  def unsafe[T: ConfigReadable](key: String): T =
    c.read[T](key).fold(err => throw IllegalArgumentException(err.message), identity)

case class DatabaseConf(url: String, user: String, pass: String)

object DatabaseConf:
  val MySQLDriver = "com.mysql.cj.jdbc.Driver"

  val appDir = Paths.get(sys.props("user.home")).resolve(".demo")
  val localConfFile = appDir.resolve("demo.conf")
  val localConfig = ConfigFactory.parseFile(localConfFile.toFile).withFallback(ConfigFactory.load())
  def demoConf: Config = ConfigFactory.load(localConfig).resolve()

  implicit val config: ConfigReadable[DatabaseConf] = ConfigReadable.config.emap { obj =>
    for
      url <- obj.read[String]("url")
      user <- obj.read[String]("user")
      pass <- obj.read[String]("pass")
    yield DatabaseConf(url, user, pass)
  }

  def parse(c: Config): Either[ErrorMessage, DatabaseConf] = c.read[DatabaseConf]("db")
  def unsafe(c: Config = demoConf) = parse(c).fold(err => throw Exception(err.message), identity)

object DoobieDatabase:
  private val log = AppLogger(getClass)

  def default[F[_]: Async](conf: DatabaseConf): Resource[F, DatabaseRunner[F]] =
    resource(hikariConf(conf)).map(tx => DoobieDatabase(tx))

  private def resource[F[_]: Async](conf: HikariConfig): Resource[F, HikariTransactor[F]] =
    for
      ec <- ExecutionContexts.fixedThreadPool[F](32)
      tx <- HikariTransactor.fromHikariConfig[F](conf, ec)
    yield tx

  private def hikariConf(conf: DatabaseConf): HikariConfig =
    val hikari = new HikariConfig()
    hikari.setDriverClassName(DatabaseConf.MySQLDriver)
    hikari.setJdbcUrl(conf.url)
    hikari.setUsername(conf.user)
    hikari.setPassword(conf.pass)
    hikari.setMaxLifetime(60.seconds.toMillis)
    hikari.setMaximumPoolSize(10)
    log.info(s"Connecting to '${conf.url}'...")
    hikari

class DoobieDatabase[F[_]: Async](tx: HikariTransactor[F]) extends DatabaseRunner[F]:
  val log = DoobieDatabase.log
  implicit val logHandler: LogHandler = LogHandler {
    case Success(sql, args, exec, processing) =>
      log.info(s"OK '$sql' exec ${exec.toMillis} ms processing ${processing.toMillis} ms.")
    case ProcessingFailure(sql, args, exec, processing, failure) =>
      log.error(s"Failed '$sql' in ${exec + processing}.", failure)
    case ExecFailure(sql, args, exec, failure) =>
      log.error(s"Exec failed '$sql' in $exec.'", failure)
  }

  def run[T](io: ConnectionIO[T]): F[T] = io.transact(tx)

trait DatabaseRunner[F[_]]:
  def logHandler: LogHandler
  def run[T](io: ConnectionIO[T]): F[T]

class DemoDatabase[F[_]: Async](db: DatabaseRunner[F]):
  def version = db.run(sql"""select version()""".query[String].unique)
