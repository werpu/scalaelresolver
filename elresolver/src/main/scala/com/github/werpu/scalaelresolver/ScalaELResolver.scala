package com.github.werpu.scalaelresolver

import scala.Predef._
import scala.AnyRef

import javax.el.{ELContext, ELResolver}
import java.beans.FeatureDescriptor
import java.lang.reflect.Method

import collection.JavaConversions._
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
            val ret = new mutable.HashSet[FeatureDescriptor]
            //We have to iterate over all properties of the base and return it
            //as feature descriptor instance
            val methods: Array[Method] = base.getClass.getMethods
            val alreadyProcessed = new mutable.HashSet[String]
            for (method <- methods if !alreadyProcessed.contains(method.getName.replaceAll(EQ_REGEXP, ""))) {
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

            val javaGetMethod = _findMethod(base.getClass, javaGetterName)
            if (javaGetMethod != null) {
                //java getter method we let our standard el resolver handle the prop
                null
            }

            val method = _findMethod(base.getClass, prop.asInstanceOf[String])
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

            val javaMethod = _findMethod(base.getClass, javaGetterName)
            if (javaMethod != null) {
                val res = javaMethod.invoke(base)
                //val res = executeMethod(base, javaGetterName)
                elContext.setPropertyResolved(true)

                return _handleCollectionConversions(res)
            }

            val method = _findMethod(base.getClass, prop.asInstanceOf[String])
            if (method != null) {
                // val res = executeMethod(base, prop.asInstanceOf[String])
                val res = method.invoke(base)
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
            if (_findMethod(base.getClass, setterName, 1) != null) {
                elContext.setPropertyResolved(true)
                return false
            }
            true
        }
    }

    def toBeginningUpperCase(in: String): String = {
        val first = in.substring(0, 1)
        val last = if(in.length > 1) in.substring(1) else ""
        first.toUpperCase + last
    }


    def setValue(elContext: ELContext, base: AnyRef, prop: AnyRef, value: AnyRef) {
        def findMethod(setterName: String): Method = {
            var javaSetMethod = _findMethod(base.getClass, setterName, value.getClass)
            if (javaSetMethod == null) {
                javaSetMethod = _findMethod(base.getClass, setterName, _mapNativeType(value))
            }
            javaSetMethod
        }
        if (base != null && base.isInstanceOf[scala.ScalaObject]) {
            val methodName = prop.asInstanceOf[String]
            val javaSetterName = SET_PREFIX + toBeginningUpperCase(methodName)
            val javaSetMethod: Method = findMethod(javaSetterName)

            if (javaSetMethod != null) {
                //java setter method we let our standard el resolver handle the prop
                return
            }

            val setterName = methodName + SCALA_SET_POSTFIX
            val setterMethod = findMethod(setterName)

            if (setterMethod != null) {
                setterMethod.invoke(base, value)
                elContext.setPropertyResolved(true)
            }
        }
    }

    /**
     * We have to map our complex types into primitives for a second fast lookup
     * @param value a value to be investigated
     * @return the class or the native type
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
     * @param col the collection to be converted
     * @return  the converted collection
     */
    def _handleCollectionConversions(col: AnyRef): AnyRef = {
        //We now do also a map and iterable conversion so that
        //those can be accessed from within the el scope
        import JavaConversions._
        col match {
            case map: Map[_, _] => mapAsJavaMap(map)
            case seq: Seq[_] => seqAsJavaList(seq)
            case iter: Iterable[_] => asJavaCollection(iter)
            case _ => col
        }
    }


    /**
     * speed optimized findFirstMethod
     */
    def _findMethod(clazz: Class[_], methodName: String, varargs: Class[_]*): Method = {
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
    /**
     * speed optimized findFirstMethod
     */
    def _findMethod(clazz: Class[_], methodName: String, varargLength: Int): Method = {
        try {
            if (varargLength == 0) {
                return clazz.getMethod(methodName)
            }
        } catch {
            case ex: NoSuchElementException => return null
        }

        for (m <- clazz.getDeclaredMethods if m.getParameterTypes.length == varargLength && m.getName.equals(methodName)) {
            return m
        }
        for (m <- clazz.getMethods if m.getParameterTypes.length == varargLength && m.getName.equals(methodName)) {
            return m
        }

        null
    }
}
