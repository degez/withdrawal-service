package com.yucel.withdrawal.repository;

import com.yucel.withdrawal.domain.entity.Account;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface AccountRepository {

  Account save(Account account);
  Optional<Account> get(String address);

  /**
   * Verifies the account and deposits the amount to the given address
   * @param address address of the account
   * @param amount amount to be deposited
   * @return updated account
   * @throws IllegalStateException if the account does not exist
   */
  Account deposit(String address, BigDecimal amount);

  /**
   * Verifies if the balance is enough for the amount to be subtracted.
   * Also verifies the account, and withdraws the amount
   * @param address address of the account
   * @param amount amount to be withdrawn
   * @return updated account
   * @throws IllegalArgumentException if the balance is not sufficient
   * @throws IllegalStateException if the account does not exist
   */
  Account withdraw(String address, BigDecimal amount);

  /**
   * Gets all the accounts
   * @return list of Account
   */
  List<Account> getAll();
}
