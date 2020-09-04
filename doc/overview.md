Crux is a database developped by **[Juxt](https://juxt.pro/)**. It has been available as a public alpha since april 2019.

At first glance it looks a bit like an open sourced Datomic, but without schemas and with a slightly different temporal model.

## Bitemporality

While Datomic is indexing datums along a single time axis based on transaction-time (the point in time where data was transacted into the database), Crux uses a bitemporal approach, indexing datums along two axis:

- transaction time
- valid time

This extra time axis (valid-time) let the user populate the DB with past and future information regardless of the order in which the information arrives, and make corrections to past recordings to build an ever-improving temporal model of a given domain.

This kind of modeling takes into account the fact that our understanding of the past grows along the way. We do not know the exact state of the domain at each moment.

As an exemple, we can think of a criminal investigation

[Crux - Open Time Store](https://opencrux.com/docs#bitemp-crime)

## Schemaless

Crux does not enforce any schema for the documents it stores. One reason for this is that data might come from many different places, and may not ultimately be owned by the service using Crux to query the data. This design enables schema-on-write and/or schema-on-read to be achieved outside of the core of Crux, to meet the exact application requirements.

The only requirement for a crux document is to have a `:crux.db/id` key

## Datalog queries

Like Datomic, Crux uses datalog as a query language. in both systems Datalog queries are represented as [EDN](https://opencrux.com/tutorials/essential-edn.html) datastructures, but are not totally compatible.

Datalog is a non turing-complete subset of prolog

Unlike in Prolog,

- statements of a Datalog program can be **stated in any order**.
- Datalog queries on finite sets **are guaranteed to terminate**
- Datalog disallows complex terms as arguments of predicates, e.g., p (1, 2) is admissible but not p (f (1), 2). It also is more restrictive about negation and recursion usage. [(wiki)](https://en.wikipedia.org/wiki/Datalog)

## Setup

In order to begin to play with Crux you only have to clone [this project](https://github.com/pbaille/crux-starter)

```shell
git clone git@github.com:pbaille/crux-starter.git
```

If you are not familiar with clojure you will find some instructions to setup an IDE in the readme file



