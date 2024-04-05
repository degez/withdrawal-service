package com.yucel.withdrawal.service;

import com.yucel.withdrawal.domain.entity.WithdrawalTransaction;
import com.yucel.withdrawal.domain.model.WithdrawalStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WithdrawalStoreService {

  /**
   * Finds withdrawal and extracts the status
   * @param id UUID of withdrawal
   * @return an optional WithdrawalStatus
   */
  Optional<WithdrawalStatus> getWithdrawalStatusById(UUID id);

  /**
   * gets withdrawal and persists it
   * @param withdrawalTransaction
   */
  void saveWithdrawal(WithdrawalTransaction withdrawalTransaction);

  /**
   * Gets the withdrawal
   * @param id UUID of the withdrawal
   * @return an optional WithdrawalTransaction
   */
  Optional<WithdrawalTransaction> getWithdrawalTransactionById(UUID id);

  /**
   * Makes a simple check if the withdrawal exists or not
   * @param id UUID of the withdrawal
   * @return boolean
   */
  boolean checkIfWithdrawalTransactionExists(UUID id);

  /**
   * Gets all withdrawals
   * @return a list of WithdrawalTransaction
   */
  List<WithdrawalTransaction> getAllWithdrawals();
}
