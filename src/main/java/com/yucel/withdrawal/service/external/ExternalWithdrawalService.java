package com.yucel.withdrawal.service.external;

import com.yucel.withdrawal.domain.model.WithdrawalStatus;

import java.math.BigDecimal;
import java.util.UUID;

public interface ExternalWithdrawalService {

  void requestExternalWithdrawal(UUID id, String address, BigDecimal amount);

  WithdrawalStatus getRequestState(UUID id);

}
