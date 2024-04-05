package com.yucel.withdrawal.service.verticle;

import com.yucel.withdrawal.domain.model.AccountRequest;
import com.yucel.withdrawal.domain.model.ErrorMessage;
import com.yucel.withdrawal.domain.model.TransferAddress;
import com.yucel.withdrawal.domain.model.WithdrawalRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

import java.math.BigDecimal;
import java.util.Optional;

import static com.yucel.withdrawal.service.verticle.util.VerticleConstantUtil.*;

public class HttpServerVerticle extends AbstractVerticle {

  private static final String JSON_CONTENT_TYPE = "application/json; charset=utf-8";
  public static final String CONTENT_TYPE_KEY = "content-type";

  /**
   * Registers routes for withdrawal and account operations.
   * There are extra endpoints to make it more feasible to test the cases
   *
   * Performs validations, transforms and delivers requests to vert.x eventbus as messages
   *
   * @param startPromise promise to follow startup
   * @throws Exception
   */
  @Override
  public void start(Promise<Void> startPromise) throws Exception {

    Router router = Router.router(vertx);
    router.route().handler(BodyHandler.create());
    router.get("/withdrawals/:withdrawalId/status").handler(this::handleWithdrawalStatusRequest);
    router.get("/withdrawals/:withdrawalId").handler(this::handleGetWithdrawalByIdRequest);
    router.post("/withdrawals").handler(this::handleCreateWithdrawalRequest);
    router.get("/withdrawals").handler(this::handleGetAllWithdrawalRequest);
    router.get("/accounts").handler(this::handleGetAllAccountsRequest);
    router.post("/accounts").handler(this::handleCreateAccountRequest);
    router.patch("/accounts/:address/balance").handler(this::handleUpdateBalance);
    router.get("/accounts/:address").handler(this::handleGetAccountByAddress);


    vertx.createHttpServer().requestHandler(router).listen(8888, http -> {
      if (http.succeeded()) {
        startPromise.complete();
        System.out.println("HTTP server started on port 8888");
      } else {
        startPromise.fail(http.cause());
      }
    });
  }

  private void handleGetAccountByAddress(RoutingContext routingContext) {
    final String addressField = "address";
    String address = routingContext.request().getParam(addressField);
    TransferAddress transferAddress = new TransferAddress(address);

    vertx.eventBus().request(GET_ACCOUNT_BY_ADDRESS_REQUEST_ADDRESS, JsonObject.mapFrom(transferAddress))
      .onComplete(messageAsyncResult -> {
        handleResponseWithBody(routingContext, messageAsyncResult);
      })
      .onFailure(this::handleServerError);
  }

  private void handleCreateAccountRequest(RoutingContext routingContext) {
    JsonObject accountRequestJson = routingContext.body().asJsonObject();

    AccountRequest accountRequest = accountRequestJson.mapTo(AccountRequest.class);

    Optional<ErrorMessage> errorMessage = validateAccountRequest(accountRequest);

    if(errorMessage.isPresent()) {
      routingContext.response().putHeader(CONTENT_TYPE_KEY, JSON_CONTENT_TYPE)
        .setStatusCode(errorMessage.get().httpCode()).end(JsonObject.mapFrom(errorMessage.get()).toString());
    } else {

      vertx.eventBus().request(CREATE_ACCOUNT_REQUEST_ADDRESS, accountRequestJson)
        .onComplete(messageAsyncResult -> {
          handleResponseWithEmptyBody(routingContext, messageAsyncResult);
        })
        .onFailure(this::handleServerError);
    }
  }

  private static void handleResponseWithEmptyBody(RoutingContext routingContext, AsyncResult<Message<Object>> messageAsyncResult) {
    if(messageAsyncResult.succeeded()) {
      routingContext.response().putHeader(CONTENT_TYPE_KEY, JSON_CONTENT_TYPE)
        .setStatusCode(HttpResponseStatus.CREATED.code()).end();
    } else {
      ReplyException cause = (ReplyException) messageAsyncResult.cause();
      JsonObject jsonObject = new JsonObject();
      jsonObject.put("message", cause.getMessage());
      routingContext.response().putHeader(CONTENT_TYPE_KEY, JSON_CONTENT_TYPE)
        .setStatusCode(cause.failureCode()).end(jsonObject.toString());
    }
  }

  private void handleGetAllAccountsRequest(RoutingContext routingContext) {

    vertx.eventBus().request(GET_ALL_ACCOUNTS_REQUEST_ADDRESS, new JsonObject())
      .onComplete(messageAsyncResult -> {
        handleResponseWithBody(routingContext, messageAsyncResult);
      })
      .onFailure(this::handleServerError);
  }

