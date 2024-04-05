package com.yucel.withdrawal.service.verticle;

import com.yucel.withdrawal.domain.entity.Account;
import com.yucel.withdrawal.domain.entity.WithdrawalTransaction;
import com.yucel.withdrawal.domain.model.*;
import com.yucel.withdrawal.mapper.WithdrawalRequestMapper;
import com.yucel.withdrawal.repository.InMemoryAccountRepository;
import com.yucel.withdrawal.repository.InMemoryWithdrawalRepository;
import com.yucel.withdrawal.service.AccountService;
import com.yucel.withdrawal.service.NeverlessAccountService;
import com.yucel.withdrawal.service.NeverlessWithdrawalStoreService;
import com.yucel.withdrawal.service.WithdrawalStoreService;
import com.yucel.withdrawal.service.external.ExternalWithdrawalService;
import com.yucel.withdrawal.service.external.ExternalWithdrawalServiceWrapper;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import static com.yucel.withdrawal.service.verticle.util.VerticleConstantUtil.*;

public class WithdrawalAndAccountOperationsVerticle extends AbstractVerticle {

  public static final long STATUS_CHECK_INTERVAL = 1000L;

  private WithdrawalRequestMapper withdrawalRequestMapper;
  private AccountService accountService;
  private WithdrawalStoreService withdrawalStoreService;
  private ExternalWithdrawalService externalWithdrawalService;
  private ConcurrentLinkedQueue<WithdrawalTransaction> statusAwaitingWithdrawals;

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    withdrawalRequestMapper = new WithdrawalRequestMapper();
    accountService = new NeverlessAccountService(new InMemoryAccountRepository());
    withdrawalStoreService = new NeverlessWithdrawalStoreService(new InMemoryWithdrawalRepository());
    externalWithdrawalService = new ExternalWithdrawalServiceWrapper();
    statusAwaitingWithdrawals = new ConcurrentLinkedQueue<>();

    vertx.eventBus().consumer(GET_WITHDRAWAL_STATUS_ADDRESS).handler(this::handleGetWithdrawalStatus);
    vertx.eventBus().consumer(CREATE_WITHDRAWAL_REQUEST_ADDRESS).handler(this::handleCreateWithdrawalRequest);
    vertx.eventBus().consumer(GET_WITHDRAWAL_BY_ID_REQUEST_ADDRESS).handler(this::handleGetWithdrawalByIdRequest);
    vertx.eventBus().consumer(GET_ALL_WITHDRAWALS_REQUEST_ADDRESS).handler(this::handleGetAllWithdrawalsRequest);
    vertx.eventBus().consumer(GET_ALL_ACCOUNTS_REQUEST_ADDRESS).handler(this::handleGetAllAccountsRequest);
    vertx.eventBus().consumer(CREATE_ACCOUNT_REQUEST_ADDRESS).handler(this::handleCreateAccountRequest);
    vertx.eventBus().consumer(UPDATE_ACCOUNT_BALANCE_REQUEST_ADDRESS).handler(this::handleUpdateAccountBalanceRequest);
    vertx.eventBus().consumer(GET_ACCOUNT_BY_ADDRESS_REQUEST_ADDRESS).handler(this::handleGetAccountByAddressRequest);


    vertx.setPeriodic(STATUS_CHECK_INTERVAL, timerId -> handleWithdrawalStatusUpdates());

