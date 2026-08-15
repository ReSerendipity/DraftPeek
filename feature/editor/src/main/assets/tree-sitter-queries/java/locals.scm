; Local variable tracking for Java
; Source: https://github.com/nvim-treesitter/nvim-treesitter/blob/master/queries/java/locals.scm

; SCOPES
(program) @scope
(class_declaration
  body: (_) @scope.members )
(record_declaration
  body: (_) @scope.members )
(enum_declaration
  body: (_) @scope.members )
(lambda_expression) @scope
(enhanced_for_statement) @scope

; block
(block) @scope

; if/else
(if_statement) @scope
(if_statement
  consequence: (_) @scope)
(if_statement
  alternative: (_) @scope)

; try/catch
(try_statement) @scope
(catch_clause) @scope

; loops
(for_statement) @scope
(for_statement
  body: (_) @scope)
(do_statement
  body: (_) @scope)
(while_statement
  body: (_) @scope)

; Functions
(constructor_declaration) @scope
(method_declaration) @scope

; DEFINITIONS
(local_variable_declaration
  declarator: (variable_declarator
                name: (identifier) @definition.var))
(formal_parameter
  name: (identifier) @definition.var)
(catch_formal_parameter
  name: (identifier) @definition.var)
(inferred_parameters (identifier) @definition.var)
(lambda_expression
    parameters: (identifier) @definition.var)
(enhanced_for_statement
  name: (identifier) @definition.var)

(field_declaration
  declarator: (variable_declarator
                name: (identifier) @definition.field))

; REFERENCES
(identifier) @reference