  private void handleUpdateBalance(RoutingContext routingContext) {
    String address = routingContext.pathParam("address");
    JsonObject body = routingContext.body().asJsonObject();
    body.put("address", address);

    vertx.eventBus().request(UPDATE_ACCOUNT_BALANCE_REQUEST_ADDRESS, body)
      .onComplete(messageAsyncResult -> {
        handleResponseWithBody(routingContext, messageAsyncResult);
      })
      .onFailure(this::handleServerError);
  }
  private void handleGetWithdrawalByIdRequest(RoutingContext routingContext) {
    final String transactionIdField = "withdrawalId";
    String transactionId = routingContext.request().getParam(transactionIdField);
    JsonObject json = new JsonObject();
    json.put(transactionIdField, transactionId);

    vertx.eventBus().request(GET_WITHDRAWAL_BY_ID_REQUEST_ADDRESS, json)
      .onComplete(messageAsyncResult -> {
        handleResponseWithBody(routingContext, messageAsyncResult);
      })
      .onFailure(this::handleServerError);
  }

  private void handleGetAllWithdrawalRequest(RoutingContext routingContext) {

    vertx.eventBus().request(GET_ALL_WITHDRAWALS_REQUEST_ADDRESS, new JsonObject())
      .onComplete(messageAsyncResult -> {
        handleResponseWithBody(routingContext, messageAsyncResult);
      })
      .onFailure(this::handleServerError);

  }

  private static void handleResponseWithBody(RoutingContext routingContext, AsyncResult<Message<Object>> messageAsyncResult) {
    if(messageAsyncResult.succeeded()) {
      routingContext.response().putHeader(CONTENT_TYPE_KEY, JSON_CONTENT_TYPE)
        .setStatusCode(HttpResponseStatus.OK.code()).end(messageAsyncResult.result().body().toString());
    } else {
      ReplyException cause = (ReplyException) messageAsyncResult.cause();
      JsonObject jsonObject = new JsonObject();
      jsonObject.put("message", cause.getMessage());
      routingContext.response().putHeader(CONTENT_TYPE_KEY, JSON_CONTENT_TYPE)
        .setStatusCode(cause.failureCode()).end(jsonObject.toString());
    }
  }

  private void handleCreateWithdrawalRequest(RoutingContext routingContext) {
    JsonObject withdrawalRequestJson = routingContext.body().asJsonObject();

    WithdrawalRequest withdrawalRequest = withdrawalRequestJson.mapTo(WithdrawalRequest.class);

    Optional<ErrorMessage> errorMessage = validateWithdrawalRequest(withdrawalRequest);

    if(errorMessage.isPresent()) {
      routingContext.response().putHeader(CONTENT_TYPE_KEY, JSON_CONTENT_TYPE)
        .setStatusCode(errorMessage.get().httpCode()).end(JsonObject.mapFrom(errorMessage.get()).toString());
    } else {

      vertx.eventBus().request(CREATE_WITHDRAWAL_REQUEST_ADDRESS, withdrawalRequestJson)
        .onComplete(messageAsyncResult -> {
          handleResponseWithEmptyBody(routingContext, messageAsyncResult);
        })
        .onFailure(this::handleServerError);
    }
  }

  private void handleWithdrawalStatusRequest(RoutingContext routingContext) {
    final String withdrawalIdField = "withdrawalId";
    String transactionId = routingContext.request().getParam(withdrawalIdField);
    JsonObject json = new JsonObject();
    json.put(withdrawalIdField, transactionId);

    vertx.eventBus().request(GET_WITHDRAWAL_STATUS_ADDRESS, json)
      .onComplete(messageAsyncResult -> {
        handleResponseWithBody(routingContext, messageAsyncResult);
      })
      .onFailure(this::handleServerError);
  }

  private Optional<ErrorMessage> validateWithdrawalRequest(WithdrawalRequest withdrawalRequest) {

    if(withdrawalRequest.amount() == null || withdrawalRequest.fromAccountAddress() == null || withdrawalRequest.toAccountAddress() == null) {
      return Optional.of(new ErrorMessage(HttpResponseStatus.BAD_REQUEST.code(), "fields cannot be empty"));
    }
    if(withdrawalRequest.amount().compareTo(BigDecimal.ZERO) < 1) {
      return Optional.of(new ErrorMessage(HttpResponseStatus.UNPROCESSABLE_ENTITY.code(), "amount must be more than zero"));
    }

    return Optional.empty();
  }

  private Optional<ErrorMessage> validateAccountRequest(AccountRequest accountRequest) {

    if(accountRequest.address() == null || accountRequest.balance() == null) {
      return Optional.of(new ErrorMessage(HttpResponseStatus.BAD_REQUEST.code(), "fields cannot be empty"));
    }
    if(accountRequest.balance().compareTo(BigDecimal.ZERO) < 1) {
      return Optional.of(new ErrorMessage(HttpResponseStatus.UNPROCESSABLE_ENTITY.code(), "balance must be more than zero"));
    }

    return Optional.empty();
  }

  private void handleServerError(Throwable throwable) {
    System.out.println(throwable.getMessage());
  }
}
