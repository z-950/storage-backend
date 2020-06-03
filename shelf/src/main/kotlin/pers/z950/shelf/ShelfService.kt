package pers.z950.shelf

import pers.z950.product.Product

interface ShelfService {
  fun getShelfList(): List<String>
  suspend fun updateShelf(list: List<Product>)
}
