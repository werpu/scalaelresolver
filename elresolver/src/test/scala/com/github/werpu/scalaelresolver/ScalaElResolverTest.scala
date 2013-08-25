package com.github.werpu.scalaelresolver

import org.scalatest.{BeforeAndAfterEach, FlatSpec}
import com.github.werpu.scalaelresolver.probes.{Bean2, Bean1}
import javax.el.ELContext
import org.easymock.EasyMock._
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

@RunWith(classOf[JUnitRunner])
class ScalaElResolverTest extends FlatSpec with BeforeAndAfterEach  {

    var probe: Bean1 = null
    val elResolver = new ScalaELResolver()

    override def beforeEach() {
        probe = new Bean1()
        probe.bean2 = new Bean2()
    }

    override def afterEach() {
        probe = null
    }

    it should "have a correct structure" in {
        assert(probe.bean2 != null)
    }

    it should "have a simple getter producing a result" in {
        val ctx = createMock(classOf[ELContext])
        assert(elResolver.getValue(ctx,probe,"hello1") == "hello from bean 1")
        assert(elResolver.getValue(ctx,probe,"hello2") == 1)
        assert(elResolver.getValue(ctx,probe,"bean2") eq probe.bean2)
    }

    "words" should "be concatenated" in {
        assert("helloworld" === "helloworld")
    }

}
