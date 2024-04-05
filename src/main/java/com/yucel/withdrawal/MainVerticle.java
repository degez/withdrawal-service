package com.yucel.withdrawal;

import com.yucel.withdrawal.service.verticle.HttpServerVerticle;
import com.yucel.withdrawal.service.verticle.WithdrawalAndAccountOperationsVerticle;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;

public class MainVerticle extends AbstractVerticle {

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    vertx.deployVerticle(WithdrawalAndAccountOperationsVerticle.class.getName());
    vertx.deployVerticle(HttpServerVerticle.class.getName());
    startPromise.complete();
  }

}
