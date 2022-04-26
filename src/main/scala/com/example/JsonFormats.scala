package com.example

//#json-formats
import spray.json.{
  DefaultJsonProtocol, 
  RootJsonFormat, 
  JsString, 
  JsValue, 
  DeserializationException
}

object JsonFormats  {
  // import the default encoders for primitive types (Int, String, Lists etc)
  import DefaultJsonProtocol._
  import com.example.routes._
  import com.example.lib._
  import com.example.controllers._
  import com.example.ApiSchema._

  // BigCommerce formats
  implicit val bcUserFormat = jsonFormat2(BcUser)
  implicit val bcAuthResponseFormat = jsonFormat5(BcAuthResponse)
  implicit val bcVerifiedUserFormat = jsonFormat4(BcVerifiedUser)
  implicit val bcSignedPayloadJwtFormat = jsonFormat6(BcJwtPayload)
  implicit val bcPaginationFormat = jsonFormat5(BcPagination)
  implicit val bcMetaFormat = jsonFormat1(BcMeta)
  implicit val bcApiErrorFormat = jsonFormat2(BcApiError)

  // Variants
  implicit val bcVariantFormat = jsonFormat4(BcVariant)
  implicit val bcVariantsResponseFormat = jsonFormat2(BcVariantsResponse)

  // Products
  implicit val bcProductFormat = jsonFormat3(BcProduct)
  implicit val bcProductsResponseFormat = jsonFormat2(BcProductsResponse)

  // Database schema formats
  implicit object UserRoleFormat extends RootJsonFormat[UserRoles.UserRole] {
    def write(role: UserRoles.UserRole) = role match {
      case UserRoles.Owner => JsString("owner")
      case UserRoles.Admin => JsString("admin")
      case UserRoles.Viewer => JsString("viewer")
    }

    def read(json: JsValue) = json match {
      case JsString("owner") => UserRoles.Owner
      case JsString("admin") => UserRoles.Admin
      case JsString("viewer") => UserRoles.Viewer
      case _ => throw new DeserializationException("Invalid UserRole type received")
    }
  }

  implicit val storeUserFormat = jsonFormat6(DbSchema.StoreUser)

  // Api response/request formats
  implicit val roleResponseFormat = jsonFormat1(RoleResponse)
  implicit val updateRoleFormat = jsonFormat2(UpdateUserRoleBody)
  implicit val storeUsersResponseFormat = jsonFormat4(UserResponse)
  implicit val paginationInfoFormat = jsonFormat3(PaginationInfo)
  implicit val paginatedUsersResponseFormat = jsonFormat2(PaginatedUsersResponse)

  implicit val variantWithProductDataFormat = jsonFormat6(VariantWithProductData)
  implicit val amendedVariantResponseFormat = jsonFormat2(AmendedVariantResponse)
  implicit val updateVariantBodyFormat = jsonFormat4(UpdateVariantBody)
}
