import datetime
from decimal import *

correctanswers = ["" for i in range(0, 12)]

correctanswers[0] = [('ATL','Atlanta             '),('BOS','Boston              '),('ORD','Chicago             '),('DFW','Dallas-Fort Worth   '),('DEN','Denver              '),('FLL','Fort Lauderdale     '),('LAX','Los Angeles         '),('JFK','New York            '),('OAK','Oakland             '),('IAD','Washington          ')]

correctanswers[1] = [('Anthony Allen                 ',), ('Anthony Edwards               ',), ('Anthony Evans                 ',), ('Anthony Garcia                ',), ('Anthony Gonzalez              ',), ('Anthony Harris                ',), ('Brian Evans                   ',), ('Brian Garcia                  ',), ('Brian Gonzalez                ',), ('Brian Jackson                 ',), ('Charles Brown                 ',), ('Charles Collins               ',), ('Charles Evans                 ',), ('Charles Garcia                ',), ('Charles Gonzalez              ',), ('Charles Hall                  ',), ('Christopher Davis             ',), ('Christopher Hernandez         ',), ('Christopher Hill              ',),('Edward Baker                  ',), ('Edward Carter                 ',), ('Edward Davis                  ',), ('Edward Edwards                ',), ('Edward Evans                  ',), ('Edward Garcia                 ',), ('Edward Harris                 ',), ('Elizabeth Anderson            ',), ('Elizabeth Collins             ',), ('Elizabeth Gonzalez            ',), ('Elizabeth Green               ',), ('Elizabeth Hall                ',), ('Elizabeth Harris              ',), ('Elizabeth Hill                ',)]

correctanswers[2] = [('cust8     ', 'Barbara Gonzalez              ', datetime.date(1976, 1, 10)),
('cust126   ', 'Jeff Harris                   ', datetime.date(1976, 1, 19)),
('cust112   ', 'James Evans                   ', datetime.date(1981, 1, 19)),
('cust81    ', 'Edward Davis                  ', datetime.date(1981, 2, 12)),
('cust87    ', 'Elizabeth Baker               ', datetime.date(1986, 11, 23)),
('cust55    ', 'Deborah Anderson              ', datetime.date(1987, 2, 2)),
('cust106   ', 'Helen Evans                   ', datetime.date(1991, 1, 4)),
('cust102   ', 'George Gonzalez               ', datetime.date(1996, 1, 30))]

correctanswers[3] = [(1, 'Anthony Edwards               ', 'SW', 6),
(1, 'Brian Evans                   ', 'SW', 6),
(3, 'Betty Gonzalez                ', 'SW', 5),
(3, 'Christopher Hernandez         ', 'SW', 5),
(3, 'Donald Allen                  ', 'UA', 5),
(3, 'Edward Evans                  ', 'SW', 5),
(7, 'Anthony Harris                ', 'UA', 4),
(7, 'Betty Brown                   ', 'UA', 4),
(7, 'Carol Baker                   ', 'SW', 4),
(7, 'Charles Collins               ', 'SW', 4),
(7, 'Charles Garcia                ', 'AA', 4),
(7, 'Daniel Baker                  ', 'SW', 4),
(7, 'Deborah Collins               ', 'SW', 4),
(7, 'George Davis                  ', 'SW', 4)]

correctanswers[4] = [('United Airlines     ', 15)]

correctanswers[5] = [('Charles Evans                 ', 18),
('Anthony Evans                 ', 17),
('Barbara Harris                ', 17),
('Barbara Gonzalez              ', 16),
('Carol Evans                   ', 16),
('Barbara Davis                 ', 15),
('Brian Gonzalez                ', 15),
('David Adams                   ', 15),
('Brian Evans                   ', 14),
('Charles Garcia                ', 14),
('Anthony Harris                ', 13),
('Barbara Hall                  ', 13),
('Betty Edwards                 ', 13),
('Barbara Collins               ', 12),
('Christopher Davis             ', 12),
('Anthony Gonzalez              ', 11),
('Betty Gonzalez                ', 11),
('Anthony Edwards               ', 10),
('Anthony Allen                 ', 4)]
correctanswers[6] = [('Dallas Fort Worth International                                                                     ', Decimal('0.38')),
('John F Kennedy International                                                                        ', Decimal('0.33')),
('Los Angeles International                                                                           ', Decimal('0.33')),
('Fort Lauderdale Hollywood International                                                             ', Decimal('0.22')),
('Hartsfield Jackson Atlanta International                                                            ', Decimal('0.14')),
('Washington Dulles International                                                                     ', Decimal('0.14')),
('Metropolitan Oakland International                                                                  ', Decimal('0.11')),
("Chicago O'Hare International                                                                        ", Decimal('0.00')),
('Denver International                                                                                ', Decimal('0.00')),
('General Edward Lawrence Logan International                                                         ', Decimal('0.00'))]
correctanswers[7] = [('Barbara Collins               ',), ('Betty Edwards                 ',), ('Carol Evans                   ',), ('Charles Evans                 ',)]

