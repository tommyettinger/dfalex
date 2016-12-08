# dfalex

Scanning / Lexical Analysis Without All The Fuss
================================================

Sometimes you need faster and more robust matching than you can get out of Java regular expressions.  Maybe they're too
slow for you, or you get stack overflows when you match things that are too long, or maybe you want to search for many
patterns simultaneously.  There plenty of lexical analysis tools you can use, but they invovle a lot of fuss.  They make
you write specifications in a domain-specific language, often mixed with code, and then generate new java code for a
scanner that you have to incorporate into your build and use in pretty specific ways.

DFALex provides that powerful matching capability without all the fuss.  It will build you a deterministic finite
automaton (DFA, google-able) for a matching/finding multiple patterns in strings simultaneously, which you can then use
with various matcher classes to perform searching or scanning operations.

Unlike other tools which use DFAs internally, but only build scanners with them, DFALex provides you with the actual DFA
in an easy-to-use form.  Yes, you can use it in standard scanners, but you can also use it in other ways that don't fit
that mold.

This project is a fork of [Matt Timmermans' dfalex library](https://github.com/mtimmerm/dfalex), bringing the required
Java version down to Java 7 so it should be able to work on Android and, with a little more work needed, GWT. This is
also a Maven-ized fork, so it should be possible to get it on Maven Central sometime soon.

Start Here:
-----------

* **DfaBuilder** for building DFAs

* **Pattern** and **CharRange** for specifying patterns to match

* **StringMatcher** for using your DFAs to find patterns in strings

Requirements
------------

DFALex needs Java 7 or better, including Android and possibly GWT (it is a goal).  No special libraries are required.

You can install this using Maven locally with `mvn install` or (once the tests pass again) as a dependency from Maven
Central, though this isn't ready yet.

If you want to run the tests, you'll need JUnit4. They don't currently all pass, and some will use a lot of heap space.

About
-----

DFALex is written by Matt Timmermans, and is all new code.  It's written in Java first, with too much attention paid to
performance. Some tweaks have been added on by Tommy Ettinger, expanding compatibility and Maven-izing the project.

DFAs are generated from NFAs with a starndard powerset construction, and minimized used a fast hash-based variant of
Hopcroft's algorithm.

This project was started because lexical analysis is no big deal.  You should be able to just do it, without having to
convince your team to add a new build step to generate code from a domain specific language.  This way you can use it
for lots of little jobs, instead of just big, important ones.
