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
import com.github.werpu.scalaelresolver.util.ReflectUtil
import scala.collection.{mutable, JavaConversions}

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
  val EQ_REGEXP = "\\_\\$eq"
  val SCALA_SET_POSTFIX = "_$eq"
}

class ScalaELResolver extends ELResolver {

  import CONST._;

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
      val methods: Array[Method] = base.getClass().getMethods
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

      val javaGetMethod = ReflectUtil.findFirstMethod(base.getClass(), javaGetterName, 0)
      if (javaGetMethod != null) {
        //java getter method we let our standard el resolver handle the prop
        null
      }

      val method = ReflectUtil.findFirstMethod(base.getClass(), prop.asInstanceOf[String], 0)
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

      val javaMethod = ReflectUtil.findFirstMethod(base.getClass(), javaGetterName, 0)
      if (javaMethod != null) {
        val res = javaMethod.invoke(base);
        //val res = ReflectUtil.executeMethod(base, javaGetterName)
        elContext.setPropertyResolved(true)

        return handleConversions(res)
      }

      val method = ReflectUtil.findFirstMethod(base.getClass(), prop.asInstanceOf[String], 0)
      if (method != null) {
        // val res = ReflectUtil.executeMethod(base, prop.asInstanceOf[String])
        val res = method.invoke(base);
        elContext.setPropertyResolved(true)

        handleConversions(res)
      } else {
        null
      }
    }
  }

  /**
   * handles the conversions for the return types
   *
   * @param res
   * @return
   */
  def handleConversions(res: AnyRef): AnyRef = {
    //We now do also a map and iterable conversion so that
    //those can be accessed from within the el scope
    res match {
       case map: Map[_,_] => JavaConversions.mapAsJavaMap(map)
       case seq: Seq[_] => JavaConversions.seqAsJavaList(seq)
       case iter: Iterable[_] => JavaConversions.asJavaCollection(iter)
       case _ => res
    }
  }

  def isReadOnly(elContext: ELContext, base: AnyRef, prop: AnyRef): Boolean = {
    if (!(base != null && base.isInstanceOf[scala.ScalaObject])) {
      true
    } else {
      val methodName: String = prop.asInstanceOf[String]
      val setterName = methodName + SCALA_SET_POSTFIX
      if (base.getClass.getMethod(setterName) != null) {
        elContext.setPropertyResolved(true)
        false
      } else {
        val m: Method = base.getClass.getMethod(methodName)
        val paramTypes = m.getParameterTypes
        val ret = !(paramTypes != null && paramTypes.length > 0)
        elContext.setPropertyResolved(true)
        ret
      }
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
      val javaSetMethod = ReflectUtil.findFirstMethod(base.getClass(), javaSetterName, 1)

      if (javaSetMethod != null) {
        //java setter method we let our standard el resolver handle the prop
        null
      }

      val setterName = methodName + SCALA_SET_POSTFIX
      val setterMethod = ReflectUtil.findFirstMethod(base.getClass(), setterName, 1)

      if (setterMethod != null ) {
        val transformedValue = getValueType(setterMethod, value)
        // ReflectUtil.executeMethod(base, setterName, transformedValue)
        setterMethod.invoke(base, transformedValue)
        elContext.setPropertyResolved(true)
      }
       null
    }
  }

  def getValueType(method: Method, theVal: AnyRef): AnyRef = {
    if (!theVal.isInstanceOf[String]) {
      theVal
    } else {
      val strVal = theVal.asInstanceOf[String]
      val paramTypes: Array[Class[_]] = method.getParameterTypes()
      val initParam = paramTypes.apply(0)
      if (initParam == classOf[String]) {
        theVal
      } else if (initParam == classOf[Int]) {
        strVal.toInt.asInstanceOf[AnyRef]
      } else if (initParam == classOf[Long]) {
        strVal.toLong.asInstanceOf[AnyRef]
      } else if (initParam == classOf[Float]) {
        strVal.toFloat.asInstanceOf[AnyRef]
      } else if (initParam == classOf[Double]) {
        strVal.toDouble.asInstanceOf[AnyRef]
      } else if (initParam == classOf[Boolean]) {
        strVal.toBoolean.asInstanceOf[AnyRef]
      } else if (initParam == classOf[Byte]) {
        strVal.toByte.asInstanceOf[AnyRef]
      } else if (initParam == classOf[Char]) {
        strVal.toCharArray.apply(0).asInstanceOf[AnyRef]
      } else {
        theVal
      }
    }
  }

}
