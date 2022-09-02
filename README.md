# Semantic-Analysis-And-IR-Generation
Implements Static Checking for a subset of Java (MiniJava) and production of Intermediate Representation code fed to LLVM compiler 📖

## Semantic Analysis:

Compilation consists of a variety of stages, first one being [Semantic Analysis](https://www.google.com/search?q=semantic+analysis+of+a+program&sxsrf=ALiCzsbc-dQ0KhKj6T80KSIrDEZw4iLVGQ%3A1662141648290&ei=0EQSY-yQEaeB9u8P7p2B4AE&ved=0ahUKEwjsuIyU2Pb5AhWngP0HHe5OABwQ4dUDCA4&uact=5&oq=semantic+analysis+of+a+program&gs_lcp=Cgdnd3Mtd2l6EAMyBggAEB4QFjIGCAAQHhAWMgYIABAeEBYyBQgAEIYDMgUIABCGAzIFCAAQhgMyBQgAEIYDMgUIABCGAzoKCAAQRxDWBBCwAzoHCAAQsAMQQzoFCAAQgAQ6BAgAEEM6CggAEIAEEIcCEBQ6CAgAEB4QDxAWSgQIQRgASgQIRhgAULoBWOUTYL4VaAFwAXgAgAGYAYgB8g2SAQQwLjEzmAEAoAEByAEKwAEB&sclient=gws-wiz).
It is task is to ensure that the declarations and the statements of a target program are semantically correct. Their meaning has to be clear and consistent with the way in
which control structures and data types are supposed to be used.

These requirements can be met by applying a throughout [Static Checking](https://web.mit.edu/6.005/www/fa16/classes/01-static-checking/#:~:text=Static%20checking%20tends%20to%20be,exactly%20which%20value%20it%20has.),
which focuses on types and errors that are independent of the specific value that a variable has. We can regard type as a set of values.
*Static Checking* guarantees that a variable will have some value from that set, but we don't know until runtime exactly which value it has.

In order to be able to traverse the contents of the target program, a map is needed, namely MiniJava's grammmar in [JavaCC Form](https://cgi.di.uoa.gr/~compilers/project_files/minijava-new-2022/minijava.jj).</br>
[JTB Tool](https://www.jtbworld.com/) is used to convert it into a grammar that produces class hierarchies, contained within a
[Abstact Syntax Tree](https://en.wikipedia.org/wiki/Abstract_syntax_tree). Following the [Visitor Pattern](https://en.wikipedia.org/wiki/Visitor_pattern),
the *First Visitor* (`MGVisitor`) program is developed that recursively traverses the different noed of the tree, based on the type of entity that is currently read from the target file.
At each step, semantic correctness is examined.

Traversal is used to gather useful data for every class such as the **names** and the **offsets** of every field and method this class contains. In this way,
the *Second Visitor* (`TCVisitor`) has all the necessary *type level* information to infer whether subsequent commands and expressions are valid based on the data type of
the operators that partake in them. This process is known as the population of classes' [Virtual Method Table](https://en.wikipedia.org/wiki/Virtual_method_table) and is crucial for the inheritance aspect
of modern languages.

## Intermediate Representation:

After we have guaranteed the *Semantic Correctness* of the target program, we have to translate it into its [Intermediate Representation](https://en.wikipedia.org/wiki/Intermediate_representation).
This is a low level language, that contains simple memory based commands, the concept of a class is no more relevant. The produced code is subsequently
fed to a [LLVM Compiler](https://llvm.org/). Here, the low level representation is translated into **binary code**, that can run on the computer's
hardware. This phase is achieved by the *Third Visitor* (`LLVMVisitor`), that traverses each expression and command in the target file and for each one,
produces its corresponding *IR*.

## Compilation & Execution:

* In order to compile the code, enter the `/Compiler` folder and type: `make compile`. 
* To make the target Java files visible to the compiler, you have to put them into the `/target_java`.
* Execute the program by staying in the `/Compiler` folder and typing: `java Main <target java file 1> ... <target java file N>`

**IMPORTANT** - Program takes the raw names of the files as they are in the `target_java` folder. You *DON'T* have to include the absolute path!

After the compilation commences, you can find the *Intermidiate Representation* of each requested target file in the `output_ll` folder.
If you want to produce an *executable* of an *IR File*, you have to download [Clang](https://clang.llvm.org/) and run the following command:</br>
`clang -o <executable name> <ll file>`

### Further Information:

*Built as part of the course: Compilers , Summer of 2021. University of Athens, DiT.*






