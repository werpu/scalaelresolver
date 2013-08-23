package com.github.werpu.scalaelresolver.scalaelresolvertest

import javax.faces.bean.{ManagedProperty, RequestScoped, ManagedBean}
import scala.reflect.BeanProperty

/**
 * Sample Bean
 */
@ManagedBean
@RequestScoped
class Bean1 {
  var prop1 = "Hello world from prop1"
  private var _prop2 = "Hello world from prop2"

  //standard bean property also must be accessible
  @BeanProperty
  var prop3 = "hello world from prop3"

  @ManagedProperty(value =  "#{bean2}")
  var bean2: Bean2 = null

  def prop2 = {
    _prop2
  }

  def prop2_$eq(in: String) {
    _prop2 = in
  }

  def doSubmitMe(){}
}
