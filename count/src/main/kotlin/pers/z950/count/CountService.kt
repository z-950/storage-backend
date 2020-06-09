package pers.z950.count

import pers.z950.common.service.Close

interface CountService : Close {
  suspend fun create(shelfId: String, worker: String)
  suspend fun getAllCount(): List<Count>
  suspend fun getNotFinishedCount(worker: String): List<Count>
  suspend fun finishCount(id: Int)
}
