package com.malliina.app

import cats.effect.Async
import cats.effect.kernel.Resource
import com.malliina.config.{ConfigError, ConfigNode, ConfigReadable}
import com.malliina.util.AppLogger
import com.malliina.values.ErrorMessage
import com.typesafe.config.{Config, ConfigFactory}
import com.zaxxer.hikari.HikariConfig
import doobie.{ConnectionIO, LogHandler}
import doobie.hikari.HikariTransactor
import doobie.util.log.{ExecFailure, ProcessingFailure, Success}
import doobie.implicits.{toConnectionIOOps, toSqlInterpolator}
import doobie.util.ExecutionContexts
import com.malliina.database.DoobieDatabase
import com.malliina.database.Conf

import concurrent.duration.DurationInt
import java.nio.file.Paths

object AppConf:
  val appDir = Paths.get(sys.props("user.home")).resolve(".demo")
  val localConfFile = appDir.resolve("demo.conf")
  val localConfig = ConfigNode.default(localConfFile)
  def parse: Either[ConfigError, Conf] =
    localConfig.parse[Conf]("db")

trait DatabaseRunner[F[_]]:
  def logHandler: LogHandler[F]
  def run[T](io: ConnectionIO[T]): F[T]

class DemoDatabase[F[_]: Async](db: DoobieDatabase[F]):
  def version = db.run(sql"""select version()""".query[String].unique)
