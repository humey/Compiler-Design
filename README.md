# Compiler-Design
Lexical Scanner -> Parser -> Syntactic analysis and code generation


A compiler for a fake language called CD21 supplied by UON.

Generated tokens from the lexical analysis -> Parser creates an AST and populated the symbol table -> Checks for syntax errors / scoping / non-declared variables
any invalid syntax IAW the CD21 language specs -> Creates OPcodes for the given stack based machine.

Did not finish the code generation - Code can store variables / strings in both memory and stack, basic math functions +/-* and simple output.

Currently re-writing in rust
