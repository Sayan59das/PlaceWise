// Screen 1: add, update, delete, search and sort students. The Java side checks every value
// (StudentValidator), so a mistake comes back as a message listing every problem at once.

let studentSort = 'ROLL_NO';

// [sort key the server understands, column heading]
const STUDENT_COLUMNS = [
  ['ROLL_NO', 'Roll no', ''], ['NAME', 'Name', ''], ['BRANCH', 'Branch', ''],
  ['SEMESTER', 'Sem', 'num'], ['CGPA', 'CGPA', 'num'], ['BACKLOGS', 'Backlogs', 'num'],
];

async function showStudents(screen) {
  screen.innerHTML = titleRow('Students', `
      <label class="field">Search by name<input id="student-search" type="search" placeholder="e.g. Sharma"></label>
      <button class="btn primary" id="student-add">Add a student</button>`) + `
    <form id="student-form" class="sheet form-grid" hidden></form>
    <div id="student-table"></div>`;
  screen.querySelector('#student-search').oninput = () => run(loadStudentTable);
  screen.querySelector('#student-add').onclick = () => openStudentForm(null);
  await loadStudentTable();
}

async function loadStudentTable() {
  const search = document.getElementById('student-search').value;
  const answer = await api('GET', '/api/students', { sort: studentSort, search });
  const students = answer.students;
  const box = document.getElementById('student-table');
  if (students.length === 0) {
    box.innerHTML = search
      ? `<p class="empty">No student's name contains "${esc(search)}".</p>`
      : '<p class="empty">There are no students yet. Add one, or load the sample data on the Import screen.</p>';
    return;
  }
  const headings = STUDENT_COLUMNS.map(([sort, label, type]) => {
    // CGPA sorts highest first; every other column sorts A to Z / smallest first
    const order = sort !== studentSort ? 'none' : (sort === 'CGPA' ? 'descending' : 'ascending');
    return `<th class="${type}" aria-sort="${order}"><button data-sort="${sort}">${label}</button></th>`;
  }).join('');
  const rows = students.map(s => `
    <tr>
      <td>${esc(s.rollNo)}</td><td class="nowrap">${esc(s.name)}</td><td>${esc(s.branch)}</td>
      <td class="num">${s.semester}</td><td class="num">${twoDecimals(s.cgpa)}</td>
      <td class="num">${s.activeBacklogs}</td><td class="soft"><span class="email" title="${esc(s.email || '')}">${esc(s.email || '')}</span></td>
      <td class="actions">
        <button class="btn small" data-edit="${esc(s.rollNo)}">Edit</button>
        <button class="btn small danger" data-delete="${esc(s.rollNo)}">Delete</button>
      </td>
    </tr>`).join('');
  box.innerHTML = `<p class="count">${plural(students.length, 'student', 'students')}
      · click a heading to sort</p>
    <div class="table-wrap"><table class="ruled">
      <thead><tr>${headings}<th>Email</th><th><span class="sr">Actions</span></th></tr></thead>
      <tbody>${rows}</tbody>
    </table></div>`;

  for (const button of box.querySelectorAll('[data-sort]')) {
    button.onclick = () => { studentSort = button.dataset.sort; run(loadStudentTable); };
  }
  for (const button of box.querySelectorAll('[data-edit]')) {
    button.onclick = () => openStudentForm(students.find(s => s.rollNo === button.dataset.edit));
  }
  for (const button of box.querySelectorAll('[data-delete]')) {
    button.onclick = () => confirmThen(button, () => deleteStudent(button.dataset.delete));
  }
}

// Opens the form above the table: empty to add, filled in to update (the roll number cannot change).
function openStudentForm(student) {
  const form = document.getElementById('student-form');
  const s = student || { rollNo: '', name: '', branch: '', semester: '', cgpa: '', activeBacklogs: '0', email: '' };
  form.innerHTML = `
    <h2>${student ? 'Update ' + esc(s.rollNo) : 'Add a student'}</h2>
    <label class="field">Roll number<input name="rollNo" value="${esc(s.rollNo)}" ${student ? 'readonly' : ''}></label>
    <label class="field">Name<input name="name" value="${esc(s.name)}"></label>
    <label class="field">Branch<input name="branch" value="${esc(s.branch)}" placeholder="e.g. CSE"></label>
    <label class="field">Semester (1-8)<input name="semester" inputmode="numeric" value="${esc(s.semester)}"></label>
    <label class="field">CGPA (0-10)<input name="cgpa" inputmode="decimal" value="${student ? twoDecimals(s.cgpa) : ''}"></label>
    <label class="field">Active backlogs<input name="activeBacklogs" inputmode="numeric" value="${esc(s.activeBacklogs)}"></label>
    <label class="field wide">Email (optional)<input name="email" value="${esc(s.email || '')}"></label>
    <div class="form-actions">
      <button class="btn primary" type="submit">${student ? 'Save changes' : 'Add student'}</button>
      <button class="btn" type="button" id="student-cancel">Cancel</button>
    </div>`;
  form.hidden = false;
  form.querySelector('input:not([readonly])').focus();
  form.querySelector('#student-cancel').onclick = () => { form.hidden = true; };
  form.onsubmit = event => {
    event.preventDefault();
    run(async () => {
      const path = student ? '/api/students/update' : '/api/students/add';
      const answer = await api('POST', path, new FormData(form));
      showMessage(answer.message);
      form.hidden = true;
      await loadStudentTable();
    });
  };
}

async function deleteStudent(rollNo) {
  const answer = await api('POST', '/api/students/delete', { rollNo });
  showMessage(answer.message);
  await loadStudentTable();
}
