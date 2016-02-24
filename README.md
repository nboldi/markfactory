# MarkFactory
Code transformation plugin for roboust exam testing

Markfactory is an Eclipse plugin that transforms test code into an instrumented version that uses reflection to evaluate the user's code. It is able to test solutions that are not complete (but are compiling) and enable to evaluate them.

The user only needs to configure her Java project to enable automatic code generation. The generated code is refreshed as the test code is edited. The generated code is placed in two new projects.
 - Handout code is generated to be handed out to students to test their own work. Handout code does not relies on the testing architecture and can be executed without further instructions.
 - Autotest code is used to automatically evaluate the solutions.
