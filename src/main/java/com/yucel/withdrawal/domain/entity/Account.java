package com.yucel.withdrawal.domain.entity;

import java.math.BigDecimal;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Account {
  /**
   * this was added to add thread safety in case the balance is read while there is a write operation
   */
  private final ReadWriteLock lock = new ReentrantReadWriteLock();

  private String address;
  private BigDecimal balance;

  public Account() {
  }

  public Account(String address, BigDecimal balance) {
    this.address = address;
    this.balance = balance;
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public BigDecimal getBalance() {
    lock.readLock().lock();
    try {
      return balance;
    } finally {
      lock.readLock().unlock();
    }
  }

  public void setBalance(BigDecimal balance) {
    lock.writeLock().lock();
    try {
      this.balance = balance;
    } finally {
      lock.writeLock().unlock();
    }
  }

  @Override
  public String toString() {
    return "Account{" +
      "address='" + address + '\'' +
      ", balance=" + balance +
      '}';
  }
}
