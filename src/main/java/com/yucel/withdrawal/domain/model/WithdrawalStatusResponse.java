package com.yucel.withdrawal.domain.model;

import java.util.UUID;

public record WithdrawalStatusResponse(UUID withdrawalId, WithdrawalStatus status) {
}
