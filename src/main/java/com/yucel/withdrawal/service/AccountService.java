package com.yucel.withdrawal.service;

import com.yucel.withdrawal.domain.entity.Account;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface AccountService {

  /**
   * To make a simple check if account is already exists or not
   * @param address account address
   * @return boolean
   */
  boolean checkIfAccountExists(String address);

  /**
   * Persists the Account
   * @param account account to be saved
   */
  void createAccount(Account account);

  /**
   * Deposits given amount to the balance of the Account
   * @param address account address
   * @param amount amount to be deposited
   * @return updated Account
   */
  Account depositToAccount(String address, BigDecimal amount);

  /**
   * Withdraws given amount to the balance of the Account
   * @param address account address
   * @param amount amount to be withdrawn
   * @return updated Account
   */
  Account withdrawFromAccount(String address, BigDecimal amount);

  /**
   * Gets all accounts
   * @return list of Account
   */
  List<Account> getAllAccounts();

  /**
   * Gets the account and provides an Optional Account object
   * @param address address of the account
   * @return an optional Account
   */
  Optional<Account> getAccountByAddress(String address);
}
