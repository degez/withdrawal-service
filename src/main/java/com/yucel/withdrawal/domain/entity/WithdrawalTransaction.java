package com.yucel.withdrawal.domain.entity;

import com.yucel.withdrawal.domain.model.TransferAddress;
import com.yucel.withdrawal.domain.model.TransferAmount;
import com.yucel.withdrawal.domain.model.WithdrawalStatus;

import java.util.UUID;

public record WithdrawalTransaction(UUID id, TransferAddress fromAccountAddress, TransferAddress toAccountAddress, TransferAmount amount, WithdrawalStatus status) {

  public WithdrawalTransaction changeStatus(WithdrawalStatus newStatus) {
    return new WithdrawalTransaction(this.id, this.fromAccountAddress, this.toAccountAddress, this.amount, newStatus);
  }
}
