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
package com.github.werpu.scalaelresolver.scalaelresolver

import javax.el.{ELContext, ELResolver}
import java.beans.FeatureDescriptor
import java.lang.reflect.Method

import collection.JavaConversions._
import collection.mutable.HashSet
import scala.collection.JavaConversions

/**
 *
 * @author Werner Punz (latest modification by $Author$)
 * @version $Revision$ $Date$
 *
 *          A custom el resolver which needs to do following things
 *          call the getters and setters of scala according to the scala convention of
 *          attr() and attr_$eq(value)
 *          It also converts scala collections to the collections JSF understands
 */

object CONST {
  val GET_PREFIX = "get"
  val SET_PREFIX = "set"
  val EQ_REGEXP = "\\_\\$eƒq"
  val SCALA_SET_POSTFIX = "_$eq"
}

class ScalaELResolver extends ELResolver {

  import CONST._

  def getCommonPropertyType(elContext: ELContext, o: AnyRef): Class[_] = {
    o.getClass
  }

  def getFeatureDescriptors(elContext: ELContext, base: AnyRef): java.util.Iterator[FeatureDescriptor] = {

    if (!base.isInstanceOf[scala.ScalaObject]) {
      //no scala object we forward it to another el
      //resolver
      null
    } else {
      val ret = new HashSet[FeatureDescriptor]
      //We have to iterate over all properties of the base and return it
      //as feature descriptor instance
      val methods: Array[Method] = base.getClass.getMethods
      val alreadyProcessed = new HashSet[String]
      for (method <- methods if (!alreadyProcessed.contains(method.getName.replaceAll(EQ_REGEXP, "")))) {
        //note every attribute of a scala object
        //is set as protected or private
        //with two encapsulating functions
        var methodName = method.getName.replaceAll(EQ_REGEXP, "")
        alreadyProcessed += methodName
        ret += makeDescriptor(methodName, methodName, base.getClass)
      }

      mutableSetAsJavaSet[FeatureDescriptor](ret).iterator
    }

  }

  /**
   * backported from myfaces
   */
  private def makeDescriptor(name: String, description: String,
                             objectType: Class[_]): FeatureDescriptor = {
    val fd = new FeatureDescriptor()
    fd.setValue(ELResolver.RESOLVABLE_AT_DESIGN_TIME, true)
    fd.setValue(ELResolver.TYPE, objectType)
    fd.setName(name)
    fd.setDisplayName(name)
    fd.setShortDescription(description)
    fd.setExpert(false)
    fd.setHidden(false)
    fd.setPreferred(true)
    fd
  }

  def getType(elContext: ELContext, base: AnyRef, prop: AnyRef): Class[_] = {
    if (base == null || !base.isInstanceOf[scala.ScalaObject]) null
    else if (base != null && prop == null) null
    else {
      val javaGetterName = GET_PREFIX + toBeginningUpperCase(prop.asInstanceOf[String])

      val javaGetMethod = _findFirstMethod(base.getClass(), javaGetterName)
      if (javaGetMethod != null) {
        //java getter method we let our standard el resolver handle the prop
        null
      }

      val method = _findFirstMethod(base.getClass(), prop.asInstanceOf[String])
      if (method != null) {
        elContext.setPropertyResolved(true)
        method.getReturnType
      } else {
        //lets delegate the analysis into the subsequent sections of the chain
        null
      }
    }
  }

  def getValue(elContext: ELContext, base: AnyRef, prop: AnyRef): AnyRef = {
    if (!(base != null && base.isInstanceOf[scala.ScalaObject])) {
      null
    } else {

      val javaGetterName = GET_PREFIX + toBeginningUpperCase(prop.asInstanceOf[String])

      val javaMethod = _findFirstMethod(base.getClass(), javaGetterName)
      if (javaMethod != null) {
        val res = javaMethod.invoke(base);
        //val res = executeMethod(base, javaGetterName)
        elContext.setPropertyResolved(true)

        return _handleCollectionConversions(res)
      }

      val method = _findFirstMethod(base.getClass(), prop.asInstanceOf[String])
      if (method != null) {
        // val res = executeMethod(base, prop.asInstanceOf[String])
        val res = method.invoke(base);
        elContext.setPropertyResolved(true)

        _handleCollectionConversions(res)
      } else {
        null
      }
    }
  }


