package com.yucel.withdrawal.repository;

import com.yucel.withdrawal.domain.entity.Account;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InMemoryAccountRepository implements AccountRepository {
  private final ConcurrentMap<String, Account> accountMap;

  public InMemoryAccountRepository() {
    this.accountMap = new ConcurrentHashMap<>();
  }

  @Override
  public Account save(Account account) {
    return accountMap.put(account.getAddress(), account);
  }

  @Override
  public Optional<Account> get(String address) {
    return Optional.ofNullable(accountMap.get(address));
  }

  @Override
  public Account deposit(String address, BigDecimal amount) {
    Account updatedAccount = accountMap.computeIfPresent(address, (addressKey, account) -> {

      account.setBalance(account.getBalance().add(amount));
      return account;
    });

    validateAccountExistence(address, updatedAccount);

    return updatedAccount;
  }



  @Override
  public Account withdraw(String address, BigDecimal amount) {
    Account updatedAccount = accountMap.computeIfPresent(address, (addressKey, account) -> {
      if (account.getBalance().compareTo(amount) < 0) {
        throw new IllegalArgumentException("account %s balance is not sufficient for the amount %,.2f".formatted(address, amount));
      }
      account.setBalance(account.getBalance().subtract(amount));
      return account;
    });

    validateAccountExistence(address, updatedAccount);

    return updatedAccount;
  }

  @Override
  public List<Account> getAll() {
    return new ArrayList<>(accountMap.values());
  }

  private static void validateAccountExistence(String address, Account updatedAccount) {
    if(updatedAccount == null) {
      throw new IllegalStateException("account with id: %s does not exist".formatted(address));
    }
  }

}
