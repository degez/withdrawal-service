package com.yucel.withdrawal.repository;

import com.yucel.withdrawal.domain.entity.WithdrawalTransaction;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InMemoryWithdrawalRepository implements WithdrawalRepository {
  private final ConcurrentMap<UUID, WithdrawalTransaction> withdrawalTransactionMap;

  public InMemoryWithdrawalRepository() {
    this.withdrawalTransactionMap = new ConcurrentHashMap<>();
  }

  @Override
  public WithdrawalTransaction save(WithdrawalTransaction withdrawalTransaction) {
    return withdrawalTransactionMap.put(withdrawalTransaction.id(), withdrawalTransaction);
  }

  @Override
  public Optional<WithdrawalTransaction> get(UUID id) {
    return Optional.ofNullable(withdrawalTransactionMap.get(id));
  }

  @Override
  public boolean checkIfWithdrawalTransactionExists(UUID id) {
    return withdrawalTransactionMap.containsKey(id);
  }

  @Override
  public List<WithdrawalTransaction> getAll() {
    return new ArrayList<>(withdrawalTransactionMap.values());
  }
}
