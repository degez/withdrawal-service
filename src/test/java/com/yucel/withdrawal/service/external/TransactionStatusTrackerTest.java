package com.yucel.withdrawal.service.external;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yucel.withdrawal.MainVerticle;
import com.yucel.withdrawal.domain.entity.WithdrawalTransaction;
import com.yucel.withdrawal.domain.model.AccountRequest;
import com.yucel.withdrawal.domain.model.WithdrawalRequest;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.ThreadingModel;
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
public class TransactionStatusTrackerTest {

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
  public void deploy_verticle(Vertx vertx, VertxTestContext testContext) {
    vertx.deployVerticle(new MainVerticle(), new DeploymentOptions().setThreadingModel(ThreadingModel.WORKER), testContext.succeedingThenComplete());
  }

  @Test
  public void givenValidWithdrawal_whenWithdrawExternal_thenVisitStatusTracker(Vertx vertx, VertxTestContext testContext) {


    final HttpClient client = vertx.createHttpClient();
    createAccounts(client);
    final BigDecimal transferAmount = BigDecimal.ONE;
    WithdrawalRequest withdrawalRequest = new WithdrawalRequest(ADDRESS_1, EXTERNAL_ADDRESS, transferAmount);
    client.request(HttpMethod.POST, PORT, HOST, WITHDRAWAL_PATH)
      .flatMap(req -> req.putHeader(CONTENT_TYPE_KEY, CONTENT_TYPE_JSON)
        .send(Json.encode(withdrawalRequest))
        .onSuccess(
          response -> testContext.verify(() -> assertEquals(response.statusCode(), 201))
        )
      )
      .flatMap(httpClientResponse -> {
        try {
          Thread.sleep(1100L);
          // withdrawal status update periodic method visited
        } catch (InterruptedException e) {
          testContext.failingThenComplete();
        }
        return Future.succeededFuture(WITHDRAWAL_PATH);
      })
      .flatMap(withdrawalPath -> client.request(HttpMethod.GET, PORT, HOST, withdrawalPath)
        .compose(req -> req.send().compose(HttpClientResponse::body))

        .onComplete(testContext.succeeding(buffer -> testContext.verify(() -> {

          assertTrue(Objects.nonNull(buffer));

          List<WithdrawalTransaction> withdrawalTransactions = null;
          try {
            withdrawalTransactions = MAPPER.readValue(buffer.toString(), new TypeReference<List<WithdrawalTransaction>>() {
            });
          } catch (JsonProcessingException e) {
            testContext.failingThenComplete();
          }
          assertNotNull(withdrawalTransactions);
          assertEquals(withdrawalTransactions.size(), 1);
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
      ).result());
  }


  @AfterEach
  void close(Vertx vertx, VertxTestContext testContext) {
    vertx.close(voidAsyncResult -> testContext.completeNow());
  }
}
