package pers.z950.`return`

interface ReturnService {
  suspend fun getAllNotChecked(worker: String): List<Return>
  suspend fun checkReturn(uid: Int, worker: String)
}
