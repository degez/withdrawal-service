package com.yucel.withdrawal.domain.model;

import java.math.BigDecimal;

public record UpdateBalanceRequest(BigDecimal balance) {
}
