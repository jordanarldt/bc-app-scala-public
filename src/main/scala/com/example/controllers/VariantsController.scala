package com.example.controllers

import com.example.ApiSchema._
import com.example.JsonFormats._
import com.example.lib.{ApiUtils, BcVerifiedUser, BigCommerce, Database}
import com.example.routes.JsonError
import com.example.UserRoles

import scala.concurrent.{Future, ExecutionContext}
import scala.util.{Try, Success, Failure}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._

import java.net.URLEncoder

import spray.json._

// Controller for requests to the /api/variants endpoint
// Creating a controller helps keep the code organized and isolate logic into components.
object VariantsController {
  def apply(
    bcClient: BigCommerce, 
    payload: BcVerifiedUser, 
    method: String
  )(implicit ec: ExecutionContext, db: Database): Route =
    new VariantsController(bcClient, payload, method, db, ec).routes
}

class VariantsController(
  bcClient: BigCommerce, 
  payload: BcVerifiedUser, 
  method: String,
  db: Database,
  ec: ExecutionContext
) {
  private implicit val executionContext: ExecutionContext = ec

  val routes: Route = method match {
    case "GET" =>
      parameters("page".as[Int].optional, "limit".as[Int].optional, "like".optional) {
        (pageOpt, limitOpt, likeOpt) =>
          val page = pageOpt.getOrElse(1)
          val limit = limitOpt.getOrElse(50)
          val like = likeOpt.getOrElse("")

          onComplete(getVariants(page, limit, like)) { 
            case Success(variants) =>
              complete(StatusCodes.OK, variants)
            case Failure(e) => 
              handleBcError(e)
          }
      } ~
      complete(StatusCodes.BadRequest)
    case "PUT" =>
      val storeHash = payload.context.split("/").last
      onSuccess(db.getStoreUser(storeHash, payload.user.id)) {
        case Some(dbUser) if dbUser.role != UserRoles.Viewer =>
          ApiUtils.validateBody[UpdateVariantBody] { body =>
            onComplete(updateVariantOrTracking(body)) {
              case Success(_) =>
                complete(StatusCodes.OK)
              case Failure(e) =>
                handleBcError(e)
            }
          }
        case _ => 
          complete(JsonError(StatusCodes.Unauthorized, "You are not authorized to perform this action."))
      }
  }

  // Private handler for BigCommerce Error parsing and responses to reduce code repetition
  private def handleBcError(e: Throwable): Route = {
    Try(e.getMessage.parseJson.convertTo[BcApiError]).toOption match {
      case Some(bcError) =>
        complete(JsonError(bcError.status, bcError.title))
      case None =>
        println(s"Unexpected error from BigCommerce: $e")
        complete(JsonError(StatusCodes.InternalServerError, "Unexpected error received from BigCommerce."))
    }
  }

  // Get a list of variants, as well as the product and inventory tracking info.
  // This requires an API request to the variants, as well as the product endpoint.
  private def getVariants(page: Int, limit: Int, like: String): Future[AmendedVariantResponse] = {
    val likeEncode = URLEncoder.encode(like, "UTF-8")
    val query = s"&page=$page&limit=$limit&sku:like=$likeEncode"

    // Fetch the variants data from BigCommerce, and then use the productIDs
    // to fetch the product names and inventory tracking settings.
    bcClient
      .get(s"v3/catalog/variants?include_fields=product_id,sku,inventory_level$query")
      .map { variantsJson =>
        val variants = variantsJson.parseJson.convertTo[BcVariantsResponse]
        val productIds = variants.data.map(_.product_id).toSet.toList
        val idQueries = productIds.sliding(100, 100).toList.map(_.mkString(","))
        val productRequests = idQueries.map { query => 
          bcClient.get(s"v3/catalog/products?include_fields=name,inventory_tracking,id&id:in=$query")
        }

        variants -> Future.sequence(productRequests)
      }
      .flatMap { case (variants, productRequests) =>
        productRequests.map { responses =>
          val productData = responses.flatMap(_.parseJson.convertTo[BcProductsResponse].data)

          // Keep the response format the same, but inject the productData values into the response
          // for each variant.
          val amendedVariants = variants.data.map { variant =>
            val product = productData.find(_.id == variant.product_id)

            val (name, inventoryTracking) = product match {
              case Some(p) => (p.name, p.inventory_tracking)
              case None => ("Unknown", "unknown")
            }

            VariantWithProductData(
              variant.id, 
              variant.product_id, 
              name, 
              variant.sku, 
              inventoryTracking, 
              variant.inventory_level
            )
          }

          AmendedVariantResponse(amendedVariants, variants.meta)
        }
      }
  }
  
  // Update a variant and/or the parent product tracking type
  private def updateVariantOrTracking(body: UpdateVariantBody): Future[Boolean] = {
    // construct the json manually since it's a simple request
    val variantBody = s"""[{"inventory_level": ${body.inventoryCount}, "id": ${body.variantId}}]"""

    val variantRequest = bcClient.put(s"v3/catalog/variants", variantBody)
    val productRequest = body.trackingType match {
      case Some(trackingType) => 
        val productBody = s"""{"inventory_tracking": "$trackingType"}"""
        bcClient.put(s"v3/catalog/products/${body.productId}", productBody)
      case None => Future.successful("noop")
    }

    Future.sequence(Seq(variantRequest, productRequest)).map { _ => true }
  }
}
