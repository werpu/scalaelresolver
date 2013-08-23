package com.github.werpu.scalaelresolver.scalaelresolvertest

import javax.inject.Named
import javax.enterprise.context.RequestScoped
import scala.reflect.BeanProperty

@Named
@RequestScoped
class Bean3 {
  @BeanProperty
  var test1 = "hello world from bean3"
  @BeanProperty
  var test2 = "hello world from bean 3 test2"

}
