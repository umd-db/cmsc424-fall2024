import psycopg2
import subprocess
from tabulate import tabulate

def executePrint(s):
	cur.execute(s)
	print(tabulate(cur.fetchall()))

def printAll(s):
    print(s)
    print()
    print("CUSTOMERS")
    executePrint("select * from customers")
    print()
    print("NEWCUSTOMERS")
    executePrint("select * from newcustomers")
    print()
    print("FFAIRLINES")
    executePrint("select * from ffairlines")
    print()

subprocess.call(["dropdb", "flighttrigger"])
subprocess.call(["createdb", "flighttrigger"])
subprocess.call(["psql", "-f", "trigger-database.sql", "flighttrigger"])
subprocess.call(["psql", "-f", "trigger.sql", "flighttrigger"])

conn = psycopg2.connect("dbname=flighttrigger user=root password=root")
cur = conn.cursor()



print("------------------- OPERATIONS ON CUSTOMERS -------------------")

printAll("Initial state")

print("Inserting ('cust4', 'Anthony Gonzalez', to_date('1977-10-06', 'yyyy-mm-dd'), 'AA') into customers table")
cur.execute("INSERT into customers values ('cust4', 'Anthony Gonzalez', to_date('1977-10-06', 'yyyy-mm-dd'), 'AA')")
conn.commit()

printAll("After inserting cust4. customer should be present in all tables")

print("Now deleting cust4")
cur.execute("DELETE from customers where customerid = 'cust4'")
conn.commit()

printAll("After deletion. cust4 should not be in any table")

print("Inserting ('cust4', 'Anthony Gonzalez', to_date('1977-10-06', 'yyyy-mm-dd'), NULL) into customers table")
cur.execute("INSERT into customers values ('cust4', 'Anthony Gonzalez', to_date('1977-10-06', 'yyyy-mm-dd'), NULL)")
conn.commit()

printAll("After inserting cust4. Should be present in customers and newcustomers but not ffairlines")

print("Updating cust4's name to 'Tony Gonzalez' in customers table")
cur.execute("UPDATE customers set name = 'Tony Gonzalez' where customerid='cust4'")
conn.commit()

printAll("After updating name.  Update should be seen in both customer tables")

print("Updating cust4's frequentflieron to DL in customers table")
cur.execute("UPDATE customers set frequentflieron = 'DL' where customerid='cust4'")
conn.commit()

printAll("After update of frequentflieron to DL for cust4. The frequentflieron column for cust4 in customers table should be DL")

print("Updating cust4's frequentflieron to NULL in customers table")
cur.execute("UPDATE customers set frequentflieron = NULL where customerid='cust4'")
conn.commit()

printAll("After updating frequentflieron to null. This should delete all entries for cust4 in ffairlines")

print("Now deleting cust4")
cur.execute("DELETE from customers where customerid = 'cust4'")
conn.commit()

printAll("After deletion.  Again cust4 should not appear in any tables")




print("------------------- CONTINUING WITH OPERATIONS ON NEWCUSTOMERS -------------------")


print("Now deleting cust15")
cur.execute("DELETE from newcustomers where customerid = 'cust15'")
conn.commit()

printAll("After deletion, cust15 should not appear in any table")

print("Inserting ('cust4', 'Anthony Gonzalez', to_date('1977-10-06', 'yyyy-mm-dd')) into newcustomers table")
cur.execute("INSERT into newcustomers values ('cust4', 'Anthony Gonzalez', to_date('1977-10-06', 'yyyy-mm-dd'))")
conn.commit()

printAll("After inserting cust4, should be in both customer tables but not ffairlines")

print("Updating cust4's name to 'Tony Gonzalez' in newcustomers table")
cur.execute("UPDATE newcustomers set name = 'Tony Gonzalez' where customerid='cust4'")
conn.commit()

printAll("After updating name, Update should appear in both customer tables")



print("------------------- CONTINUING WITH OPERATIONS ON FFAIRLINES -------------------")

