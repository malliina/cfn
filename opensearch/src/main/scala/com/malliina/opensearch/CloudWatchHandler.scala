package com.malliina.opensearch

import com.amazonaws.services.lambda.runtime.events.CloudWatchLogsEvent
import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}

import java.time.Instant

class CloudWatchHandler extends RequestHandler[CloudWatchLogsEvent, String] {
  override def handleRequest(event: CloudWatchLogsEvent, context: Context): String = {
    val now = Instant.now()
    println(s"Hello, Lambda! The time is now $now.")
    s"Handled event '${event.getAwsLogs.getData}'."
  }
}
