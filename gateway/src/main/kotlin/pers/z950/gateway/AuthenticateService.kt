package pers.z950.gateway

import pers.z950.common.service.Close

interface AuthenticateService : Close {
  /**
   * return <id, role>
   */
  suspend fun authenticate(username: String, password: String, openId: String? = null): Pair<String, String>
  suspend fun authenticate(openId: String): Pair<String, String>
}
