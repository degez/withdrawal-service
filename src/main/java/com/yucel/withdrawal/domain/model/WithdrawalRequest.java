package com.yucel.withdrawal.domain.model;

import java.math.BigDecimal;

public record WithdrawalRequest(
  String fromAccountAddress,
  String toAccountAddress,
  BigDecimal amount) {
}
