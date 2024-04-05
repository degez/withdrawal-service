package com.yucel.withdrawal.service.verticle;

import com.yucel.withdrawal.MainVerticle;
import com.yucel.withdrawal.domain.model.AccountRequest;
import com.yucel.withdrawal.domain.model.UpdateBalanceRequest;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.Json;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.math.BigDecimal;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(VertxExtension.class)
class AccountIntegrationTest {

  public static final int PORT = 8888;
  public static final String HOST = "localhost";
  public static final String CONTENT_TYPE_KEY = "Content-Type";
  public static final String CONTENT_TYPE_JSON = "application/json";
  public static final String ACCOUNTS_PATH = "/accounts";

  @BeforeEach
  void deploy_verticle(Vertx vertx, VertxTestContext testContext) {
    vertx.deployVerticle(new MainVerticle(), testContext.succeedingThenComplete());
  }

  @Test
  void givenNoAccounts_whenGetAllAccounts_thenGetEmptyArray(Vertx vertx, VertxTestContext testContext) {
    HttpClient client = vertx.createHttpClient();

    client.request(HttpMethod.GET, PORT, HOST, "/accounts")
      .compose(req -> req.send().compose(HttpClientResponse::body))
      .onComplete(testContext.succeeding(buffer -> testContext.verify(() -> {

        assertEquals(buffer.toString(), "[]");
        testContext.completeNow();
      })));
  }

  @Test
  void givenValidAccount_whenCreateAccount_thenSuccess(Vertx vertx, VertxTestContext testContext) {
    HttpClient client = vertx.createHttpClient();
    final AccountRequest testAccount = new AccountRequest("DE123", BigDecimal.TEN);
    client.request(HttpMethod.POST, PORT, HOST, "/accounts")
      .flatMap(req -> req.putHeader(CONTENT_TYPE_KEY, CONTENT_TYPE_JSON)
        .send(Json.encode(testAccount))
        .onSuccess(
          response -> testContext.verify(() -> assertEquals(response.statusCode(), 201))
        )
      )
      .flatMap(httpClientResponse -> Future.succeededFuture(testAccount.address()))
      .flatMap(accountAddress -> client.request(HttpMethod.GET, PORT, HOST, "/accounts")
        .compose(req -> req.send().compose(HttpClientResponse::body))

        .onComplete(testContext.succeeding(buffer -> testContext.verify(() -> {

          assertTrue(Objects.nonNull(buffer));
          assertTrue(buffer.toString().contains(testAccount.address()));
          testContext.completeNow();
        }))));

  }

  @Test
  void givenValidAccount_whenUpdateBalance_thenSuccess(Vertx vertx, VertxTestContext testContext) {
    HttpClient client = vertx.createHttpClient();
    final BigDecimal initialBalance = BigDecimal.TEN;
    final BigDecimal amountToBeAdded = BigDecimal.TEN;
    final AccountRequest testAccount = new AccountRequest("DE123", initialBalance);
    UpdateBalanceRequest balanceRequest = new UpdateBalanceRequest(amountToBeAdded);
    client.request(HttpMethod.POST, PORT, HOST, "/accounts")
      .flatMap(req -> req.putHeader(CONTENT_TYPE_KEY, CONTENT_TYPE_JSON)
        .send(Json.encode(testAccount))
        .onSuccess(
          response -> assertEquals(response.statusCode(), 201)
        )
      )
      .flatMap(httpClientResponse -> Future.succeededFuture(testAccount.address()))
      .flatMap(accountAddress -> client.request(HttpMethod.PATCH, PORT, HOST, "/accounts/%s/balance".formatted(accountAddress))
        .compose(req -> req.putHeader(CONTENT_TYPE_KEY, CONTENT_TYPE_JSON)
          .send(Json.encode(balanceRequest)).compose(HttpClientResponse::body))
        .onComplete(testContext.succeeding(buffer -> testContext.verify(() -> {

          assertTrue(Objects.nonNull(buffer));
          AccountRequest accountRequest = Json.decodeValue(buffer, AccountRequest.class);
          assertEquals(accountRequest.balance(), initialBalance.add(amountToBeAdded));
          testContext.completeNow();
        }))));

  }

  @Test
  void givenInvalidAccount_whenUpdateBalance_thenNotFound(Vertx vertx, VertxTestContext testContext) {
    HttpClient client = vertx.createHttpClient();
    final BigDecimal initialBalance = BigDecimal.TEN;
    final BigDecimal amountToBeAdded = BigDecimal.TEN;
    final AccountRequest testAccount = new AccountRequest("DE123", initialBalance);
    final String notExistingAccountAddress = "XXX";
    UpdateBalanceRequest balanceRequest = new UpdateBalanceRequest(amountToBeAdded);
    client.request(HttpMethod.PATCH, PORT, HOST, "/accounts/%s/balance".formatted(notExistingAccountAddress))
      .flatMap(req -> req.putHeader(CONTENT_TYPE_KEY, CONTENT_TYPE_JSON)
        .send(Json.encode(testAccount))
        .onSuccess(
          response -> testContext.verify(() -> {
            assertEquals(response.statusCode(), 404);
            testContext.completeNow();
          })
        )
//        .onComplete(httpClientResponse -> testContext.completeNow())
      )
      ;

  }

  @Test
  void givenDuplicateAccount_whenCreateAccount_thenGetConflict(Vertx vertx, VertxTestContext testContext) {
    HttpClient client = vertx.createHttpClient();
    final AccountRequest testAccount = new AccountRequest("DE123", BigDecimal.TEN);
    createAccount(client, testAccount);

    client.request(HttpMethod.POST, PORT, HOST, ACCOUNTS_PATH)
      .flatMap(req -> req.putHeader(CONTENT_TYPE_KEY, CONTENT_TYPE_JSON)
        .send(Json.encode(testAccount))
        .onSuccess(
          response -> testContext.verify(() ->  assertEquals(response.statusCode(), 409))
        ).onComplete(httpClientResponse -> testContext.completeNow())
      );


  }

  @Test
  void givenInvalidAccount_whenCreateAccount_thenGetBadRequest(Vertx vertx, VertxTestContext testContext) {
    HttpClient client = vertx.createHttpClient();
    final AccountRequest testAccount = new AccountRequest(null, BigDecimal.TEN);

    client.request(HttpMethod.POST, PORT, HOST, ACCOUNTS_PATH)
      .flatMap(req -> req.putHeader(CONTENT_TYPE_KEY, CONTENT_TYPE_JSON)
        .send(Json.encode(testAccount))
        .onSuccess(
          response -> testContext.verify(() -> assertEquals(response.statusCode(), 400))
        ).onComplete(httpClientResponse -> testContext.completeNow())
      );


  }

  void createAccount(HttpClient httpClient, AccountRequest accountRequest) {
    httpClient.request(HttpMethod.POST, PORT, HOST, ACCOUNTS_PATH)
      .flatMap(req -> req.putHeader(CONTENT_TYPE_KEY, CONTENT_TYPE_JSON)
        .send(Json.encode(accountRequest))
        .mapEmpty()
      );
  }

  @AfterEach
  void close(Vertx vertx, VertxTestContext testContext) {
    vertx.close(voidAsyncResult -> testContext.completeNow());
  }
}
