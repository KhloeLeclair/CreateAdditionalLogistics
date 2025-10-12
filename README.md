Create: Additional Logistics
============================

An addition for Create 6 for Minecraft 1.21.1.

This mod aims to improve or expand several areas related to logistics with the
Create mod.


## Additions

### Promise Limits (Factory Gauge Improvement)

We've all been there. You set up a new factory gauge, and request 100 stacks
of whatever, only to find out that your production facility can't keep up and
Create tried sending 1,000 packages worth of ingredients and now everything is
awful.

Promise Limits are here to help. When configuring a factory gauge, you can now
set a limit for how many promises it can have open at once. This allows you to,
for example, limit it to only waiting for at most a stack of something at a time.

Your factory's logistics grid will thank you.


### Regular Expressions (Package Address Improvement)

Globs are nice, but sometimes they just lack a little something, right? Well, go
ahead and use regex if you *dare*. No, really. You can now. Just start your address
pattern with `regex:` (case-insensitive) and the rest of it will be treated as a
regular expression.


### Cash Register (New Block, Stock Ticker-Like)

A new block that serves as an alternative to the Stock Ticker. Cash Registers
are meant for use in shops specifically. They feature UIs for extracting items
rather than right-clicking like the Stock Ticker. Additionally, they track all
sales made through them in a Sales Ledger. Plus, they look neat!

As a little bonus, you can take the Sale Ledger out of a Cash Register and
click a Stock Keeper with it to quickly select all the items you sold. Good
for restocking your shop, if you're doing that manually. (This works just like
a clipboard list of items for a Schematicannon.)


### Package Accelerator (New Block, Packagers Go Zoom)

An auxiliary block to accompany Packagers, this block uses stress units to
speed up the Packager in front of it. I use Sophisticated Storage for bulk
storage of items, and as the system has grown, having a single packager has
presented a bottleneck for sending items out. This is my solution, allowing
packages to be sent out much quicker but at a cost.

Package Accelerators require at least 30 RPM to function.


### Package Editor (New Block, Rename Packages)

A new package handling block. The Package Editor is similar to a Re-Packager,
but rather than combining packages from the same order, it allows you to edit
the addresses of packages using rules. Rules are input by either placing a
sign next to the Package Editor, or by attaching a clipboard to the side of
the Package Editor. Clipboards are recommended.

When writing rules for a Package Editor, the first line is the pattern to
match and the second line is the text to replace the match with. For example,
say you have packages being routed into a warehouse with the prefix `Ware1-`
and a full package name might be `Ware1-Floor2-Storage`. What if you want to
chop off the `Ware1-` once the package gets to your warehouse? That's easy,
just send the packages through a Package Editor with this pattern and a
blank replacement line:

```
Ware1-

```

The Package Editor will search each package name for `Ware1-` at the start,
and if it finds it, replace it with nothing. You could also write that like this:

```
Ware1-{*}
$1
```

So what's going on there? The Package Editor treats anything inside `{` and `}` as
a capture group. In other words, that text is saved and can be referenced in your
replacement text by adding a `$` followed one you want.  You can also use regular
expressions, of course, if you want to do something more advanced than globs
allow for.

Finally, there are two special behaviors you can manage if you're using a clipboard:

1. If you check the box next to the pattern line, the pattern will be interpreted
   as case-insensitive.
2. If you check the box next to the replacement line, then the Package Editor will
   stop after this rule (assuming this rule matched). You could use this, for
   example, to make a catch-all rule that sends packages to some Lost & Found if it
   doesn't match any other rules.


### Short and Tall Seats (New Block, Basically... Seats?)

Err, seats aren't logistics? Whatever do you mean? You can, erm... sit on them,
and—okay, fine. These are a bit out of scope for 'Additional Logistics', but
there's a good reason we've added seats! Aesthetics, of course.

Depending on what kind of mob you choose for your Stock Keeper, the default seat isn't
always at a good height for whatever build you're doing. As such, we've added
Short Seats and Tall Seats. Short Seats are 2/16ths of a block tall, while Tall Seats
are 14/16ths of a block tall. (Normal Seats are, of course, 8/16ths or 1/2 of a
block tall.)


### Lazy Shafts (New Block, Basically Shafts)

I know what you're thinking. This isn't logistics either. And Create has had shafts
for years. Get it together, Khloe. But this is important, I promise. Lazy Shafts
are my attempt at creating lag friendly shafts. Basically, normal shafts have a
block entity for each individual block, right? These try to avoid that when possible.
As long as you have more than two Lazy Shafts in a row, you're going to be coming
out ahead on block entities. If you use a lot of shafts, and I mean a lot, then
replacing them with these might help your server performance.


### Flexible Lazy Shafts (New Block, Basically Gearboxes + Lazy Shafts + ???)

There are Flexible Lazy Shafts too. These are like Lazy Shafts, but they can connect
in any direction. Not just that, but they have configurable sides. Just use your
wrench on a side to expose a shaft. Use your wrench again to reverse the direction.

