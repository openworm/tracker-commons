minimal_exception

function check_lasterror(expected)
  if (!strcmp(lasterror.message, expected))
    # Take account of older versions prefixing with "error: " and adding a newline at the end
    if (!strcmp(regexprep(lasterror.message, 'error: (.*)\n$', '$1'), expected))
      error(["Bad exception order. Expected: \"", expected, "\" Got: \"", lasterror.message, "\""])
    endif
  endif
endfunction

a = A()

try
a.foo()
catch
  check_lasterror("C++ side threw an exception of type E1")
end_try_catch

try
  a.bar()
catch
  check_lasterror("C++ side threw an exception of type E2")
end_try_catch

elite = a.foobar()
a.bar()
hitchhiker = a.barfoo()
