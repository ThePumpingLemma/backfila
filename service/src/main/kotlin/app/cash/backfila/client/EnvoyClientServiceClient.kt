package app.cash.backfila.client

import app.cash.backfila.protos.clientservice.GetNextBatchRangeRequest
import app.cash.backfila.protos.clientservice.GetNextBatchRangeResponse
import app.cash.backfila.protos.clientservice.PrepareBackfillRequest
import app.cash.backfila.protos.clientservice.PrepareBackfillResponse
import app.cash.backfila.protos.clientservice.RunBatchRequest
import app.cash.backfila.protos.clientservice.RunBatchResponse
import retrofit2.HttpException
import retrofit2.Response

internal class EnvoyClientServiceClient internal constructor(
  private val api: EnvoyClientServiceApi,
  private val connectionLogData: String,
) : BackfilaClientServiceClient {

  override fun prepareBackfill(request: PrepareBackfillRequest): PrepareBackfillResponse {
    return api.prepareBackfill(request).execute().getOrThrow()
  }

  override suspend fun getNextBatchRange(request: GetNextBatchRangeRequest):
    GetNextBatchRangeResponse {
    return api.getNextBatchRange(request)
  }

  override suspend fun runBatch(request: RunBatchRequest): RunBatchResponse {
    return api.runBatch(request)
  }

  override fun connectionLogData() = connectionLogData

  private fun <T> Response<T>.getOrThrow(): T {
    if (!this.isSuccessful) {
      throw HttpException(this)
    }
    return this.body()!!
  }
}
