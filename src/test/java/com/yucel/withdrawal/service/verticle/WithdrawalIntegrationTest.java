package com.yucel.withdrawal.service.verticle;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yucel.withdrawal.MainVerticle;
import com.yucel.withdrawal.domain.entity.WithdrawalTransaction;
import com.yucel.withdrawal.domain.model.AccountRequest;
import com.yucel.withdrawal.domain.model.WithdrawalRequest;
import com.yucel.withdrawal.domain.model.WithdrawalStatus;
import com.yucel.withdrawal.domain.model.WithdrawalStatusResponse;
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
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(VertxExtension.class)
public class WithdrawalIntegrationTest {

  public static final int PORT = 8888;
  public static final String HOST = "localhost";
  public static final String CONTENT_TYPE_KEY = "Content-Type";
  public static final String CONTENT_TYPE_JSON = "application/json";
  public static final String WITHDRAWAL_PATH = "/withdrawals";
  public static final String ACCOUNTS_PATH = "/accounts";
  public static final String ADDRESS_1 = "DE123";
  public static final String ADDRESS_2 = "DE124";
  public static final String EXTERNAL_ADDRESS = "DE1234";
  public static final ObjectMapper MAPPER = new ObjectMapper();


  @BeforeEach
  void deploy_verticle(Vertx vertx, VertxTestContext testContext) {
    vertx.deployVerticle(new MainVerticle(), testContext.succeedingThenComplete());
  }

  @Test
  void givenValidAccount_whenExternalWithdraw_thenSuccess(Vertx vertx, VertxTestContext testContext) {
    final HttpClient client = vertx.createHttpClient();
    createAccounts(client);
    final BigDecimal transferAmount = BigDecimal.ONE;
    final BigDecimal initialBalance = BigDecimal.TEN;
    WithdrawalRequest withdrawalRequest = new WithdrawalRequest(ADDRESS_1, EXTERNAL_ADDRESS, transferAmount);
    client.request(HttpMethod.POST, PORT, HOST, WITHDRAWAL_PATH)
      .flatMap(req -> req.putHeader(CONTENT_TYPE_KEY, CONTENT_TYPE_JSON)
        .send(Json.encode(withdrawalRequest))
        .onSuccess(
          response -> testContext.verify(() -> assertEquals(response.statusCode(), 201))
        )
      )
      .flatMap(httpClientResponse -> Future.succeededFuture(ACCOUNTS_PATH))
      .flatMap(accountAddress -> client.request(HttpMethod.GET, PORT, HOST, accountAddress + "/%s".formatted(ADDRESS_1))
        .compose(req -> req.send().compose(HttpClientResponse::body))

        .onComplete(testContext.succeeding(buffer -> testContext.verify(() -> {

          assertTrue(Objects.nonNull(buffer));

          AccountRequest account = Json.decodeValue(buffer, AccountRequest.class);

          assertEquals(account.address(), ADDRESS_1);
          assertEquals(account.balance(), initialBalance.subtract(transferAmount));
          testContext.completeNow();
        }))));

  }

  @Test
  void givenValidAccount_whenInternalWithdraw_thenSuccess(Vertx vertx, VertxTestContext testContext) {
    final HttpClient client = vertx.createHttpClient();
    final BigDecimal transferAmount = BigDecimal.ONE;
    final BigDecimal initialBalance = BigDecimal.TEN;

    createAccounts(client);
    WithdrawalRequest withdrawalRequest = new WithdrawalRequest(ADDRESS_1, ADDRESS_2, transferAmount);
    client.request(HttpMethod.POST, PORT, HOST, WITHDRAWAL_PATH)
      .flatMap(req -> req.putHeader(CONTENT_TYPE_KEY, CONTENT_TYPE_JSON)
        .send(Json.encode(withdrawalRequest))
        .onSuccess(
          response -> testContext.verify(() -> assertEquals(response.statusCode(), 201))
        )
      )
      .flatMap(httpClientResponse -> Future.succeededFuture(ACCOUNTS_PATH))
      .flatMap(accountAddress -> client.request(HttpMethod.GET, PORT, HOST, accountAddress + "/%s".formatted(ADDRESS_1))
        .compose(req -> req.send().compose(HttpClientResponse::body))
        .onComplete(testContext.succeeding(buffer -> testContext.verify(() -> {
          assertTrue(Objects.nonNull(buffer));

          AccountRequest account = Json.decodeValue(buffer, AccountRequest.class);

          assertEquals(account.address(), ADDRESS_1);
          assertEquals(account.balance(), initialBalance.subtract(transferAmount));
          testContext.completeNow();
        }))));

  }

