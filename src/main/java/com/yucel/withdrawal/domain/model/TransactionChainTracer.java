package com.yucel.withdrawal.domain.model;

public class TransactionChainTracer {
  private boolean isWithdrawalPersisted;
  private boolean isBalanceCorrected;

  public boolean isWithdrawalPersisted() {
    return isWithdrawalPersisted;
  }

  public void setWithdrawalPersisted(boolean withdrawalPersisted) {
    isWithdrawalPersisted = withdrawalPersisted;
  }

  public boolean isBalanceCorrected() {
    return isBalanceCorrected;
  }

  public void setBalanceCorrected(boolean balanceCorrected) {
    isBalanceCorrected = balanceCorrected;
  }
}
