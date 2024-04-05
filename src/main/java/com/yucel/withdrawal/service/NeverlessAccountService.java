package com.yucel.withdrawal.service;

import com.yucel.withdrawal.domain.entity.Account;
import com.yucel.withdrawal.repository.AccountRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public class NeverlessAccountService implements AccountService {

  private final AccountRepository accountRepository;

  public NeverlessAccountService(AccountRepository accountRepository) {
    this.accountRepository = accountRepository;
  }


  @Override
  public boolean checkIfAccountExists(String address) {
    return accountRepository.get(address).isPresent();
  }

  @Override
  public void createAccount(Account account) {
    accountRepository.save(account);
  }

  @Override
  public Account depositToAccount(String address, BigDecimal amount) {
    return accountRepository.deposit(address, amount);
  }

  @Override
  public Account withdrawFromAccount(String address, BigDecimal amount) {
    return accountRepository.withdraw(address, amount);
  }

  @Override
  public List<Account> getAllAccounts() {
    return accountRepository.getAll();
  }

  @Override
  public Optional<Account> getAccountByAddress(String address) {
    return accountRepository.get(address);
  }
}