  @Test
  void givenValidAccount_whenExternalWithdraw_thenCorrectWithdrawalStatus(Vertx vertx, VertxTestContext testContext) {
    final HttpClient client = vertx.createHttpClient();
    final BigDecimal transferAmount = BigDecimal.ONE;

    createAccounts(client);
    WithdrawalRequest withdrawalRequest = new WithdrawalRequest(ADDRESS_1, EXTERNAL_ADDRESS, transferAmount);

    client.request(HttpMethod.POST, PORT, HOST, WITHDRAWAL_PATH)
      .flatMap(req -> req.putHeader(CONTENT_TYPE_KEY, CONTENT_TYPE_JSON)
        .send(Json.encode(withdrawalRequest))
        .onSuccess(
          response -> testContext.verify(() -> assertEquals(response.statusCode(), 201))
        )
      )
      .flatMap(httpClientResponse -> Future.succeededFuture(WITHDRAWAL_PATH))
      .flatMap(withdrawalPath -> client.request(HttpMethod.GET, PORT, HOST, withdrawalPath)
        .compose(req -> req.send().compose(HttpClientResponse::body))

        .onComplete(testContext.succeeding(buffer -> testContext.verify(() -> {

          assertTrue(Objects.nonNull(buffer));

          List<WithdrawalTransaction> withdrawalTransactions = MAPPER.readValue(buffer.toString(), new TypeReference<List<WithdrawalTransaction>>() {
          });
          assertEquals(withdrawalTransactions.size(), 1);
          WithdrawalTransaction withdrawalTransaction = withdrawalTransactions.getFirst();

          assertEquals(withdrawalTransaction.fromAccountAddress().address(), ADDRESS_1);
          assertEquals(withdrawalTransaction.status(), WithdrawalStatus.PROCESSING);
          assertEquals(withdrawalTransaction.amount().amount(), transferAmount);
          testContext.completeNow();

        }))));

  }

  @Test
  void givenValidWithdrawal_whenGetStatus_thenCorrectWithdrawalStatus(Vertx vertx, VertxTestContext testContext) {
    final HttpClient client = vertx.createHttpClient();
    final BigDecimal transferAmount = BigDecimal.ONE;

    createAccounts(client);
    WithdrawalRequest withdrawalRequest = new WithdrawalRequest(ADDRESS_1, EXTERNAL_ADDRESS, transferAmount);

    client.request(HttpMethod.POST, PORT, HOST, WITHDRAWAL_PATH)
      .flatMap(req -> req.putHeader(CONTENT_TYPE_KEY, CONTENT_TYPE_JSON)
        .send(Json.encode(withdrawalRequest))
        .onSuccess(
          response -> testContext.verify(() -> assertEquals(response.statusCode(), 201))
        )
      )
      .flatMap(httpClientResponse -> Future.succeededFuture(WITHDRAWAL_PATH))
      .flatMap(withdrawalPath ->
        client.request(HttpMethod.GET, PORT, HOST, withdrawalPath)
          .compose(httpClientRequest -> httpClientRequest.send().compose(HttpClientResponse::body))
          .flatMap(buffer -> {
            WithdrawalTransaction transaction = null;
            try {
              final List<WithdrawalTransaction> withdrawalTransactions = MAPPER.readValue(buffer.toString(), new TypeReference<List<WithdrawalTransaction>>() {
              });
              testContext.verify(() -> {
                assertNotNull(withdrawalTransactions);
                assertEquals(withdrawalTransactions.size(), 1);
              });
              transaction = withdrawalTransactions.getFirst();
            } catch (JsonProcessingException e) {
              testContext.failingThenComplete();
            }

            return Future.succeededFuture(transaction.id());
          })
      )
      .flatMap(uuid ->
        client.request(HttpMethod.GET, PORT, HOST, WITHDRAWAL_PATH + "/%s/%s".formatted(uuid.toString(), "status"))
          .compose(req -> req.send().compose(HttpClientResponse::body))
          .onComplete(testContext.succeeding(buffer -> testContext.verify(() -> {

            assertTrue(Objects.nonNull(buffer));

            WithdrawalStatusResponse withdrawalStatusResponse = Json.decodeValue(buffer, WithdrawalStatusResponse.class);
            assertEquals(withdrawalStatusResponse.withdrawalId(), uuid);
            assertEquals(withdrawalStatusResponse.status(), WithdrawalStatus.PROCESSING);
            testContext.completeNow();
          }))));

  }

