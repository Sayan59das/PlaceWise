package com.placewise.ui.screens;

import com.placewise.AppContext;
import com.placewise.model.Cgpa;
import com.placewise.model.Student;
import com.placewise.ranking.StudentSort;
import com.placewise.ui.ConsoleIO;
import com.placewise.ui.Menu;
import com.placewise.ui.Views;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

/** Screen 1: add, update, delete, search and list students. */
public final class StudentMenu extends Menu {

    public StudentMenu(ConsoleIO io, AppContext app) {
        super(io, app);
    }

    @Override
    public void run() {
        int choice;
        do {
            choice = choose("Students", List.of("Add a student", "Update a student", "Delete a student",
                    "Search by roll number", "Search by name", "List all students"));
            switch (choice) {
                case 1 -> add();
                case 2 -> update();
                case 3 -> delete();
                case 4 -> searchByRoll();
                case 5 -> searchByName();
                case 6 -> list();
                default -> { }
            }
        } while (choice != 0);
    }

    private void add() {
        String rollNo = io.text("Roll number");
        String name = io.text("Name");
        String branch = io.text("Branch (for example CSE)");
        int semester = io.integer("Semester", 1, 8);
        BigDecimal cgpa = io.decimal("CGPA", Cgpa.MIN, Cgpa.MAX);
        int backlogs = io.integer("Active backlogs", 0, 99);
        String email = io.text("Email (optional)", "");
        Student saved = app.students().add(new Student(rollNo, name, branch, semester, cgpa, backlogs, email));
        io.println("Added " + saved.rollNo() + " - " + saved.name() + ".");
    }

    private void update() {
        Student old = app.students().get(io.text("Roll number to update"));
        io.println("Press Enter to keep the current value.");
        Student changed = new Student(old.rollNo(),
                io.text("Name", old.name()),
                io.text("Branch", old.branch()),
                io.integer("Semester", 1, 8, old.semester()),
                io.decimal("CGPA", Cgpa.MIN, Cgpa.MAX, old.cgpa()),
                io.integer("Active backlogs", 0, 99, old.activeBacklogs()),
                io.text("Email", old.email() == null ? "" : old.email()));
        app.students().update(changed);
        io.println("Updated " + changed.rollNo() + ".");
    }

    private void delete() {
        String rollNo = io.text("Roll number to delete");
        Student student = app.students().get(rollNo);
        if (io.confirm("Delete " + student.rollNo() + " - " + student.name() + "?")) {
            app.students().delete(student.rollNo());
            io.println("Deleted.");
        } else {
            io.println("Nothing deleted.");
        }
    }

    private void searchByRoll() {
        Student student = app.students().get(io.text("Roll number"));
        io.println(Views.students("Student " + student.rollNo(), List.of(student)));
    }

    private void searchByName() {
        List<Student> found = app.students().searchByName(io.text("Part of the name"));
        io.println(found.isEmpty() ? "No student matches." : Views.students("Matching students", found));
    }

    private void list() {
        io.println("Sort by: " + Arrays.toString(StudentSort.values()));
        String answer = io.text("Sort field (Enter = ROLL_NO)", "ROLL_NO");
        StudentSort sort = StudentSort.parse(answer).orElse(null);
        if (sort == null) {
            io.println("'" + answer + "' is not a sort field; using ROLL_NO.");
            sort = StudentSort.ROLL_NO;
        }
        List<Student> all = app.students().listAll(sort);
        io.println(all.isEmpty() ? "There are no students yet." : Views.students("Students by " + sort, all));
    }
}
