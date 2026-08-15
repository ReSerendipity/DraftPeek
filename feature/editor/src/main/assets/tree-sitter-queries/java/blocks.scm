; Code block patterns for Java
; Capture names ending with '.marked' use the last terminal node's start position as scope end

(class_declaration
  body: (_) @scope.marked)

(record_declaration
  body: (_) @scope.marked)

(enum_declaration
  body: (_) @scope.marked)

(block) @scope.marked

(array_initializer) @scope.marked
