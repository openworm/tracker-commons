c_exception

function print_and_continue()
  disp("Caught Exception: "),disp(lasterror.message)
endfunction

try
  c_exception.foo()
catch
  print_and_continue()
end_try_catch

try
  c_exception.bar()
catch
  print_and_continue()
end_try_catch

c_exception.foo()
