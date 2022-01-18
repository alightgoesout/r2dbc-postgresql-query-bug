# r2dbc-postgresql query bug reproduction

This repository reproduces a bug I encountered in r2dbc-postgresql when using
custom types.

## Reproduction

### Steps

1. Create a postgresql database. If you have docker and docker-compose installed 
   you can just run `docker-compose up -d`. If you use your own database make
   sure to run the `init_db.sql` file to create the required types and table.
2. Make sure the connection configuration in `src/main/kotlin/Main.kt` is
   correct. If you used the provided docker compose file, there is nothing to
   change
3. Run the program using gradle: `./gradlew run`


### What is expected

The insertion query should succeed and `rowsUpdated: 1` should be printed in the
console

### What is happening

We get an exception:

```
Exception in thread "main" io.r2dbc.postgresql.ExceptionFactory$PostgresqlBadGrammarException: [22P02] malformed record literal: "GOOD"
	at io.r2dbc.postgresql.ExceptionFactory.createException(ExceptionFactory.java:96)
	…
```

## Diagnostic

I used Wireshark to analyze the frames sent to the database and obtained the
following result:

```
0000   00 04 00 01 00 06 02 42 e4 63 49 ab 00 00 08 00   .......B.cI.....
0010   45 00 01 3a a6 02 40 00 40 06 3b 94 ac 12 00 01   E..:..@.@.;.....
0020   ac 12 00 02 c8 06 15 38 70 34 af 61 65 2d 79 76   .......8p4.ae-yv
0030   80 18 01 f5 59 54 00 00 01 01 08 0a a9 30 4e e5   ....YT.......0N.
0040   e2 ad a7 00 50 00 00 00 6d 53 5f 30 00 49 4e 53   ....P...mS_0.INS
0050   45 52 54 20 49 4e 54 4f 20 69 74 65 6d 73 20 28   ERT INTO items (
0060   69 64 2c 20 6e 61 6d 65 2c 20 74 75 70 6c 65 2c   id, name, tuple,
0070   20 73 74 61 74 75 73 2c 20 72 61 6e 67 65 29 20    status, range) 
0080   76 61 6c 75 65 73 20 28 24 31 2c 20 24 32 2c 20   values ($1, $2, 
0090   24 33 2c 20 24 34 2c 20 24 35 29 00 00 05 00 00   $3, $4, $5).....
00a0   0b 86 00 00 04 13 00 00 40 03 00 00 40 05 00 00   ........@...@...
00b0   40 0a 42 00 00 00 71 42 5f 30 00 53 5f 30 00 00   @.B...qB_0.S_0..
00c0   05 00 00 00 00 00 00 00 00 00 00 00 05 00 00 00   ................
00d0   24 64 34 38 32 31 36 31 62 2d 62 32 63 37 2d 34   $d482161b-b2c7-4
00e0   35 31 64 2d 38 62 34 33 2d 65 36 62 34 35 63 66   51d-8b43-e6b45cf
00f0   37 62 64 66 38 00 00 00 09 54 65 73 74 20 49 74   7bdf8....Test It
0100   65 6d 00 00 00 04 47 4f 4f 44 00 00 00 05 5b 34   em....GOOD....[4
0110   2c 37 5d 00 00 00 0b 28 6f 6e 65 2c 32 2c 33 2e   ,7]....(one,2,3.
0120   33 29 00 00 44 00 00 00 09 50 42 5f 30 00 45 00   3)..D....PB_0.E.
0130   00 00 0c 42 5f 30 00 00 00 00 00 43 00 00 00 09   ...B_0.....C....
0140   50 42 5f 30 00 53 00 00 00 04                     PB_0.S....
```

First there is a 'P' message (_Parse_ command) with the correct query and
parameter types. The OIDs are respectively 2950 (`uuid`), 1043 (`varchar`),
16387 (`item_tuple`), 16389 (`item_status`), 16394 (`item_range`). The
OIDs were checked with the database.

Then there is a 'B' message (_Bind_ command). All parameters are sent in text
format. First there is the UUID ('`d482161b-b2c7-451d-8b43-e6b45cf7bdf8`' in
the example), then the name ('`Test Item`), then the status ('`GOOD`') and
finally the tuple ('`(one,2,3.3)`'). **The order is of the parameter is not
correct.**
