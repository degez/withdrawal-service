package com.yucel.withdrawal.service;

import com.yucel.withdrawal.domain.entity.WithdrawalTransaction;
import com.yucel.withdrawal.domain.model.WithdrawalStatus;
import com.yucel.withdrawal.repository.WithdrawalRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class NeverlessWithdrawalStoreService implements WithdrawalStoreService {

  private final WithdrawalRepository withdrawalRepository;

  public NeverlessWithdrawalStoreService(WithdrawalRepository withdrawalRepository) {
    this.withdrawalRepository = withdrawalRepository;
  }

  @Override
  public Optional<WithdrawalStatus> getWithdrawalStatusById(UUID id) {
    Optional<WithdrawalTransaction> withdrawalTransaction = withdrawalRepository.get(id);

    return withdrawalTransaction.map(WithdrawalTransaction::status);

  }

  @Override
  public void saveWithdrawal(WithdrawalTransaction withdrawalTransaction) {

    withdrawalRepository.save(withdrawalTransaction);
  }

  @Override
  public Optional<WithdrawalTransaction> getWithdrawalTransactionById(UUID id) {
    return withdrawalRepository.get(id);
  }

  @Override
  public boolean checkIfWithdrawalTransactionExists(UUID id) {
    return withdrawalRepository.checkIfWithdrawalTransactionExists(id);
  }

  @Override
  public List<WithdrawalTransaction> getAllWithdrawals() {
    return withdrawalRepository.getAll();
  }
}
