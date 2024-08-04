package com.malliina.app

import cats.effect.{Async, Sync}
import cats.effect.kernel.Resource
import com.malliina.config.{ConfigError, ConfigNode, ConfigReadable}
import com.malliina.util.AppLogger
import com.zaxxer.hikari.HikariConfig
import doobie.{ConnectionIO, LogHandler}
import doobie.hikari.HikariTransactor
import doobie.util.log.{ExecFailure, LogEvent, ProcessingFailure, Success}
import doobie.implicits.{toConnectionIOOps, toSqlInterpolator}
import doobie.util.{ExecutionContexts, log}

import concurrent.duration.DurationInt
import java.nio.file.Paths

case class DatabaseConf(url: String, user: String, pass: String)

object DatabaseConf:
  val MySQLDriver = "com.mysql.cj.jdbc.Driver"

  val appDir = Paths.get(sys.props("user.home")).resolve(".demo")
  val localConfFile = appDir.resolve("demo.conf")
  def demoConf: ConfigNode = ConfigNode.default(localConfFile)

  implicit val config: ConfigReadable[DatabaseConf] = ConfigReadable.node.emap { obj =>
    for
      url <- obj.parse[String]("url")
      user <- obj.parse[String]("user")
      pass <- obj.parse[String]("pass")
    yield DatabaseConf(url, user, pass)
  }

  def parse(c: ConfigNode): Either[ConfigError, DatabaseConf] = c.parse[DatabaseConf]("db")
  def unsafe(c: ConfigNode = demoConf) =
    parse(c).fold(err => throw Exception(err.message.message), identity)

object DoobieDatabase:
  private val log = AppLogger(getClass)

  def default[F[_]: Async](conf: DatabaseConf): Resource[F, DatabaseRunner[F]] =
    resource(hikariConf(conf)).map(tx => DoobieDatabase(tx))

  private def resource[F[_]: Async](conf: HikariConfig): Resource[F, HikariTransactor[F]] =
    for
      ec <- ExecutionContexts.fixedThreadPool[F](32)
      tx <- HikariTransactor.fromHikariConfig[F](conf)
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
  implicit val logHandler: LogHandler[F] = new LogHandler[F]:
    override def run(logEvent: LogEvent): F[Unit] =
      Sync[F].delay(
        logEvent match
          case Success(sql, args, _, exec, processing) =>
            log.info(s"OK '$sql' exec ${exec.toMillis} ms processing ${processing.toMillis} ms.")
          case ProcessingFailure(sql, args, _, exec, processing, failure) =>
            log.error(s"Failed '$sql' in ${exec + processing}.", failure)
          case ExecFailure(sql, args, _, exec, failure) =>
            log.error(s"Exec failed '$sql' in $exec.'", failure)
      )

  def run[T](io: ConnectionIO[T]): F[T] = io.transact(tx)

trait DatabaseRunner[F[_]]:
  def logHandler: LogHandler[F]
  def run[T](io: ConnectionIO[T]): F[T]

class DemoDatabase[F[_]: Async](db: DatabaseRunner[F]):
  def version = db.run(sql"""select version()""".query[String].unique)
