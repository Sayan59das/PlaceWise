FILES
=====
src/Person.java               Abstract class (name + abstract display)
src/EligibilityRule.java      Interface with isEligible(Student)
src/Student.java              Student record, extends Person, implements Comparable
src/Company.java              Company criteria, implements EligibilityRule
src/InvalidDataException.java Custom checked exception
src/PlacementManager.java     ArrayList + LinkedHashMap storage, sorting, CSV read, report write
src/Main.java                 Console menu, input validation, Scanner

src/WebApp.java               Small web version using the built-in JDK HttpServer
web/index.html                Main page of the web version
web/result.html               Eligibility result page
web/style.css                 Plain CSS for the web version

data/students.csv             Sample student data, loaded automatically at start
data/students_bad.csv         File with broken lines, used to show validation
report.txt                    Sample exported report

THERE ARE TWO WAYS TO RUN IT
============================
1. Console version (terminal menu)
   run.bat
   or: javac -d out src\*.java   then   java -cp out Main

2. Web version (browser page)
   run-web.bat
   or: javac -d out src\*.java   then   java -cp out WebApp
   Then open http://localhost:8080

Both versions use the same Student, Company, PlacementManager and
InvalidDataException classes. Only the input method is different.

WEB PAGE
========
The HTML is not written in Java. There are two real HTML files:

web/index.html    the main page
web/result.html   the eligibility result page
web/style.css     the styling

When a page is requested, WebApp reads the .html file from the web folder,
fills in the {{PLACEHOLDERS}} with data, and sends it back. The placeholders
in index.html are:

{{MESSAGE}}          the green message box, if there is one
{{STUDENT_ROWS}}     one table row per student
{{STUDENT_COUNT}}    total number of students
{{COMPANY_ROWS}}     one table row per company with the eligible count
{{STUDENT_OPTIONS}}  the options of the student dropdown
{{COMPANY_OPTIONS}}  the options of the company dropdown
{{RANK_ROWS}}        the ranked table rows
{{ELIGIBLE_BLOCKS}}  the eligible list of every company

result.html has only {{CLASS}} (ok or no) and {{RESULT}}.

You can open web/index.html directly in a browser to see the plain layout,
but the tables will look empty because the placeholders are not filled in.
The full version needs the Java side running.

The page shows, top to bottom:
- the student table
- the company table with the eligible count for each company
- a dropdown form to check one student against one company
- the ranked student table
- the eligible students of every company
- a form to add a student
- a form to add a company
- an "Export Report" link that writes report.txt

Every form is a normal HTML form. There is no JavaScript.

WHAT THE PROJECT DOES
=====================
1. Stores academic records of students (roll number, name, branch, CGPA, backlogs).
2. Stores company eligibility criteria (minimum CGPA, maximum allowed backlogs).
3. Checks a single student against a single company.
4. Generates the list of eligible students for a company.
5. Ranks all students by CGPA (higher CGPA first, roll number breaks ties).
6. Exports a full placement report to a text file.
7. Reads student records from a CSV file and skips invalid lines.

CONSOLE MENU
============
1. Add Student
2. Add Company
3. Show All Students
4. Show All Companies
5. Check One Student For A Company
6. Show Eligible Students For A Company
7. Show Ranked Students
8. Export Placement Report
9. Load Students From CSV File
10. Exit

CSV FORMAT
==========
RollNo,Name,Branch,CGPA,Backlogs
101,Ikjot Singh,CSE,8.75,0

DEMO STEPS FOR THE TEACHER
==========================
Console:
1. Run run.bat, press 3 to show the 8 stored students.
2. Press 6, type Amazon, show the eligible list.
3. Press 7 to show the ranking table.
4. Press 1, enter CGPA 15, show the rejection message.
5. Press 9, type data/students_bad.csv, show that bad lines are skipped.
6. Press 8, type report.txt, then open report.txt.

Web:
1. Run run-web.bat and let the browser open.
2. Show the student table and the company table.
3. Pick 105 and Amazon, press Check, show NOT ELIGIBLE.
4. Pick 106 and Amazon, press Check, show ELIGIBLE.
5. Add a student with CGPA 15 to show the error message.
6. Click Export Report, then open report.txt.
