=== Installing SCALA

* $brew install scala -> deberia ser la version 0.9.2
* setup documentation
    $ cd /usr/local/Cellar/scala/2.9.2/libexec/
    $ mkdir -p scala-devel-docs/api
    $ wget http://www.scala-lang.org/downloads/distrib/files/scala-docs-2.9.2.zip
    $ unzip scala-docs-2.9.2.zip
    $ mv scala-docs-2.9.2/* ./
    $ rm -rf scala-docs-2.9.2
* setup scala en el proyecto intellij -> scala module -> set scala home -> /usr/local/Cellar/scala/2.9.2/libexec


=== Installing sbt

* $brew install sbt

=== Running the project using IntelliJ

* IntelliJ deberia utilizar Maven para resolver las dependencias.
* El proyecto Maven está descrito en el fichero pom.xml
* botón derecho en la clase ejecutable y después 'run NameOfTheFile'

=== Running the project using SBT

* SBT mantiene su configuración en el fichero build.sbt
* para ejecutar el proyecto se puede utilizar $sbt run
e.g:
 antonio@Antonios-MacBook-Pro-4:~/Development/Business/AgentBAD [master] $ sbt run
 [info] Set current project to AgentBAD (in build file:/Users/antonio/Development/Business/AgentBAD/)
 [info] Updating {file:/Users/antonio/Development/Business/AgentBAD/}default-28b7f5...
 [info] Resolving com.typesafe#config;0.3.1 ...
 [info] Done updating.
 [info] Running mmaker.TestMain
 ** RECEIVED hey
 ** RECEIVED ping
 ** TERMINATING
 [success] Total time: 5 s, completed Dec 31, 2012 11:06:09 PM

=== Test Driven Development

Los tests del proyecto estan escritos inicialmente usando Scalatest/FunSuite (http://www.scalatest.org/getting_started_with_fun_suite)


=== Running the tests using IntelliJ

Right button and selecting 'Run...' deberia ser suficiente para ejecutar la test suite en la interfaz de testing de IntelliJ

=== Running the tests using sbt

* ejecutar $sbt test
e.g:
 antonio@Antonios-MacBook-Pro-4:~/Development/Business/AgentBAD [master] $ sbt
 [info] Set current project to AgentBAD (in build file:/Users/antonio/Development/Business/AgentBAD/)
 > test
 [info] Updating {file:/Users/antonio/Development/Business/AgentBAD/}default-28b7f5...
 [info] Resolving org.scalatest#scalatest_2.9.2;1.8 ...
 [info] Done updating.
 [info] Compiling 9 Scala sources to /Users/antonio/Development/Business/AgentBAD/target/scala-2.9.2/classes...
 [warn] there were 6 deprecation warnings; re-run with -deprecation for details
 [warn] one warning found
 [info] Compiling 10 Scala sources to /Users/antonio/Development/Business/AgentBAD/target/scala-2.9.2/classes...
 [warn] there were 6 deprecation warnings; re-run with -deprecation for details
 [warn] one warning found
 [info] Compiling 1 Scala source to /Users/antonio/Development/Business/AgentBAD/target/scala-2.9.2/test-classes...
 [info] OrderBookSuite:
 [info] - OrderBooks should order bids in descending order
 [info] Passed: : Total 1, Failed 0, Errors 0, Passed 1, Skipped 0
 [success] Total time: 36 s, completed Jan 1, 2013 5:15:07 PM
