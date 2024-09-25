import psycopg2
import os
import sys
import datetime
from operator import itemgetter
from collections import Counter
from types import *
import argparse
import decimal

from queries import *
from answers import *

parser = argparse.ArgumentParser()
parser.add_argument('-v', '--verbose', help="Print out the query results and more details", required=False, action="store_true")
parser.add_argument('-q', '--query', type = int, help="Only run and check the given query number", required=False)
args = parser.parse_args()

verbose = args.verbose

# Check if x and y are almost near match
def match(x, y):
	print(x == y)
	return (x == y)

def compareAnswers(ans, correct):
	# Special case empty answer
	if len(ans) == 0:
		if len(correct) == 0:
			return ("Score = 4: Both answers empty", 4)
		else:
			return ("Score = 0: Empty answer", 0)

	if len(correct) == 0:
		return ("Score = 0: The answer should have been empty", 0)


	# If the number of columns is not correct, no score
	if len(ans[0]) != len(correct[0]):
		return ("Score = 0: Incorrect Number of Columns", 0)

	# If the number of rows in the answer is the same, check for near-exact match
	if len(ans) == len(correct):
		c = Counter()
		for x in correct:
			if x in ans:
				c[True] += 1
			else:
				c[False] += 1
		if c[False] == 0:
			return ("Score = 5: Exact or Near-exact Match", 5)

	# Let's try to do an approximate match
	flattened_ans = Counter([str(x).strip() for y in ans for x in y])
	flattened_correct = Counter([str(x).strip() for y in correct for x in y])


	jaccard = sum((flattened_correct & flattened_ans).values()) * 1.0/sum((flattened_correct | flattened_ans).values())
	if verbose:
		print ("------ Creating word counts and comparing answers ---------")
		print (flattened_correct)
		print (flattened_ans)
		print ("Jaccard Coefficient: {}".format(jaccard))

	if jaccard > 0.9:
		if len(ans) == len(correct):
			return ("Score = 3: Very similar, but not an exact match (possibly wrong sort order)", 3)
		else:
			return ("Score = 2: Very similar, but incorrect number of rows", 2)
	if jaccard > 0.5:
		return ("Score = 1: Somewhat similar answers", 1)
	return ("Score = 0: Answers too different", 0)

conn = psycopg2.connect("dbname=flights user=root password=root")
cur = conn.cursor()

totalscore = 0
for i in [1,2,3]:
	# If a query is specified by -q option, only do that one
	if args.query is None or args.query == i:
		try:
			q = queries[i]
			if i == 1:
				q = """
				SELECT flightid, {0} 
				FROM {1} 
				WHERE customerid = 'cust7' or customerid is null
				GROUP BY flightid
				ORDER BY flightid;
				""".format(queries[i][0], queries[i][1])
				
			print ("========== Executing Query {}".format(i))
			print (q)

			cur.execute(q)
			ans = cur.fetchall()

			if i == 3:
				ans = sorted(ans, key=itemgetter(0))
				correctanswers[i] = sorted(correctanswers[i], key=itemgetter(0))

			if verbose:
				print ("--------- Your Query Answer ---------")
				for t in ans:
					print (t)
				print ("--------- Correct Answer ---------")
				for t in correctanswers[i]:
					print (t)

			# Compare with correctanswers[i]
			cmp_res = compareAnswers(ans, correctanswers[i])
			print ("-----> " + cmp_res[0])
			totalscore += cmp_res[1]

		except:
			print (sys.exc_info())
			raise

print ("-----------------> Total Score = {}".format(totalscore))