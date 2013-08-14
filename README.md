dbdeploy
========

Java tools to manage agile database development.

This fork extends the statement parser to interpret Oracle DDL statements on a logical basis i.e. based on content and 
not on pre-defined statement delimiters (this is mainly due to the fact that whilst one can define a "custom" statement
delimiter in SQL*Plus, this is not possible globally). 