correctanswers[8] = [('Anthony Evans                 ', 'UA101 ', datetime.date(2016, 8, 1), datetime.date(2016, 8, 2)), ('Anthony Evans                 ', 'UA101 ', datetime.date(2016, 8, 7), datetime.date(2016, 8, 8)), ('Anthony Evans                 ', 'UA101 ', datetime.date(2016, 8, 8), datetime.date(2016, 8, 9)), ('Anthony Garcia                ', 'UA101 ', datetime.date(2016, 8, 3), datetime.date(2016, 8, 4)), ('Anthony Garcia                ', 'UA101 ', datetime.date(2016, 8, 4), datetime.date(2016, 8, 5)), ('Anthony Gonzalez              ', 'SW116 ', datetime.date(2016, 8, 3), datetime.date(2016, 8, 4)), ('Barbara Davis                 ', 'UA101 ', datetime.date(2016, 8, 4), datetime.date(2016, 8, 5)), ('Barbara Davis                 ', 'UA101 ', datetime.date(2016, 8, 5), datetime.date(2016, 8, 6)), ('Barbara Hall                  ', 'UA101 ', datetime.date(2016, 8, 7), datetime.date(2016, 8, 8)), ('Barbara Harris                ', 'UA101 ', datetime.date(2016, 8, 1), datetime.date(2016, 8, 2)), ('Betty Brown                   ', 'UA101 ', datetime.date(2016, 8, 2), datetime.date(2016, 8, 3)), ('Betty Edwards                 ', 'UA101 ', datetime.date(2016, 8, 8), datetime.date(2016, 8, 9)), ('Betty Gonzalez                ', 'UA101 ', datetime.date(2016, 8, 4), datetime.date(2016, 8, 5)), ('Betty Gonzalez                ', 'UA101 ', datetime.date(2016, 8, 5), datetime.date(2016, 8, 6)), ('Brian Evans                   ', 'UA101 ', datetime.date(2016, 8, 1), datetime.date(2016, 8, 2)), ('Brian Gonzalez                ', 'UA101 ', datetime.date(2016, 8, 4), datetime.date(2016, 8, 5)), ('Carol Evans                   ', 'UA101 ', datetime.date(2016, 8, 1), datetime.date(2016, 8, 2)), ('Carol Evans                   ', 'UA101 ', datetime.date(2016, 8, 4), datetime.date(2016, 8, 5)), ('Charles Evans                 ', 'UA101 ', datetime.date(2016, 8, 3), datetime.date(2016, 8, 4)), ('Charles Evans                 ', 'UA101 ', datetime.date(2016, 8, 6), datetime.date(2016, 8, 7)), ('Charles Evans                 ', 'UA101 ', datetime.date(2016, 8, 7), datetime.date(2016, 8, 8)), ('Charles Evans                 ', 'UA101 ', datetime.date(2016, 8, 8), datetime.date(2016, 8, 9)), ('Charles Garcia                ', 'AA127 ', datetime.date(2016, 8, 1), datetime.date(2016, 8, 2)), ('Charles Garcia                ', 'UA101 ', datetime.date(2016, 8, 6), datetime.date(2016, 8, 7)), ('Daniel Green                  ', 'UA101 ', datetime.date(2016, 8, 6), datetime.date(2016, 8, 7)), ('Daniel Jackson                ', 'UA101 ', datetime.date(2016, 8, 2), datetime.date(2016, 8, 3)), ('Daniel Jackson                ', 'UA101 ', datetime.date(2016, 8, 3), datetime.date(2016, 8, 4)), ('David Hall                    ', 'UA101 ', datetime.date(2016, 8, 4), datetime.date(2016, 8, 5)), ('David Hernandez               ', 'UA101 ', datetime.date(2016, 8, 3), datetime.date(2016, 8, 4)), ('David Hill                    ', 'UA101 ', datetime.date(2016, 8, 2), datetime.date(2016, 8, 3)), ('Deborah Adams                 ', 'UA101 ', datetime.date(2016, 8, 6), datetime.date(2016, 8, 7)), ('Deborah Allen                 ', 'SW102 ', datetime.date(2016, 8, 1), datetime.date(2016, 8, 2)), ('Deborah Anderson              ', 'SW107 ', datetime.date(2016, 8, 8), datetime.date(2016, 8, 9)), ('Deborah Baker                 ', 'SW104 ', datetime.date(2016, 8, 4), datetime.date(2016, 8, 5)), ('Deborah Baker                 ', 'UA101 ', datetime.date(2016, 8, 6), datetime.date(2016, 8, 7)), ('Donald Allen                  ', 'UA101 ', datetime.date(2016, 8, 3), datetime.date(2016, 8, 4)), ('Donna Brown                   ', 'UA101 ', datetime.date(2016, 8, 6), datetime.date(2016, 8, 7)), ('Donna Edwards                 ', 'UA101 ', datetime.date(2016, 8, 5), datetime.date(2016, 8, 6)), ('Dorothy Allen                 ', 'UA101 ', datetime.date(2016, 8, 4), datetime.date(2016, 8, 5)), ('Dorothy Carter                ', 'UA101 ', datetime.date(2016, 8, 5), datetime.date(2016, 8, 6)), ('Elizabeth Collins             ', 'SW124 ', datetime.date(2016, 8, 3), datetime.date(2016, 8, 4)), ('George Evans                  ', 'UA101 ', datetime.date(2016, 8, 6), datetime.date(2016, 8, 7)), ('George Garcia                 ', 'UA101 ', datetime.date(2016, 8, 4), datetime.date(2016, 8, 5)), ('Helen Evans                   ', 'SW103 ', datetime.date(2016, 8, 5), datetime.date(2016, 8, 6)), ('Helen Evans                   ', 'SW103 ', datetime.date(2016, 8, 6), datetime.date(2016, 8, 7)), ('Helen Harris                  ', 'UA101 ', datetime.date(2016, 8, 1), datetime.date(2016, 8, 2)), ('Jason Hall                    ', 'UA101 ', datetime.date(2016, 8, 3), datetime.date(2016, 8, 4)), ('Jeff Green                    ', 'SW102 ', datetime.date(2016, 8, 8), datetime.date(2016, 8, 9))]

