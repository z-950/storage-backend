package pers.z950.gateway.impl

import pers.z950.common.service.repository.PostgresRepositoryWrapper
import pers.z950.gateway.AuthenticateService

class AuthenticateServiceImpl : PostgresRepositoryWrapper(), AuthenticateService {
  override suspend fun authenticate(username: String, password: String, openId: String?): Pair<String, String> {
    return Pair("ididid", "customer")
  }

  override suspend fun authenticate(openId: String): Pair<String, String> {
    TODO("Not yet implemented")
  }
}