  def isReadOnly(elContext: ELContext, base: AnyRef, prop: AnyRef): Boolean = {
    if (!(base != null && base.isInstanceOf[scala.ScalaObject])) {
      true
    } else {
      val methodName = prop.asInstanceOf[String]
      val setterName = methodName + SCALA_SET_POSTFIX
      if (_findFirstMethod(base.getClass, setterName, 1) != null) {
        elContext.setPropertyResolved(true)
        return false
      }
      true
    }
  }

  def toBeginningUpperCase(in: String): String = {
    val first = in.substring(0, 1);
    val last = in.substring(1);
    (first.toUpperCase + last)
  }



  def setValue(elContext: ELContext, base: AnyRef, prop: AnyRef, value: AnyRef) {
    if (base != null && base.isInstanceOf[scala.ScalaObject]) {
      val methodName: String = prop.asInstanceOf[String]
      val javaSetterName = SET_PREFIX + toBeginningUpperCase(methodName)
      var javaSetMethod = _findFirstMethod(base.getClass, javaSetterName, value.getClass)
      if (javaSetMethod != null) {
        javaSetMethod = _findFirstMethod(base.getClass, javaSetterName, _mapNativeType(value))
      }

      if (javaSetMethod != null) {
        //java setter method we let our standard el resolver handle the prop
        null
      }

      val setterName = methodName + SCALA_SET_POSTFIX
      var setterMethod = _findFirstMethod(base.getClass, setterName, value.getClass)
      if (setterMethod == null) {
        setterMethod = _findFirstMethod(base.getClass, setterName, _mapNativeType(value))
      }

      if (setterMethod != null) {
        setterMethod.invoke(base, value)
        elContext.setPropertyResolved(true)
      }
      null
    }
  }

  /**
   * We have to map our complex types into primitives for a second fast lookup
   * @param value
   * @return
   */
  def _mapNativeType(value: AnyRef) = {
    if (value.isInstanceOf[Int] || value.isInstanceOf[java.lang.Integer]) {
      Integer.TYPE
    }
    else if (value.isInstanceOf[Double] || value.isInstanceOf[java.lang.Double]) {
      java.lang.Double.TYPE
    }
    else if (value.isInstanceOf[Long] || value.isInstanceOf[java.lang.Long]) {
      java.lang.Long.TYPE
    }
    else if (value.isInstanceOf[Float] || value.isInstanceOf[java.lang.Float]) {
      java.lang.Float.TYPE
    }
    else if (value.isInstanceOf[Byte] || value.isInstanceOf[java.lang.Byte]) {
      java.lang.Byte.TYPE
    }
    else if (value.isInstanceOf[Short] || value.isInstanceOf[java.lang.Short]) {
      java.lang.Short.TYPE
    }
    else if (value.isInstanceOf[Boolean] || value.isInstanceOf[java.lang.Boolean]) {
      java.lang.Boolean.TYPE
    }
    else if (value.isInstanceOf[Char] || value.isInstanceOf[java.lang.Character]) {
      java.lang.Character.TYPE
    } else {
      value.getClass
    }
  }

  /**
   * handles the conversions for the return types
   *
   * @param res
   * @return
   */
  def _handleCollectionConversions(res: AnyRef): AnyRef = {
    //We now do also a map and iterable conversion so that
    //those can be accessed from within the el scope
    res match {
      case map: Map[_, _] => JavaConversions.mapAsJavaMap(map)
      case seq: Seq[_] => JavaConversions.seqAsJavaList(seq)
      case iter: Iterable[_] => JavaConversions.asJavaCollection(iter)
      case _ => res
    }
  }


  /**
   * speed optimized findFirstMethod
   */
  def _findFirstMethod(clazz: Class[_], methodName: String, varargs: Class[_]*): Method = {
    try {
      clazz.getDeclaredMethod(methodName, varargs: _*)
    } catch {
      case ex: NoSuchMethodException => {
        try {
          clazz.getMethod(methodName, varargs: _*)
        } catch {
          case e: NoSuchMethodException => null
        }
      }
    }
  }


  /**
   * speed optimized findFirstMethod
   */
  def _findFirstMethod(clazz: Class[_], methodName: String, varargLength: Int): Method = {
    var myClz = clazz;
    try {
      if (varargLength == 0) {
        return clazz.getMethod(methodName)
      }
    } catch {
      case ex: NoSuchElementException => null
    }

    while (myClz != null) {
      for (m <- myClz.getDeclaredMethods if (m.getParameterTypes.length == varargLength && m.getName.equals(methodName))) {
        return m
      }
      myClz = myClz.getSuperclass
    }
    null
  }
}
