# Flights
[![License: MIT](https://img.shields.io/badge/license-MIT-green.svg)](https://opensource.org/licenses/MIT)
[![Build Status](https://travis-ci.org/xuliang2019/Flights.svg?branch=master)](https://travis-ci.org/xuliang2019/Flights)
[![Coverage Status](https://coveralls.io/repos/github/xuliang2019/Flights/badge.svg?branch=master)](https://coveralls.io/github/xuliang2019/Flights?branch=master)
## Overview
This projects aims to realize a flight booking application. It enables us to create user account, search and book desired flights, as well as check reservation information. As always, you have the right to cancel booked flights. This application is achived by Java and SQL languages.

<div align=center> <img src="https://github.com/xuliang2019/Flights/raw/master/figures/logo.png" width = "400"> </div>

## Function explanations
#### 1. Create
This method enables us to create a user account, including username, password and balance.
#### 2. Login
Using login command to sign in our account.
#### 3. Search
* Search method can display the full flights information once we executed it, such as flight number, departing time, price and etc. 
* when the value of `<direct>` variable is 1, it means the searching flights are direct. On the contrary, the result will be indirect flights if we input 0 for that item;
* The range for `<day>` variable is from 1 to 31. It represents the day of month;
* `<num itineraries>` means the number of flights you are going to search.
#### 4. Book
Once you confirmed a flight, you could utilize the `book` function to reserve the airline.
#### 5. Pay
After reservation, all you need is to `pay` it.
#### 6. Reservations
If you need to check your reservations, just type the command `reservations`. It will display all your reserved flights.
#### 7. Cancel
If you changed you idea about one itinerary, just `cancel` it!
#### 8. Quit
```quit``` helps us to exit the application.

## Interface
```
*** Please enter one of the following commands ***
> create <username> <password> <initial amount>
> login <username> <password>
> search <origin city> <destination city> <direct> <day> <num itineraries>
> book <itinerary id>
> pay <reservation id>
> reservations
> cancel <reservation id>
> quit
```

## Installation
------------
* Use `git clone` to download the whole files:
```
git clone https://github.com/xuliang2019/Flights.git
```
* Create the three needed tables using `createTables.sql` according to the instruction mentioned in the `documents/table explanation`;
* Import flights data into cloud database; 
* Set the `dbconn.properties` parameters.Open it and you will see detail instructions. In my work, I created a database on [``Azure``](https://azure.microsoft.com/);
* Finally, open a `CMD` and navigate to the cloned folder. Under the directory of `code`, run the command:
```
java -jar flights/flightapp-1.0-jar-with-dependencies.jar
```
And you will see above interface. Congratulations!
## Instruction Demo
You can download the instruction video [``here``](https://github.com/xuliang2019/Flights/raw/master/figures/CMI_GIF.mp4?raw=true)
