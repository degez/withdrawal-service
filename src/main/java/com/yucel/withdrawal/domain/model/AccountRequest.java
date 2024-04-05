package com.yucel.withdrawal.domain.model;

import java.math.BigDecimal;

public record AccountRequest(String address, BigDecimal balance) {
}
