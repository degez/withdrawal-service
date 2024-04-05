package com.yucel.withdrawal.repository;

import com.yucel.withdrawal.domain.entity.WithdrawalTransaction;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WithdrawalRepository {

  WithdrawalTransaction save(WithdrawalTransaction withdrawalTransaction);

  Optional<WithdrawalTransaction> get(UUID id);

  boolean checkIfWithdrawalTransactionExists(UUID id);

  List<WithdrawalTransaction> getAll();

}
