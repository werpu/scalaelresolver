package com.github.werpu.scalaelresolver.scalaelresolvertest

import javax.inject.{Inject, Named}
import javax.enterprise.context.RequestScoped

@Named
@RequestScoped
class Bean3 {
  var test1 = "hello world from bean3"
  var test2 = "hello world from bean 3 test2"

  @Inject
  var bean4 : Bean4 = null

}
