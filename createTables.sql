CREATE TABLE Users (
  username VARCHAR(20) NOT NULL PRIMARY KEY,
  password VARCHAR(20),
  balance INT
  );


CREATE TABLE Capacities (
  fid INT NOT NULL PRIMARY KEY ,
  capacity INT
  );


CREATE TABLE Reservations (
  rid INT NOT NULL PRIMARY KEY,
  fid1 INT,
  fid2 INT,
  paid INT,
  price INT,
  username VARCHAR(20),
  dayOfMonth INT,
  canceled INT
  );