print("Adding cust4's flights to flewon table and subsequently adding ffairline as AA")
cur.execute("insert into flewon values ('SW132', 'cust4', to_date('2016-08-05', 'YYYY-MM-DD'))")
cur.execute("insert into flewon values ('AA131', 'cust4', to_date('2016-08-08', 'YYYY-MM-DD'))")
cur.execute("insert into flewon values ('SW116', 'cust4', to_date('2016-08-03', 'YYYY-MM-DD'))")
cur.execute("insert into flewon values ('UA101', 'cust4', to_date('2016-08-01', 'YYYY-MM-DD'))")
cur.execute("insert into flewon values ('SW116', 'cust4', to_date('2016-08-04', 'YYYY-MM-DD'))")
cur.execute("insert into flewon values ('SW110', 'cust4', to_date('2016-08-02', 'YYYY-MM-DD'))")
cur.execute("insert into flewon values ('SW106', 'cust4', to_date('2016-08-06', 'YYYY-MM-DD'))")
cur.execute("insert into flewon values ('UA138', 'cust4', to_date('2016-08-09', 'YYYY-MM-DD'))")
cur.execute("insert into flewon values ('SW107', 'cust4', to_date('2016-08-07', 'YYYY-MM-DD'))")
cur.execute("INSERT into ffairlines VALUES ('cust4', 'AA', (select coalesce(sum(trunc(extract(epoch from local_arrival_time - local_departing_time)/60)), 0) from flewon natural join flights where customerid = 'cust4' AND airlineid = 'AA'));")
conn.commit()

printAll('After setting cust4 ff airline as AA. frequentflieron in customer should also be AA')

print('Updating AA to be SW instead for cust4 in ffairlines')
cur.execute("UPDATE ffairlines set airlineid = 'SW', points = (select coalesce(sum(trunc(extract(epoch from local_arrival_time - local_departing_time)/60)), 0) from flewon natural join flights where customerid = 'cust4' AND airlineid = 'SW') where customerid = 'cust4' and airlineid = 'AA'")
conn.commit()

printAll('After updating AA to SW as ff airline.  SW should now be the frequentflieron for cust4')

cur.execute("INSERT into ffairlines VALUES ('cust4', 'AA', (select coalesce(sum(trunc(extract(epoch from local_arrival_time - local_departing_time)/60)), 0) from flewon natural join flights where customerid = 'cust4' AND airlineid = 'AA'));")
conn.commit()

printAll("With AA and SW as ffairlines for cust4,  frequentflieron for cust4 should be 'AA' because of the most recent flightdate")

print('Removing SW as cust4 ff airline')
cur.execute("DELETE from ffairlines where customerid = 'cust4' and airlineid = 'SW'")

printAll('After Removing SW as ff airline it should still be AA for frequentflieron')

print('Removing AA as cust4 ffairline')
cur.execute("DELETE from ffairlines where customerid = 'cust4' and airlineid = 'AA'")

printAll('After Removing AA as ffairline.  Now it should be null because cust4 has no entries in fffairlines')

print("------------------- CONTINUING WITH OPERATIONS ON FLEWON -------------------")


print("Adding both UA and AA as ff airlines for cust4")
cur.execute("INSERT into ffairlines VALUES ('cust4', 'UA', (select coalesce(sum(trunc(extract(epoch from local_arrival_time - local_departing_time)/60)), 0) from flewon natural join flights where customerid = 'cust4' AND airlineid = 'UA'));")
cur.execute("INSERT into ffairlines VALUES ('cust4', 'AA', (select coalesce(sum(trunc(extract(epoch from local_arrival_time - local_departing_time)/60)), 0) from flewon natural join flights where customerid = 'cust4' AND airlineid = 'AA'));")
conn.commit()

printAll("Initial state. NOTE: cust4 has 2 fffairlines.  The frequentflieron airline should be UA.")

print("Now removing a recent UA flight in flewon for cust 4")
cur.execute("DELETE from flewon where customerid = 'cust4' and flightid = 'UA138'")

printAll("Deleted recent UA flight and cust 4 has recently flown AA")

print("Updating an AA flight to be SW instead")
cur.execute("UPDATE flewon set flightid = 'SW132' where customerid = 'cust4' and flightid = 'AA131'")

printAll("After the update cust4 has flown recent on UA than AA.")

print("Inserting ('UA101', 'cust4', to_date('2016-08-01', 'YYYY-MM-DD')) into flewon")
cur.execute("INSERT into flewon values ('UA101', 'cust4', to_date('2016-08-01', 'YYYY-MM-DD'))")

printAll("After the insert UA is still the most recent flight.")