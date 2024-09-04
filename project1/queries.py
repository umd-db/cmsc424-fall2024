queries = ["" for i in range(0, 11)]
### EXAMPLE
### 0. List all airport codes and their cities. Order by the city name in the increasing order.
### Output column order: airportid, city

queries[0] = """
select airportid, city
from airports
order by city;
"""

### 1. Write a query to find the names of customers who have flights on a Monday and 
###    first name that has a second letter is not a vowel [a, e, i, o, u].
###    If a customer who satisfies the condition flies on multiple fridays, output their name only once.
###    Do not include the oldest customer among those that satisfies the above conditions in the results.
### Hint:  - See postgresql date operators that are linked to from the README, and the "like" operator (see Chapter 3.4.2). 
###        - Alternately, you can use a regex to match the condition imposed on the name.
###        - See postgresql date operators and string functions
###        - You may want to use a self-join to avoid including the youngest customer.
###        - When testing, write a query that includes all customers, then modify that to exclude the youngest.
### Order: by name
### Output columns: name
queries[1] = """
"""


### 2. Write a query to find customers who are frequent fliers on Delta Airlines (DL) 
###    and have their birthday are either before 02/15 or after 11/15 (mm/dd). 
### Hint: See postgresql date functions.
### Order: by birthdate
### Output columns: customer id, name, birthdate
queries[2] = """
"""

### 3. Write a query to rank the customers who have taken most number of flights with their
###    frequentflieron airline, along with their name, airlineid, and number of times they 
###    have flown with the airlines. If any ties make the top 10 rankings exceed 10 results 
###    (ex. the number of most flights is shared by 20 people), list all such customers.
### Output: (rank, name, airlineid count)
### Order: rank, name
### HINT: Use the RANK() function provided by PostgreSQL. 
queries[3] = """
"""

### 4. Write a query to find the airlines with the least number of customers that 
###    choose it as their frequent flier airline. For example, if 10 customers have Delta
###    listed as their frequent flier airline, and no other airlines have fewer than 10
###    frequent flier customers, then the query should return  "DELTA, 10" as the
###    only result. In the case of a tie, return all tied airlines.
### Hint: use `with clause` and nested queries (Chapter 3.8.6). 
### Output: name, count
### Order: name
queries[4] = """
"""


### 5. Write a query to find the most-frequent flyers (customers who have flown on most number of flights).
###    In this dataset and in general, always assume that there could be multiple flyers who satisfy this condition.
###    Assuming multiple customers exist, list the customer names along with the count of other frequent flyers
###    they have flown with.
###    Two customers are said to have flown together when they have a flewon entry with a matching flightid and flightdate.
###    For example if Alice, Bob and Charlie flew on the most number of flighs (3 each). Assuming Alice and Bob never flew together,
###    while Charlie flew with both of them, the expected output would be: [('Alice', 1), ('Bob', 1), ('Charlie', 2)].
### NOTE: A frequent flyer here is purely based on number of occurances in flewon, (not the frequentflieron field).
### Output: name, count
### Order: order by count desc, name.
queries[5] = """
"""

### 6. Write a query to find the percentage participation of American Airlines in each airport, relative to the other airlines.
### One instance of participation in an airport is defined as a flight (EX. AA150) having a source or dest of that airport.
### If UA101 leaves OAK and arrives in DFW, that adds 1 to American's count for both OAK and DFW airports.
### This means that if AA has 1 in DFW, UA has 1 in DFW, DL has 2 in DFW, and SW has 3 in DFW, the query returns:
###     airport 		                              | participation
###     General Edward Lawrence Logan International   | .14
### Output: (airport_name, participation).
### Order: Participation in descending order, airport name
### Note: - The airport column must be the full name of the airport
###       - The participation percentage is rounded to 2 decimals, as shown above
###       - You do not need to confirm that the flights actually occur by referencing the flewon table. This query is only concerned with
###         flights that exist in the flights table.
###       - You must not leave out airports that have no UA flights (participation of 0)
queries[6] = """
"""

### 7. Write a query to find the customer/customers that taken the highest number of flights but have never flown on their frequentflier airline.
###    If there is a tie, return the names of all such customers. 
### Output: Customer name
### Order: name
queries[7] = """
"""

### 8. Write a query to find customers that took the same flight (identified by flightid) on consecutive days.
###    Return the name, flightid start and end date of the customers flights.
###    The start date should be the first date of the sequence and the end date should be the last date of the sequence.
###    If a customer took the same flight on multiple sequences of consecutive days, return all the sequences.
### Output: customer_name, flightid, start_date, end_date
### Order: by customer_name, flightid, start_date
queries[8] = """
"""

### 9. A layover consists of set of two flights where the destination of the first flight is the same 
###    as the source of the second flight. Additionally, the arrival of the first flight must be before the
###    departure of the first flight. 
###    Write a query to find all pairs of flights belonging to the same airline that had a layover in IAD
###    between 1 and 4 hours in length (inclusive).
### Output columns: 1st flight id, 2nd flight id, source city, destination city, layover duration
### Order by: layover duration
queries[9] = """
"""



### 10. Provide a ranking of the airlines that are most loyal to their hub. 
###     The loyalty of an airline to its hub is defined by the ratio of the number
###     of flights that fly in or out of the hub versus the total number of flights
###     operated by airline. 
###     Output: (name, loyalty, rank)
###     Order: rank, name
### Note: a) If two airlines tie, then they should both get the same rank, and the next rank should be skipped. 
### For example, if the top two airlines have the same ratio, then there should be no rank 2, e.g., 1, 1, 3 ...
queries[10] = """
"""