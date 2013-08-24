/*
 * Copyright 2013 Werner Punz
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at

 *  http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

  var prop4 = Map("key1"->("value1"), "key2"->"value2")
  var prop5 = List("val1","val2","val3")

  def prop2 = {
    _prop2
  }

  def prop2_$eq(in: String) {
    _prop2 = in
  }



  def doSubmitMe {}
}
