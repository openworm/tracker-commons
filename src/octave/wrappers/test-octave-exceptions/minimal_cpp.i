%module minimal_exception

%include "exception.i"

%inline %{
  struct E1
  {
  };

  struct E2 
  {
  };

  struct E3 
  {
  };

  struct A 
  {
    /* caught by the user's throw definition */
    int foo() throw(E1) 
    {
      throw E1();
      return 0;     
    }
    
    int bar() throw(E2)
    {
      throw E2();
      return 0;     
    }

    int foobar()
    {
      return 1337;
    }
    
    int barfoo()
    {
      return 42;
    }
  };
  %}

