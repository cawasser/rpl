#Experiments

A collection of namespaces that allow for [REPL](http://jr0cket.co.uk/2018/11/REPL-driven-development-with-Clojure.html) 
exploration of a variety of interesting topics



## Crux

[hello.clj](./src/crux/hello.clj) provides a playground for experimenting with [Crux]().




## Resource Allocation

One of our first tasks is to develop a demonstration of "Open Frequency Planning." Setting aside for 
a moment, exactly what that phrase means, we _can_ say that it is fundamentally a 
"resource allocation" problem. We have a constrained resource (frequencies) and we have
"requestors" who would like to use it. The problem then is: 

>How do we allocate this resource to make all the requestors happy? 


(see [allocation-try-2.clj](./src/allocation_try_2.clj))

Let's start with the information model of the resource itself. To keep things as 
simple as possible, both to get going and to have a solid foundation for enhancements,
we will model "frequency" as s Clojure vector of abstract fixed-unit "slots": 

```[slot-1 slot 2 slot3]```

If we view the slots as the X-axis then we can model _Time_ as the Y-axis using 
another vector of the first:

```
  [[slot-1 slot-2 slot-3]
   [slot-4 slot-5 slot-6]
   [slot-7 slot-8 slot-9]]
```

Pretty simple. To handle the allocation, we will put a marker into each slot to 
show which request is using it:

```
  [[:a   :b    _]
   [ _    _    _]
   [ _    _    _]]
```

We'll actually make each slot hold a Clojure `set` of the request IDs:

```
  [[#{:a} #{:b}  #{  }]
   [#{  } #{  }  #{  }]
   [#{  } #{  }  #{  }]]
```

Using sets makes this much easier, since we can take advantage of the 
[(disj...)](https://clojuredocs.org/clojure.core/disj) function
to remove unwanted IDs if we need to (and we need to).


## Constraints Programming

Now that we have a basic information model for our resources, we can start
to expand to bring it closer to how we might want to use it in the real world.

For example, currently, the allocation model expects each request to specify _exactly_
which resources it wants to use, both the `channel` and the `time-slot`. This isn't very
flexible. What if the requestor didn't care which channel it got, just that it
got _some_ channel at the needed times?

This is where [Constraint Programming](https://en.m.wikipedia.org/wiki/Constraint_programming) 
comes in. What if we could express the requests as a range of options each rquestor could
accept and let the computer figure out how to satisfy them all?

For this experiment, we will use the [loco](https://github.com/aengelberg/loco) library, by 
Alex Engleberg. _loco_ is a declarative, functional wrapper around the 
[Choco](http://www.choco-solver.org) Java library _(See the advantages of being hosted?)_




