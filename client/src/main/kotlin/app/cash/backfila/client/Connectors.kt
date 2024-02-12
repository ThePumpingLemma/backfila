package app.cash.backfila.client

object Connectors {
  const val HTTP = "HTTP"
  const val HTTP2 = "HTTP2"
  const val ENVOY = "ENVOY"
}

data class HttpHeader(val name: String, val value: String)

data class HttpConnectorData @JvmOverloads constructor(val url: String, val headers: List<HttpHeader> = listOf())

data class Http2ConnectorData @JvmOverloads constructor(
  val host: String,
  val pathPrefix: String = "backfila",
  val protocol: String = "https",
  val headers: List<HttpHeader> = listOf()
)

data class EnvoyConnectorData @JvmOverloads constructor(val clusterType: String, val headers: List<HttpHeader> = listOf())