Just be careful when connecting things. Due to how they're connected in code, sometimes
Create ends up breaking an inconvenient Flexible Lazy Shaft if there's a problem
with incompatible rotations.


## Improvements

### Address Matcher Optimization

Create's package address matching is just a little slow and unoptimized. This can be
fine, but it can also cause issues when using Create with other mods. We're
here to address the problem with a couple easy changes:

1. Cache the compiled matchers. Every package address is compiled into a regular
   expression when performing package matching. This involves first parsing the
   glob into regex, and then compiling the regex into a Java Pattern. All we have
   to do is cache that.

2. There are technically two caches because of how the internals of the package
   address system works, so I'm counting it as two things. But really, just caching
   was enough to make things work better in a doomsday scenario my SMP server saw
   recently thanks to Sophisticated Storage handling packages with package filters.


### Regular Expressions (Safety)

I'm actually aware there was already another mod that let you do regex with
package addresses, but when we tried that in our SMP the server was crashed by a
player almost immediately. Regular expressions can actually be fairly dangerous, so
I've included a few basic things that should hopefully stop someone from at least
*accidentally* killing a server with them. Namely:

1. **Star Height Limit.** Did you know the regular expression `(a+)+` is dangerous?
   It's because of something called catastrophic backtracking, and the Java regular
   expression library is vulnerable to it. They're not impossible to write by accident
   for that matter. We check the star height of all regular expressions and prevent
   them from running if they're problematic.
2. **Repetition Limit.** It's not quite as bad, but if you try to match, say, 1000 of
   something in a row, the performance can start to drag. That should never come up
   for package addresses in the first place, so we put a protection on.
3. **No Backreferences.** A backreference in regular expressions allows you to say
   "match this thing that was already matched", which can be useful, but it's also
   likely irrelevant for package addresses and it can be slow. They're disallowed
   by default.

We also make sure to test every regular expression at the moment it's parsed through
Create's `Glob.toRegexPattern` method, even though this doesn't normally compile the
expression, just convert it from a glob to regex. This is important because it's
implied that all regular expressions returned from this method should compile without
any errors.


### Longer Frogport Names

Regular expressions tend to be a bit... bigger than globs, which could be a problem
for frogs. As such, we've increased the maximum name length of frogports and
postboxes to 100. ... it's a good thing you can copy and paste.


### Let Your Stock Keepers Remain Seated

Let me set a scene for you. You're at a honey shop. The stock keeper is a bee. You
get a shopping list and you're ready to check out, so you click on the stock keeper
and-- oh no! You clicked the seat instead! You just sat down, and the bee took to
the skies with newfound freedom. You don't have a lead. How are you supposed to get
a bee back on a seat? Flying mobs are the worst.

This is a simple change. If you click on a seat occupied by a stock keeper, nothing
will happen. If you need to dislodge the stock keeper, you can break the seat or
the stock ticker. But at least you won't do it by accident.


# Changelog

## 1.2.3

### Added

* Lazy Cogs, which work together with Lazy Shafts.

### Changed

* Added a `lazy` tag for blocks/items that are part of the lazy network system.
* Viewing a Lazy Shaft or Flexible Shaft will now show an Overstressed
  warning correctly even if the block in question does not have a block entity.
* Further optimizations to how lazy networks interact with Create's networks.
* Adjust the model for Lazy and Flexible Shafts to hopefully fix z-fighting
  for some users.

### Fixed

* Cash Registers not working with Blaze Burners as stock keepers.
* Position of entities on Short and Tall Seats when those seats are part of
  a contraption.
* Issue where lazy networks wouldn't update when a block entity is removed due
  to all of a Flexible Shaft's faces being disabled.
* Rendering of the Cash Register in the Cash Register's menu.
* Issue where the server could crash after a tick if a lazy network block is 
  removed in a weird way.


## 1.2.2

### Added

* Configuration screen when interacting with a Flexible Lazy Shaft with an empty hand.

### Changed

* Draw the configured side of a Flexible Lazy Shaft differently if the rotation is
  reversed to allow for easy visual identification.

### Fixed

* Continued fix for issue where C:AL Lazy Shafts could result in a server deadlock when
  making changes to a kinetic network.
* Recipe for converting Lazy Shafts to regular Shafts having incorrect quantities.


## 1.2.1

### Added

* Encased Lazy Shafts

### Fixed

* Issue where C:AL Lazy Shafts could result in a server deadlock when making changes
  to a kinetic network.


## 1.2.0

### Added

* Lazy Shafts and Flexible Lazy Shafts


## 1.1.0

### Added

* ComputerCraft Support for Cash Registers, Package Editors, and Sales Ledgers.

### Fixed

* Cash Registers not dropping their Sales Ledger when broken.


### 1.0

* The initial release. I hope you all like it!
