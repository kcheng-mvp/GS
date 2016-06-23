#! /usr/bin/env groovy
def message ="https://github.com/polarmobile/coffeescript-style-guide"
/*
Immediately inside parentheses, brackets or braces
($ 'body') # Yes
   ( $ 'body' ) # No
*/
def test1 = "( 'body')";
def test2 = "(good )"
def test3 = "( go od )"
def test4 = "(a, b)"
def test5 = "(ab)"
def test6 = "(ab  )"
def test7 = "( ab )"
def test8 = "asdasfas( ab )asdasdfa"
def r_i_s_p = ~/\(\s+|\s+\)/;
assert (test1 =~ r_i_s_p)
assert (test2 =~ r_i_s_p)
assert (test3 =~ r_i_s_p)
assert !(test4 =~ r_i_s_p)
assert !(test5 =~ r_i_s_p)
assert (test6 =~ r_i_s_p)
assert (test7 =~ r_i_s_p)
assert (test8 =~ r_i_s_p)

/*
Immediately before a comma
console.log x, y # Yes
console.log x , y # No
*/

def r_i_b_c = ~/\s+,/;
def test9="abcd,"
def test10="abcd ,"
assert !(test9 =~ r_i_b_c)
assert (test10 =~ r_i_b_c)


/*
Always surround these binary operators with a single space on either side
1: assignment: =
2: augmented assignment: +=, -=, etc.
3: comparisons: ==, <, >, <=, >=, unless, etc.
4: arithmetic operators: +, -, *, /, etc.
*/
//                      =  |    +=     |  -=     |==       |<      |>     |<=       |>=        |+
//def r_operators = ~/\w=|=\w|\w\+=|\+=\w|\w\-=|\-=\w|\w==|==\w|\w<|<\w|\w>|>\w|\w<=|<=\w|\w>=|>=\w|\w[\+\-\*\/]|[\+\-\*\/]\w/
def r_operators = ~/\w\+=|\+=\w|\w\-=|\-=\w|\w==|==\w|\w<|<\w|\w>|>\w|\w<=|<=\w|\w>=|>=\w|\w[\+\-\*\/=]|[\+\-\*\/=]\w/
def test111 ="#{API_URL}/loan_applications/:application_id/purposes";
assert (test111 =~ r_operators)
def test11 = "asdasdfasdaa=3"
def test12 = " =asdfasdfasd"
def test13 = "asdfasfasa = 4asdfasdfasda"
assert (test11 =~ r_operators)
assert (test12 =~ r_operators)
assert !(test13 =~ r_operators)
def test14 = "asdfaa+= 13"
def test15 = "asda +=13"
def test16 = "asdasa += 13"
assert (test14 =~ r_operators)
assert (test15 =~ r_operators)
assert !(test16 =~ r_operators)
def test17 = "asda-=13 as"
def test18 = "asda -=13 "
def test19 = "a -= 13"
assert (test17 =~ r_operators)
assert (test18 =~ r_operators)
assert !(test19 =~ r_operators)

def test20 = " a == bdas"
def test21 = "adsaa ==badsf"
def test22 = "adsfaa== basdfa"
def test222 = "adsfaa==basdfa"
assert !(test20 =~ r_operators)
assert (test21 =~ r_operators)
assert (test22 =~ r_operators)
assert (test22 =~ r_operators)

def test23 = "a < b"
def test24 = "asdas<b"
def test25 = "asdas< b"
def test26 = "asdas <b"
assert !(test23 =~ r_operators)
assert (test24 =~ r_operators)
assert (test25 =~ r_operators)
assert (test26 =~ r_operators)


def test27 = "a > b"
def test28 = "asdas>b"
def test29 = "asdas> b"
def test30 = "asdas >b"
assert !(test27 =~ r_operators)
assert (test28 =~ r_operators)
assert (test29 =~ r_operators)
assert (test30 =~ r_operators)

def test31 = "a <= b"
def test32 = "asdas<=b"
def test33 = "asdas<= b"
def test34 = "asdas <=b"
assert !(test31 =~ r_operators)
assert (test32 =~ r_operators)
assert (test33 =~ r_operators)
assert (test34 =~ r_operators)


def test35 = "a >= b"
def test36 = "asdas>=b"
def test37 = "asdas>= b"
def test38 = "asdas >=b"
assert !(test35 =~ r_operators)
assert (test36 =~ r_operators)
assert (test37 =~ r_operators)
assert (test38 =~ r_operators)

def test39 = "a + b"
def test40 = "asdas+b"
def test41 = "asdas+ b"
def test42 = "asdas +b"
assert !(test39 =~ r_operators)
assert (test40 =~ r_operators)
assert (test41 =~ r_operators)
assert (test42 =~ r_operators)

def test43 = "a - b"
def test44 = "asdas-b"
def test45 = "asdas- b"
def test46 = "asdas -b"
assert !(test43 =~ r_operators)
assert (test44 =~ r_operators)
assert (test45 =~ r_operators)
assert (test46 =~ r_operators)
def test47 = "a * b"
def test48 = "asdas*b"
def test49 = "asdas* b"
def test50 = "asdas *b"
assert !(test47 =~ r_operators)
assert (test48 =~ r_operators)
assert (test49 =~ r_operators)
assert (test50 =~ r_operators)
def test51 = "a / b"
def test52 = "asdas/b"
def test53 = "asdas/ b"
def test54 = "asdas /b"
assert !(test51 =~ r_operators)
assert (test52 =~ r_operators)
assert (test53 =~ r_operators)
assert (test54 =~ r_operators)


def function_rule = ~/\S->/;
def test55 ="foo = (arg1, arg2)->"
assert (test55 =~ function_rule)

/*

and is preferred over &&.
or is preferred over ||.
is is preferred over ==.
not is preferred over !.
*/


