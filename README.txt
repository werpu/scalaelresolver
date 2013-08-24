scalaelresolver
===============

Custom Scala EL Resolver for JSF and usage example
As for now there is no hosting in the Maven central repository given its alpha state
you have to compile yourself for your project.


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
             com.github.werpu.scalaelresolver.ScalaELResolver
         </el-resolver>
     </application>

Example Application:

There is a testcase/example included, it can be started from the elresolvertest dir
by starting mvn tomcat7:run-war and then pointing your browser to http://localhost:9080