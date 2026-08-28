package com.draftpeek.feature.browser.util

object FileTemplateProvider {

    enum class TemplateType {
        EMPTY,
        HELLO_WORLD,
        CLASS
    }

    fun getEmptyTemplate(language: String, extension: String): String = ""

    fun getHelloWorldTemplate(language: String, extension: String): String = when (language) {
        "Java" ->
            """public class Main {
    public static void main(String[] args) {
        System.out.println("Hello, World!");
    }
}"""
        "Kotlin" ->
            """fun main() {
    println("Hello, World!")
}"""
        "Python" -> """print("Hello, World!")"""
        "JavaScript" -> """console.log("Hello, World!");"""
        "TypeScript" -> """console.log("Hello, World!");"""
        "C" ->
            """#include <stdio.h>

int main() {
    printf("Hello, World!\n");
    return 0;
}"""
        "C++" ->
            """#include <iostream>

int main() {
    std::cout << "Hello, World!" << std::endl;
    return 0;
}"""
        "C#" ->
            """using System;

class Program {
    static void Main() {
        Console.WriteLine("Hello, World!");
    }
}"""
        "Go" ->
            """package main

import "fmt"

func main() {
    fmt.Println("Hello, World!")
}"""
        "Rust" ->
            """fn main() {
    println!("Hello, World!");
}"""
        "PHP" ->
            """<?php
echo "Hello, World!";
?>"""
        "Ruby" -> "puts \"Hello, World!\""
        "Swift" -> """print("Hello, World!")"""
        "Shell" -> "#!/bin/bash\necho \"Hello, World!\""
        "SQL" -> """SELECT 'Hello, World!' AS message;"""
        "HTML" ->
            """<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Hello World</title>
</head>
<body>
    <h1>Hello, World!</h1>
</body>
</html>"""
        "CSS" ->
            """/* Hello World in CSS - add to your HTML */
body::after {
    content: "Hello, World!";
}"""
        "Markdown" ->
            """# Hello World

This is a **Hello World** example in Markdown.

- Item 1
- Item 2
- Item 3
"""
        "JSON" ->
            """{
  "message": "Hello, World!"
}"""
        "XML" ->
            """<?xml version="1.0" encoding="UTF-8"?>
<message>Hello, World!</message>"""
        "YAML" -> """message: Hello, World!"""
        "Dart" ->
            """void main() {
  print('Hello, World!');
}"""
        "Lua" -> """print("Hello, World!")"""
        "Scala" ->
            """object Main extends App {
  println("Hello, World!")
}"""
        "Groovy" -> """println 'Hello, World!'"""
        "R" -> """cat("Hello, World!\n")"""
        "Julia" -> """println("Hello, World!")"""
        "Perl" ->
            """#!/usr/bin/perl
use strict;
use warnings;

print "Hello, World!\n";"""
        "Haskell" ->
            """module Main where

main :: IO ()
main = putStrLn "Hello, World!\""""
        "Elixir" -> """IO.puts("Hello, World!")"""
        "Erlang" ->
            """-module(main).
-export([main/0]).

main() ->
    io:format("Hello, World!~n")."""
        "OCaml" -> """let () = print_endline "Hello, World!\""""
        "Clojure" -> """(println "Hello, World!")"""
        "Lisp" -> """(format t "Hello, World!~%")"""
        "Vim Script" -> """echo "Hello, World!\""""
        "TOML" ->
            """# Configuration file
title = "Hello, World!\""""
        "Dockerfile" ->
            """FROM alpine:latest
CMD ["echo", "Hello, World!"]"""
        "INI" ->
            """; Configuration file
[general]
name = Hello World"""
        "LaTeX" ->
            """\documentclass{article}
\begin{document}
Hello, World!
\end{document}"""
        else -> ""
    }

    fun getClassTemplate(language: String, extension: String): String = when (language) {
        "Java" ->
            """public class Main {

}"""
        "Kotlin" ->
            """class Main {

}"""
        "Python" ->
            """class Main:
    def __init__(self):
        pass"""
        "JavaScript" ->
            """class Main {
    constructor() {

    }
}"""
        "TypeScript" ->
            """class Main {
    constructor() {

    }
}"""
        "C" ->
            """/* C does not have built-in classes */
/* Use structs and functions instead */

#include <stdio.h>

typedef struct {
    int id;
} Main;

int main() {
    return 0;
}"""
        "C++" ->
            """#include <iostream>

class Main {
public:
    Main() {

    }
};

int main() {
    return 0;
}"""
        "C#" ->
            """using System;

class Main {
    public Main() {

    }

    static void Main() {

    }
}"""
        "Go" ->
            """package main

type Main struct {
    id int
}

func NewMain() *Main {
    return &Main{}
}

func main() {

}"""
        "Rust" ->
            """struct Main {
    id: i32,
}

impl Main {
    fn new() -> Self {
        Main { id: 0 }
    }
}

fn main() {

}"""
        "PHP" ->
            """<?php

class Main {
    public function __construct() {

    }
}
?>"""
        "Swift" ->
            """class Main {
    init() {

    }
}"""
        "Dart" ->
            """class Main {
    Main() {

    }
}"""
        "HTML" ->
            """<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Document</title>
</head>
<body>

</body>
</html>"""
        "CSS" ->
            """/* Reset and base styles */
* {
    margin: 0;
    padding: 0;
    box-sizing: border-box;
}

body {
    font-family: sans-serif;
}"""
        "Lua" ->
            """Main = {}
Main.__index = Main

function Main.new()
    local self = setmetatable({}, Main)
    return self
end

return Main"""
        "Scala" ->
            """class Main {

}

object Main {
    def main(args: Array[String]): Unit = {
        val app = new Main()
    }
}"""
        "Groovy" ->
            """class Main {
    static void main(String[] args) {

    }
}"""
        "R" ->
            """Main <- setRefClass("Main",
    fields = list(),
    methods = list(
        initialize = function() {
        }
    )
)"""
        "Julia" ->
            """struct Main
end

function main()
    app = Main()
end

main()"""
        "Perl" ->
            """package Main;

sub new {
    my (${'$'}class) = @_;
    my ${'$'}self = {};
    bless ${'$'}self, ${'$'}class;
    return ${'$'}self;
}

1;"""
        "Haskell" ->
            """module Main where

data Main = Main

main :: IO ()
main = do
    let app = Main
    return ()"""
        "Elixir" ->
            """defmodule Main do
  def new do
    %{}
  end
end"""
        "Erlang" ->
            """-module(main).
-export([new/0]).

-record(main, {}).

new() ->
    #main{}."""
        "OCaml" ->
            """class main =
  object (self)
    initializer
      ()
  end

let () =
  let _app = new main in
  ()"""
        "Clojure" ->
            """(defrecord Main [])

(defn -main []
  (let [app (->Main)]
    ))"""
        "Lisp" ->
            """(defclass main ()
  ())

(defun make-main ()
  (make-instance 'main))"""
        else -> ""
    }
}