correctanswers[9] = [('SW110 ', 'SW171 ', 'DFW', 'DFW', datetime.timedelta(seconds=3840)),
('SW136 ', 'SW157 ', 'OAK', 'FLL', datetime.timedelta(seconds=4740)),
('SW118 ', 'SW171 ', 'OAK', 'DFW', datetime.timedelta(seconds=13200))]

correctanswers[10] = [('American Airlines   ', 1),
('Southwest Airlines  ', 2),
('Delta Airlines      ', 3),
('United Airlines     ', 4)]

correctanswers[11] = [('Anthony Evans                 ', 'UA101 ', datetime.date(2016, 8, 1), datetime.date(2016, 8, 2)), ('Anthony Evans                 ', 'UA101 ', datetime.date(2016, 8, 7), datetime.date(2016, 8, 9)), ('Anthony Garcia                ', 'UA101 ', datetime.date(2016, 8, 3), datetime.date(2016, 8, 5)), ('Anthony Gonzalez              ', 'SW116 ', datetime.date(2016, 8, 3), datetime.date(2016, 8, 4)), ('Barbara Davis                 ', 'UA101 ', datetime.date(2016, 8, 4), datetime.date(2016, 8, 6)), ('Barbara Hall                  ', 'UA101 ', datetime.date(2016, 8, 7), datetime.date(2016, 8, 8)), ('Barbara Harris                ', 'UA101 ', datetime.date(2016, 8, 1), datetime.date(2016, 8, 2)), ('Betty Brown                   ', 'UA101 ', datetime.date(2016, 8, 2), datetime.date(2016, 8, 3)), ('Betty Edwards                 ', 'UA101 ', datetime.date(2016, 8, 8), datetime.date(2016, 8, 9)), ('Betty Gonzalez                ', 'UA101 ', datetime.date(2016, 8, 4), datetime.date(2016, 8, 6)), ('Brian Evans                   ', 'UA101 ', datetime.date(2016, 8, 1), datetime.date(2016, 8, 2)), ('Brian Gonzalez                ', 'UA101 ', datetime.date(2016, 8, 4), datetime.date(2016, 8, 5)), ('Carol Evans                   ', 'UA101 ', datetime.date(2016, 8, 1), datetime.date(2016, 8, 2)), ('Carol Evans                   ', 'UA101 ', datetime.date(2016, 8, 4), datetime.date(2016, 8, 5)), ('Charles Evans                 ', 'UA101 ', datetime.date(2016, 8, 3), datetime.date(2016, 8, 4)), ('Charles Evans                 ', 'UA101 ', datetime.date(2016, 8, 6), datetime.date(2016, 8, 9)), ('Charles Garcia                ', 'AA127 ', datetime.date(2016, 8, 1), datetime.date(2016, 8, 2)), ('Charles Garcia                ', 'UA101 ', datetime.date(2016, 8, 6), datetime.date(2016, 8, 7)), ('Daniel Green                  ', 'UA101 ', datetime.date(2016, 8, 6), datetime.date(2016, 8, 7)), ('Daniel Jackson                ', 'UA101 ', datetime.date(2016, 8, 2), datetime.date(2016, 8, 4)), ('David Hall                    ', 'UA101 ', datetime.date(2016, 8, 4), datetime.date(2016, 8, 5)), ('David Hernandez               ', 'UA101 ', datetime.date(2016, 8, 3), datetime.date(2016, 8, 4)), ('David Hill                    ', 'UA101 ', datetime.date(2016, 8, 2), datetime.date(2016, 8, 3)), ('Deborah Adams                 ', 'UA101 ', datetime.date(2016, 8, 6), datetime.date(2016, 8, 7)), ('Deborah Allen                 ', 'SW102 ', datetime.date(2016, 8, 1), datetime.date(2016, 8, 2)), ('Deborah Anderson              ', 'SW107 ', datetime.date(2016, 8, 8), datetime.date(2016, 8, 9)), ('Deborah Baker                 ', 'SW104 ', datetime.date(2016, 8, 4), datetime.date(2016, 8, 5)), ('Deborah Baker                 ', 'UA101 ', datetime.date(2016, 8, 6), datetime.date(2016, 8, 7)), ('Donald Allen                  ', 'UA101 ', datetime.date(2016, 8, 3), datetime.date(2016, 8, 4)), ('Donna Brown                   ', 'UA101 ', datetime.date(2016, 8, 6), datetime.date(2016, 8, 7)), ('Donna Edwards                 ', 'UA101 ', datetime.date(2016, 8, 5), datetime.date(2016, 8, 6)), ('Dorothy Allen                 ', 'UA101 ', datetime.date(2016, 8, 4), datetime.date(2016, 8, 5)), ('Dorothy Carter                ', 'UA101 ', datetime.date(2016, 8, 5), datetime.date(2016, 8, 6)), ('Elizabeth Collins             ', 'SW124 ', datetime.date(2016, 8, 3), datetime.date(2016, 8, 4)), ('George Evans                  ', 'UA101 ', datetime.date(2016, 8, 6), datetime.date(2016, 8, 7)), ('George Garcia                 ', 'UA101 ', datetime.date(2016, 8, 4), datetime.date(2016, 8, 5)), ('Helen Evans                   ', 'SW103 ', datetime.date(2016, 8, 5), datetime.date(2016, 8, 7)), ('Helen Harris                  ', 'UA101 ', datetime.date(2016, 8, 1), datetime.date(2016, 8, 2)), ('Jason Hall                    ', 'UA101 ', datetime.date(2016, 8, 3), datetime.date(2016, 8, 4)), ('Jeff Green                    ', 'SW102 ', datetime.date(2016, 8, 8), datetime.date(2016, 8, 9))]