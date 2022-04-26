package com.example

// Schema declaration for API route marshalling/object types
object ApiSchema {
  final case class UpdateUserRoleBody(userId: Int, role: UserRoles.UserRole)
  final case class PaginationInfo(currentPage: Int, totalPages: Int, totalItems: Int)
  final case class RoleResponse(role: UserRoles.UserRole)
  final case class UserResponse(userId: Int, email: String, role: UserRoles.UserRole, lastLogin: Int)
  final case class PaginatedUsersResponse(data: Seq[UserResponse], pagination: PaginationInfo)

  final case class VariantWithProductData(
    id: Int, 
    product_id: Int, 
    product_name: String,
    sku: String, 
    inventory_tracking: String,
    inventory_level: Int
  )
  final case class AmendedVariantResponse(data: Seq[VariantWithProductData], meta: BcMeta)
  final case class UpdateVariantBody(
    variantId: Int, 
    productId: Int, 
    inventoryCount: Int,
    trackingType: Option[String]
  )

  // BigCommerce API response types
  final case class BcApiError(status: Int, title: String)
  
  final case class BcPagination(
    total: Int, 
    count: Int, 
    per_page: Int, 
    current_page: Int, 
    total_pages: Int
  )
  final case class BcMeta(pagination: BcPagination)
  
  final case class BcVariant(id: Int, product_id: Int, sku: String, inventory_level: Int)
  final case class BcVariantsResponse(data: Seq[BcVariant], meta: BcMeta)

  final case class BcProduct(id: Int, name: String, inventory_tracking: String)
  final case class BcProductsResponse(data: Seq[BcProduct], meta: BcMeta)
}
