package app.cash.backfila.client

import app.cash.backfila.client.interceptors.OkHttpClientSpecifiedHeadersInterceptor
import app.cash.backfila.client.interceptors.OkHttpClientSpecifiedHeadersInterceptor.Companion.headersSizeWithinLimit
import app.cash.backfila.service.HttpClientNetworkInterceptor
import com.squareup.moshi.Moshi
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import misk.client.HttpClientConfigUrlProvider
import misk.client.HttpClientFactory
import misk.client.HttpClientsConfig
import misk.moshi.adapter
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import retrofit2.Retrofit
import retrofit2.adapter.guava.GuavaCallAdapterFactory
import retrofit2.converter.wire.WireConverterFactory

@Singleton
class Http2ClientServiceClientProvider @Inject constructor(
  private val httpClientsConfig: HttpClientsConfig,
  private val httpClientFactory: HttpClientFactory,
  private val httpClientConfigUrlProvider: HttpClientConfigUrlProvider,
  @HttpClientNetworkInterceptor private val networkInterceptors: List<Interceptor>,
  private val moshi: Moshi,
) : BackfilaClientServiceClientProvider {
  override fun validateExtraData(connectorExtraData: String?) {
    checkNotNull(connectorExtraData, { "Extra data required for HTTP connector" })
    val fromJson = adapter().fromJson(connectorExtraData)
    checkNotNull(fromJson, { "Failed to parse HTTP connector extra data JSON" })
    checkNotNull(fromJson.host, { "HTTP connector extra data must contain a host" })

    if (!fromJson.headers.isNullOrEmpty()) {
      check(headersSizeWithinLimit(fromJson.headers)) { "Headers too large" }

      for (header in fromJson.headers) {
        checkNotNull(header.name, { "Header names must be set" })
        checkNotNull(header.value, { "Header values must be set" })
      }
    }
  }

  override fun clientFor(
    serviceName: String,
    connectorExtraData: String?,
  ): BackfilaClientServiceClient {
    val extraData = adapter().fromJson(connectorExtraData!!)
    val host = extraData!!.host
    val pathPrefix = extraData!!.pathPrefix
    val protocol = extraData!!.protocol
    val rootUrl = URL(protocol, host, "")
    val headers = extraData!!.headers

    val httpClientEndpointConfig = httpClientsConfig[rootUrl]
    val okHttpClientBuilder = httpClientFactory.create(httpClientEndpointConfig)
      .newBuilder()
      .apply {
        networkInterceptors.forEach {
          addNetworkInterceptor(it)
        }
      }
    if (!headers.isNullOrEmpty()) {
      okHttpClientBuilder.addInterceptor(OkHttpClientSpecifiedHeadersInterceptor(headers))
    }
    val baseUrl = httpClientConfigUrlProvider.getUrl(httpClientEndpointConfig).toHttpUrlOrNull()!!.resolve(pathPrefix)
    val retrofit = Retrofit.Builder()
      .baseUrl(baseUrl)
      .client(okHttpClientBuilder.build())
      .addConverterFactory(WireConverterFactory.create())
      .addCallAdapterFactory(GuavaCallAdapterFactory.create())
      .build()
    val api = retrofit.create(Http2ClientServiceApi::class.java)
    val logData = "url: ${httpClientEndpointConfig.url}, " +
      "headers: $headers"
    return Http2ClientServiceClient(api, logData)
  }

  private fun adapter() = moshi.adapter<Http2ConnectorData>()
}
