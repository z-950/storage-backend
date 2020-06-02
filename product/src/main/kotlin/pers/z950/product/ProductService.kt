package pers.z950.product

import pers.z950.common.service.Close

interface ProductService : Close {
  suspend fun getProduct(id: String): Product?
  suspend fun patchProduct()
}
