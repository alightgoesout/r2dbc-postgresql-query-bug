CREATE TYPE item_tuple AS (one text, two integer, three double precision);

CREATE TYPE item_status AS ENUM ('GOOD', 'BAD');

CREATE TYPE item_range AS RANGE (SUBTYPE = integer);

CREATE TABLE items (
  id uuid primary key,
  name text not null,
  tuple item_tuple not null,
  status item_status not null,
  range item_range not null
);
