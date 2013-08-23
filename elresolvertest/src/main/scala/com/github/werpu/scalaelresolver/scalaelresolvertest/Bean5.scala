package com.github.werpu.scalaelresolver.scalaelresolvertest

import javax.enterprise.context.RequestScoped
import javax.inject.Named

@Named
@RequestScoped
class Bean5 extends BaseBean {
  var hello2 = "hello from bean5"
  var hello3:Int = 1
}
