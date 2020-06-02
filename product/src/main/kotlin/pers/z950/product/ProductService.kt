package pers.z950.product

import pers.z950.common.service.Close

interface ProductService : Close {
  suspend fun getProduct(id: String): Product?
  suspend fun getAllProduct(): List<Product>
  suspend fun reduceProducts(map: Map<String, Int>)
  suspend fun updateProducts(list: List<Product>)
}
