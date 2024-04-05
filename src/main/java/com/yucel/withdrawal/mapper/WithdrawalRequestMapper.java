package com.yucel.withdrawal.mapper;

import com.yucel.withdrawal.domain.entity.WithdrawalTransaction;
import com.yucel.withdrawal.domain.model.TransferAddress;
import com.yucel.withdrawal.domain.model.TransferAmount;
import com.yucel.withdrawal.domain.model.WithdrawalRequest;
import com.yucel.withdrawal.domain.model.WithdrawalStatus;

import java.util.UUID;

public class WithdrawalRequestMapper {

  public WithdrawalTransaction mapToWithdrawalTransaction(WithdrawalRequest withdrawalRequest) {
    return new WithdrawalTransaction(
      UUID.randomUUID(),
      new TransferAddress(withdrawalRequest.fromAccountAddress()),
      new TransferAddress(withdrawalRequest.toAccountAddress()),
      new TransferAmount(withdrawalRequest.amount()),
      WithdrawalStatus.PROCESSING
    );
  }
}
