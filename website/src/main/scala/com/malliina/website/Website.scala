package com.malliina.website

import org.slf4j.{Logger, LoggerFactory}
import scalatags.Text.all.*
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path, Paths}

object Website:
  val log = AppLogger(getClass)

  def main(args: Array[String]): Unit =
    val targetDir =
      sys.env
        .get("WEBSITE_TARGET")
        .map(s => Paths.get(s))
        .getOrElse(Paths.get("target/website"))
    buildWebsite(targetDir)

  def buildWebsite(targetDir: Path): Path =
    if !Files.exists(targetDir) then Files.createDirectories(targetDir)
    val file = targetDir.resolve("index.html")
    val written =
      Files.write(file, index.render.getBytes(StandardCharsets.UTF_8))
    log.info(s"Wrote ${Files.size(written)} bytes to ${file.toAbsolutePath}.")
    written

  def index = html(
    body(p(s"Hello, world! This is build ${BuildInfo.gitHash}."))
  )

object AppLogger:
  def apply(cls: Class[?]): Logger =
    val name = cls.getName.reverse.dropWhile(_ == '$').reverse
    LoggerFactory.getLogger(name)
