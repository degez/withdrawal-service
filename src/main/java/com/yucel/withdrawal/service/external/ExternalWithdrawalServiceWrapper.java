package com.yucel.withdrawal.service.external;

import com.yucel.withdrawal.domain.model.WithdrawalStatus;
import com.yucel.withdrawal.service.external.model.Amount;

import java.math.BigDecimal;
import java.util.UUID;

public class ExternalWithdrawalServiceWrapper implements ExternalWithdrawalService{
  private final WithdrawalService withdrawalService;

  public ExternalWithdrawalServiceWrapper() {
    withdrawalService = new WithdrawalServiceStub();
  }

  @Override
  public void requestExternalWithdrawal(UUID id, String address, BigDecimal amount) {
    WithdrawalService.WithdrawalId withdrawalId = new WithdrawalService.WithdrawalId(id);
    WithdrawalService.Address addressRecord = new WithdrawalService.Address(address);
    Amount amountRecord = new Amount(amount);
    withdrawalService.requestWithdrawal(withdrawalId, addressRecord, amountRecord);
  }

  @Override
  public WithdrawalStatus getRequestState(UUID id) {
    WithdrawalService.WithdrawalId withdrawalId = new WithdrawalService.WithdrawalId(id);
    return WithdrawalStatus.valueOf(withdrawalService.getRequestState(withdrawalId).name());
  }
}