  @Test
  void givenValidWithdrawal_whenGetById_thenSuccess(Vertx vertx, VertxTestContext testContext) {
    final HttpClient client = vertx.createHttpClient();
    final BigDecimal transferAmount = BigDecimal.ONE;

    createAccounts(client);
    WithdrawalRequest withdrawalRequest = new WithdrawalRequest(ADDRESS_1, EXTERNAL_ADDRESS, transferAmount);

    client.request(HttpMethod.POST, PORT, HOST, WITHDRAWAL_PATH)
      .flatMap(req -> req.putHeader(CONTENT_TYPE_KEY, CONTENT_TYPE_JSON)
        .send(Json.encode(withdrawalRequest))
        .onSuccess(
          response -> testContext.verify(() -> assertEquals(response.statusCode(), 201))
        )
      )
      .flatMap(httpClientResponse -> Future.succeededFuture(WITHDRAWAL_PATH))
      .flatMap(withdrawalPath ->
        client.request(HttpMethod.GET, PORT, HOST, withdrawalPath)
          .compose(httpClientRequest -> httpClientRequest.send().compose(HttpClientResponse::body))
          .flatMap(buffer -> {
            WithdrawalTransaction transaction = null;
            try {
              final List<WithdrawalTransaction> withdrawalTransactions = MAPPER.readValue(buffer.toString(), new TypeReference<List<WithdrawalTransaction>>() {
              });
              testContext.verify(() -> {
                assertNotNull(withdrawalTransactions);
                assertEquals(withdrawalTransactions.size(), 1);
              });
              transaction = withdrawalTransactions.getFirst();
            } catch (JsonProcessingException e) {
              testContext.failingThenComplete();
            }

            return Future.succeededFuture(transaction.id());
          })
      )
      .flatMap(uuid ->
        client.request(HttpMethod.GET, PORT, HOST, WITHDRAWAL_PATH + "/%s".formatted(uuid.toString()))
          .compose(req -> req.send().compose(HttpClientResponse::body))
          .onComplete(testContext.succeeding(buffer -> testContext.verify(() -> {

            assertTrue(Objects.nonNull(buffer));

            WithdrawalTransaction withdrawalTransaction = Json.decodeValue(buffer, WithdrawalTransaction.class);
            assertEquals(withdrawalTransaction.id(), uuid);
            assertEquals(withdrawalTransaction.status(), WithdrawalStatus.PROCESSING);
            testContext.completeNow();
          }))));

  }

  void createAccounts(HttpClient client) {
    final AccountRequest testAccount1 = new AccountRequest(ADDRESS_1, BigDecimal.TEN);
    final AccountRequest testAccount2 = new AccountRequest(ADDRESS_2, BigDecimal.TEN);

    List.of(testAccount1, testAccount2).forEach(accountRequest -> client.request(HttpMethod.POST, PORT, HOST, ACCOUNTS_PATH)
      .flatMap(req -> req.putHeader(CONTENT_TYPE_KEY, CONTENT_TYPE_JSON)
        .send(Json.encode(accountRequest))
        .mapEmpty()
      ));
  }


  @AfterEach
  void close(Vertx vertx, VertxTestContext testContext) {
    vertx.close(voidAsyncResult -> testContext.completeNow());
  }
}
