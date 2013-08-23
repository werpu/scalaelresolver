package com.github.werpu.scalaelresolver.scalaelresolvertest

import javax.enterprise.context.RequestScoped
import javax.inject.Named
import java.io.Serializable

@Named
@RequestScoped
class Bean5 extends BaseBean with Serializable  {
  var hello2 = "hello from bean5"
  var hello3:Int = 1
}
