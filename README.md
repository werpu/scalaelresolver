scalaelresolver
===============

Custom Scala EL Resolver for JSF and usage example

Installation howto:

* Compile the project and after compilation add following to your maven project

      <dependency>
            <groupId>com.github.werpu.scalaelresolver</groupId>
            <artifactId>scalaelresolver</artifactId>
            <version>1.0-SNAPSHOT</version>
      </dependency>

And to your faces-config:

        <application>
             <el-resolver>
                 com.github.werpu.scalaelresolver.scalaelresolver.ScalaELResolver
             </el-resolver>
        </application>
