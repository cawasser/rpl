#Experiments

A collection of namespaces that allow for [REPL](http://jr0cket.co.uk/2018/11/REPL-driven-development-with-Clojure.html) 
exploration of a variety of interesting topics


## Setup

This "project" is really just a collection of disjoint namespaces, each intended to explore a single
Clojure library or topic. As such, there is no notion of "running" the project. It is designed to
work _only_ from the REPL.

### Configuring a REPL using Cursive

Use _Edit Configurations..._ to create  a run configuration. The click the `+` button to
_Add New Configuration_. Select `Clojure REPL` and the `Local`.

In the right-hand panel, give the configuration a name, select `nREPL` and `Run with Deps`. Click
_Ok_ to save and close th Configuration panel.

Now you can start the REPL using the _run_ VCR control button and after a few seconds a shiny new REPL
will be started and ready for use.

### Evaluating Code

By default, Cursive will automatically `in-ns` the namespace as you switch files in the editor. Just
be sure to evaluate the namespace for itself to be sure the namespace is available in the REPL.



## Crux

[hello.clj](./src/crux/hello.clj) provides a playground for experimenting with [Crux](https://opencrux.com).

[space_port.clj](./src/crux/space_port.clj) follows the Crux tutorial found [here](https://juxt.pro/blog/crux-tutorial-setup)


## Resource Allocation

One of our early tasks was to develop a demonstration of "Open Channel Planning." Setting aside for
a moment, exactly what that phrase means, we _can_ say that it is fundamentally a 
"resource allocation" problem. We have a constrained resource (channel) and we have
"requestors" who would like to use it. The problem then is: 

> How do we allocate this resource to make all the requestors happy?


(see [allocation-try-2.clj](./src/resource_alloc/allocation_try_2.clj))

Let's start with the information model of the resource itself. To keep things as 
simple as possible, both to get going and to have a solid foundation for enhancements,
we will model "channels" as s Clojure vector of abstract fixed-unit "slots":

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


## Excel Programming

[excel_play.clj](./src/excel_play.clj) provides a playground to work with [docjure](https://github.com/mjul/docjure)



## Oz

[simple_oz.clj](./src/oz/simple_oz.clj) is a REPL-based playgorund for [Oz](https://github.com/metasoarous/oz)