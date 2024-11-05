# gass (GNU Assembler Script)
High-level script language for GAS

>This repository continues in [gass](https://github.com/miruji/gass) implemented in Rust;

## A few important notes

1. Previously, I made a comment that GCC uses GAS and therefore this project does not make sense. However, in practice everything turned out to be completely different. There is not enough information yet due to too little functionality of GASS, but the usual output on Linux x64 when compressed with upx -q -q -q looks like this (5 runs of perf):
   ```
    Compiler | Task-Clock | Instructions | File Size |  GHz
    GAS        0.21 msec      47,511        3.7KiB     0.279
    GCC C      0.54 msec      216,734       5.4KiB     0.428
    Clang C    0.54 msec      216,444       5.7KiB     0.516
    GCC C++    1.12 msec     2,685,009      6.1KiB     0.646
    Rust       2.62 msec     7,684,702     182.3KiB    2.826
    D          3.31 msec     10,743,599    296.5KiB    2.955
    Go         6.96 msec     21,421,797     1.2MiB     3.415
   ```
   I associate this with:
     1. The functions used in GASS were currently written separately in GAS and do not have unnecessary dependencies.
     2. The GASS compiler calculates everything to the maximum limits, which leaves no living parts in the program code.
   
   The actual code is at your disposal, and everything below explains the basic idea and distribution rule.
2. The GASS compiler must calculate the entire program to the limit in advance and produce only the result. In cases where this is not possible, he leaves live sections of the code with their own optimizations.
3. GASS should rely first of all on GAS, secondly on GASS code, and only lastly on insertions from other languages. All primary types created for GASS are written in GAS and are fully compatible but extended.

## Introduction

*So, the concept of gas does not imply the creation or use of machines for processing byte and bit codes or the use of ready-made solutions. It's too dependent, expensive, and besides, I don't expect any performance gains from using this development approach.*

*Now perhaps about other languages. Family C-Similar languages and C itself do not look like an ideal solution, so I would say that GASS is a branch of this branch of languages at the expense of GAS (GNU Assembler). The output is supposed to be pure assembler code and then machine code for the required architectures. In my opinion, this is the most productive way to work.*

*GASS only works in compile mode, has its own type system and high-level syntax and is fully compatible with GAS, since it is based on it.*

Java is used to develop the compiler because it is cross-platform, simple, and better suited for the task. Where possible, comments will be left on the code and the same syntax will be used.
The working process is as follows:
  1. ~~Getting code from a .gs file~~
  2. ~~Tokenization and code parsing~~
  3. ~~Getting AST~~ and optimizing the code
  4. ~~Obtaining gas code from AST~~
  5. ~~Building a program~~ or running part of a program in real time.
     > GAS in this case is an intermediate representation between gass and machine code
  7. * Writing code in gas/C/gass and implementing it in point 4 to get more gass functionality
     > Thus, the initial core of libraries and functionality will be written in GAS, with the possibility of extending the gass language itself. I don't think it's impossible to write anything in GAS, so the plan is to simply add more functionality over time.
  8. * Writing a JIT to run GAS code at runtime -> second alternative way to work gass

Therefore, assistance to the project is welcome; when modifying, take into account the GPL3 license under which both GAS itself and this gass project are distributed. Compiled programs and code created with and for gass are licensed at your discretion.

## Syntax template
Typing: 
> static, loose, implicit

Block structure:
> The structure is built on code blocks. Blocks of code can be functions or procedures. There is no need to specify anything explicitly, just specify the block name. If the block name is not specified, then it is a temporary block
```
main:
  # BLOCK
;
```
> Exception: the main program function will automatically turn return 0 if return was not specified
```
println(10)
= 0

# or
println(10)
```
> Parameters can be omitted for global methods
```
ab(a: I, b: I):
  # block
;

# or
main:
  # block
;

# in local methods -> no parameters
```
> Local or otherwise temporary func or proc are used as areas for stubs, or code for variables, etc. if no need to go beyond a certain section of code
```
test:
  a = :
    return 10
  ;
  # or
  a = 10

  b = :
    println(10)
    ## no return and b wait =
       then return exception ##
  ;
;
```
> Local variables will be searched first in the local block, then higher up to the global function itself. The very last instance of search is the parameters of the global function. All new variables will remain only inside the block, and changes will be applied to the found instances
```
test:
  a = 10
  :
    b = 15
    a += b # a = 25
  ;
  # in this place no b variable
  println(a) # a = 25
;
```
Block declaration rule:
```
global in global -> nope
local  in global -> yes
local  in local  -> yes
```
Other types of blocks:
> loops, classes, enums ...

if & switch
> As before, we use the usual temporary sections of code : end
```
a = 10
if a == 10:
  # block
;

a = 10
switch a:
  case 9:
    # block
  ;
  default:
    # go here
  ;
;
```
cycles
```
loop i = 0, i < 10, i++:
  # block
;

loop true:
  # block
;
```
classes
> Thus classes are designed for OOP and wrapping global functions and variables into objects. Access is regulated by private and public flags both when declaring a class and internally for specific elements
```
myFunction:
  # block
;

MyClass: 
  # block
  test = "test text"
;
```
enum
```
list:
  # block
  "apples" = 10
  "pears" = 20
;
```
variables
> Variables have a name and assigned value on the right
```
a = "Hello!"
a = :
  # block
  10
;
```
gas code
```
asm:
  # asm block
end
```