    startPromise.complete();
  }

  /**
   * This method handles periodic status checks. If the withdrawal COMPLETED updates the state,
   * if it failed or we get an error it marks it as FAILED
   * and returns the amount back to the balance
   */
  private void handleWithdrawalStatusUpdates() {
    statusAwaitingWithdrawals.removeIf(withdrawalTransaction -> {
      WithdrawalStatus status = null;
      boolean withdrawalDoesNotExist = false;
      boolean unknownExternalServiceError = false;
      try {
        status = externalWithdrawalService.getRequestState(withdrawalTransaction.id());
      } catch (IllegalArgumentException argumentException) {
        withdrawalDoesNotExist = true;
      } catch (Exception e) {
        unknownExternalServiceError = true;
      }

      if (WithdrawalStatus.COMPLETED.equals(status)) {

        withdrawalStoreService.saveWithdrawal(withdrawalTransaction.changeStatus(status));
        return true;
      } else if (WithdrawalStatus.FAILED.equals(status) || withdrawalDoesNotExist || unknownExternalServiceError) {
        // fail and correct balance
        withdrawalStoreService.saveWithdrawal(withdrawalTransaction.changeStatus(WithdrawalStatus.FAILED));
        accountService.depositToAccount(withdrawalTransaction.fromAccountAddress().address(), withdrawalTransaction.amount().amount());
        return true;
      } else {
        return false;
      }
    });
  }

  private void handleGetAccountByAddressRequest(Message<Object> message) {
    JsonObject messageJson = (JsonObject) message.body();
    TransferAddress transferAddress = messageJson.mapTo(TransferAddress.class);

    Optional<Account> accountByAddress = accountService.getAccountByAddress(transferAddress.address());

    if (accountByAddress.isPresent()) {
      message.reply(JsonObject.mapFrom(accountByAddress.get()));
    } else {
      message.fail(HttpResponseStatus.NOT_FOUND.code(), "account with address: %s does not exist".formatted(transferAddress.address()));
    }
  }

  private void handleUpdateAccountBalanceRequest(Message<Object> message) {
    JsonObject messageJson = (JsonObject) message.body();
    AccountRequest accountRequest = messageJson.mapTo(AccountRequest.class);

    boolean isValid = validateUpdateAccountRequest(message, accountRequest);

    if (isValid) {
      Account account = accountService.depositToAccount(accountRequest.address(), accountRequest.balance());
      message.reply(JsonObject.mapFrom(account));
    }
  }

  private boolean validateUpdateAccountRequest(Message<Object> message, AccountRequest accountRequest) {
    boolean accountExists = accountService.checkIfAccountExists(accountRequest.address());
    if (!accountExists) {
      message.fail(HttpResponseStatus.NOT_FOUND.code(), "account with address: %s not found".formatted(accountRequest.address()));
      return false;
    } else if (accountRequest.balance().compareTo(BigDecimal.ZERO) < 1) {
      message.fail(HttpResponseStatus.UNPROCESSABLE_ENTITY.code(), "update balance amount cannot be negative or zero");
      return false;
    }
    return true;
  }

  private void handleCreateAccountRequest(Message<Object> message) {
    JsonObject messageJson = (JsonObject) message.body();
    Account account = messageJson.mapTo(Account.class);

    boolean hasFailed = failIfAccountAlreadyExists(message, account);

    if (!hasFailed) {
      accountService.createAccount(account);
      message.reply("account created");
    }
  }

  private boolean failIfAccountAlreadyExists(Message<Object> message, Account account) {
    boolean accountExists = accountService.checkIfAccountExists(account.getAddress());

    if (accountExists) {
      message.fail(HttpResponseStatus.CONFLICT.code(), "account with address: %s already exists".formatted(account.getAddress()));
      return true;
    }
    return false;
  }

  private void handleGetAllAccountsRequest(Message<Object> message) {
    List<Account> allAccounts = accountService.getAllAccounts();
    String encoded = Json.encode(allAccounts);

    message.reply(encoded);
  }

  private void handleGetAllWithdrawalsRequest(Message<Object> message) {

    List<WithdrawalTransaction> allWithdrawals = withdrawalStoreService.getAllWithdrawals();
    String encoded = Json.encode(allWithdrawals);
    message.reply(encoded);
  }

  private void handleGetWithdrawalByIdRequest(Message<Object> message) {
    JsonObject messageJson = (JsonObject) message.body();
    String withdrawalId = messageJson.getString("withdrawalId");

    try {
      UUID uuid = UUID.fromString(withdrawalId);
      if (!withdrawalStoreService.checkIfWithdrawalTransactionExists(uuid)) {
        message.fail(HttpResponseStatus.NOT_FOUND.code(), "withdrawal with id: %s does not exist".formatted(withdrawalId));
      } else {

        Optional<WithdrawalTransaction> withdrawalTransaction = withdrawalStoreService.getWithdrawalTransactionById(uuid);
        withdrawalTransaction.ifPresent(transaction -> message.reply(JsonObject.mapFrom(transaction)));
      }
    } catch (Exception e) {
      message.fail(HttpResponseStatus.INTERNAL_SERVER_ERROR.code(), e.getMessage());
    }
  }

  private void handleCreateWithdrawalRequest(Message<Object> message) {

    JsonObject messageJson = (JsonObject) message.body();
    WithdrawalRequest withdrawalRequest = messageJson.mapTo(WithdrawalRequest.class);

    WithdrawalTransaction withdrawalTransaction = withdrawalRequestMapper.mapToWithdrawalTransaction(withdrawalRequest);

    String fromAccountAddress = withdrawalRequest.fromAccountAddress();
    if (!accountService.checkIfAccountExists(fromAccountAddress)) {

      message.fail(HttpResponseStatus.NOT_FOUND.code(), "account with address %s not found".formatted(fromAccountAddress));
    } else {
      // if the recipient is not in our accounts we will use external service
      String toAccountAddress = withdrawalRequest.toAccountAddress();
      if (!accountService.checkIfAccountExists(toAccountAddress)) {
        // handle external
        TransactionChainTracer transactionChainTracer = new TransactionChainTracer();

        try {
          externalWithdrawalOperations(withdrawalTransaction, transactionChainTracer, withdrawalRequest);
          message.reply("withdrawal succeeded");
        } catch (IllegalStateException stateException) {

          message.fail(HttpResponseStatus.CONFLICT.code(), stateException.getMessage());
        } catch (IllegalArgumentException exception) {

          message.fail(HttpResponseStatus.UNPROCESSABLE_ENTITY.code(), exception.getMessage());
        } catch (Exception e) {

          message.fail(HttpResponseStatus.INTERNAL_SERVER_ERROR.code(), e.getMessage());
        } finally {

          rollbackStatesAndFailIfChainIsBroken(transactionChainTracer, withdrawalTransaction, withdrawalRequest);
        }
      } else {
        // handle internal
        TransactionChainTracer transactionChainTracer = new TransactionChainTracer();

        try {
          internalWithdrawOperations(withdrawalTransaction, transactionChainTracer, withdrawalRequest);
          message.reply("withdrawal succeeded");
        } catch (Exception e) {
          message.fail(HttpResponseStatus.INTERNAL_SERVER_ERROR.code(), e.getMessage());
        } finally {
          rollbackStatesAndFailIfChainIsBroken(transactionChainTracer, withdrawalTransaction, withdrawalRequest);
        }
      }
    }
  }

  private void rollbackStatesAndFailIfChainIsBroken(TransactionChainTracer transactionChainTracer, WithdrawalTransaction withdrawalTransaction, WithdrawalRequest withdrawalRequest) {
    if (!transactionChainTracer.isWithdrawalPersisted()) {
      withdrawalStoreService.saveWithdrawal(withdrawalTransaction.changeStatus(WithdrawalStatus.FAILED));
    }
    if (!transactionChainTracer.isBalanceCorrected()) {
      accountService.depositToAccount(withdrawalRequest.fromAccountAddress(), withdrawalRequest.amount());
    }
  }

  private void internalWithdrawOperations(WithdrawalTransaction withdrawalTransaction, TransactionChainTracer transactionChainTracer, WithdrawalRequest withdrawalRequest) {
    withdrawalStoreService.saveWithdrawal(withdrawalTransaction);
    transactionChainTracer.setWithdrawalPersisted(true);

    accountService.withdrawFromAccount(withdrawalRequest.fromAccountAddress(), withdrawalRequest.amount());
    accountService.depositToAccount(withdrawalRequest.toAccountAddress(), withdrawalRequest.amount());
    transactionChainTracer.setBalanceCorrected(true);

    withdrawalStoreService.saveWithdrawal(withdrawalTransaction.changeStatus(WithdrawalStatus.COMPLETED));
  }

  private void externalWithdrawalOperations(WithdrawalTransaction withdrawalTransaction, TransactionChainTracer transactionChainTracer, WithdrawalRequest withdrawalRequest) {
    externalWithdrawalService.requestExternalWithdrawal(withdrawalTransaction.id(), withdrawalTransaction.toAccountAddress().address(), withdrawalTransaction.amount().amount());
    withdrawalStoreService.saveWithdrawal(withdrawalTransaction);
    transactionChainTracer.setWithdrawalPersisted(true);

    accountService.withdrawFromAccount(withdrawalRequest.fromAccountAddress(), withdrawalRequest.amount());
    transactionChainTracer.setBalanceCorrected(true);

    statusAwaitingWithdrawals.add(withdrawalTransaction);
  }

  private void handleGetWithdrawalStatus(Message<Object> message) {

    JsonObject messageJson = (JsonObject) message.body();
    String withdrawalId = messageJson.getString("withdrawalId");

    try {
      UUID uuid = UUID.fromString(withdrawalId);
      if (!withdrawalStoreService.checkIfWithdrawalTransactionExists(uuid)) {
        message.fail(HttpResponseStatus.NOT_FOUND.code(), "withdrawal with id: %s does not exist".formatted(withdrawalId));
      } else {

        Optional<WithdrawalStatus> withdrawalStatusById = withdrawalStoreService.getWithdrawalStatusById(uuid);
        if (withdrawalStatusById.isPresent()) {
          WithdrawalStatusResponse withdrawalStatusResponse = new WithdrawalStatusResponse(uuid, withdrawalStatusById.get());
          message.reply(JsonObject.mapFrom(withdrawalStatusResponse));
        }

      }
    } catch (Exception e) {
      message.fail(HttpResponseStatus.INTERNAL_SERVER_ERROR.code(), e.getMessage());
    }
  }
}
